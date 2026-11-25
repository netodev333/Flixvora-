package com.flixvora.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flixvora.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/** Lifecycle states for a queued/active download. */
object DownloadStatus {
    const val QUEUED = 0
    const val RUNNING = 1
    const val PAUSED = 2
    const val COMPLETED = 3
    const val FAILED = 4
}

/** One download request, persisted to disk so it survives process death. */
data class DownloadTask(
    val id: Long,
    val title: String,
    val streamUrl: String,
    val cookies: String,
    val isHls: Boolean,
    val posterUrl: String,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long = 0L,
    var status: Int = DownloadStatus.QUEUED,
    var error: String? = null,
    // HLS resume state
    var mediaUrl: String = "",
    var segmentIndex: Int = 0,
    var totalSegments: Int = 0
) {
    val progress: Int
        get() = when {
            totalBytes > 0 -> ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            // HLS: the final size is unknown until every segment lands, so report
            // progress from the segment index instead of leaving it stuck at 0%.
            totalSegments > 0 -> ((segmentIndex * 100) / totalSegments).coerceIn(0, 100)
            else -> 0
        }
}

/** Immutable view of the queue; [revision] bumps on every change so observers re-read. */
data class DownloadSnapshot(val revision: Long, val tasks: List<DownloadTask>)

/**
 * Runs downloads in a foreground service so they keep going when the app is
 * backgrounded or the screen is off. The previous implementation lived in the
 * ViewModel scope: leaving the app silently cancelled the job, deleted the
 * partial `.part` file, and forced a restart from zero — which is exactly why
 * data was consumed without any download ever finishing.
 *
 * Downloads run strictly one at a time. Progress and the queue are persisted,
 * and partial episodes resume from where they stopped instead of re-downloading.
 */
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "flixvora_download_channel"
        private const val NOTIFICATION_ID = 4001
        private const val STATE_FILE = "download_queue.json"

        const val ACTION_START = "com.flixvora.download.START"
        const val ACTION_PAUSE = "com.flixvora.download.PAUSE"
        const val ACTION_RESUME = "com.flixvora.download.RESUME"
        const val ACTION_CANCEL = "com.flixvora.download.CANCEL"
        const val ACTION_STOP = "com.flixvora.download.STOP"

        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_URL = "url"
        const val EXTRA_COOKIES = "cookies"
        const val EXTRA_IS_HLS = "is_hls"
        const val EXTRA_POSTER = "poster"

        private val _snapshot = MutableStateFlow(DownloadSnapshot(0L, emptyList()))

        /** Queued + active + paused tasks, oldest first. Completed ones drop out. */
        val snapshots: StateFlow<DownloadSnapshot> = _snapshot

        fun enqueue(context: Context, task: DownloadTask) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, task.id)
                putExtra(EXTRA_TITLE, task.title)
                putExtra(EXTRA_URL, task.streamUrl)
                putExtra(EXTRA_COOKIES, task.cookies)
                putExtra(EXTRA_IS_HLS, task.isHls)
                putExtra(EXTRA_POSTER, task.posterUrl)
            }
            startSafely(context, intent)
        }

        fun pause(context: Context, id: Long) = send(context, id, ACTION_PAUSE)
        fun resume(context: Context, id: Long) = send(context, id, ACTION_RESUME)
        fun cancel(context: Context, id: Long) = send(context, id, ACTION_CANCEL)

        /**
         * Restarts the service on app launch if a previous session left downloads
         * unfinished, so a killed process resumes instead of silently losing the
         * queue. Safe to call on every launch: it no-ops when nothing is pending.
         */
        fun resumePendingIfAny(context: Context) {
            val pending = try {
                val file = File(context.filesDir, STATE_FILE)
                if (!file.exists()) false
                else JSONArray(file.readText()).let { arr ->
                    (0 until arr.length()).any { i ->
                        val st = arr.getJSONObject(i).optInt("status")
                        st == DownloadStatus.QUEUED || st == DownloadStatus.RUNNING
                    }
                }
            } catch (_: Exception) {
                false
            }
            if (pending) {
                startSafely(
                    context,
                    Intent(context, DownloadService::class.java).apply { action = ACTION_START }
                )
            }
        }

        private fun send(context: Context, id: Long, action: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                this.action = action
                putExtra(EXTRA_ID, id)
            }
            startSafely(context, intent)
        }

        private fun startSafely(context: Context, intent: Intent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start DownloadService", e)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskList = java.util.concurrent.CopyOnWriteArrayList<DownloadTask>()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var worker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastPublish = 0L

    /**
     * Id of the task the worker should stop. Pausing/cancelling sets this instead
     * of cancelling the worker, so the pump stays alive and keeps draining the
     * rest of the queue instead of halting every other queued episode.
     */
    @Volatile
    private var abortTaskId: Long = -1L

    private class DownloadAborted : Exception("aborted")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (taskList.isEmpty()) {
            taskList.addAll(loadPersisted())
            publish(force = true)
        }
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Promote to foreground immediately so the OS never kills us mid-download.
        startForegroundCompat(buildNotification(null))

        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                if (id > 0 && url.isNotBlank()) {
                    addTask(
                        DownloadTask(
                            id = id,
                            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                            streamUrl = url,
                            cookies = intent.getStringExtra(EXTRA_COOKIES).orEmpty(),
                            isHls = intent.getBooleanExtra(EXTRA_IS_HLS, false),
                            posterUrl = intent.getStringExtra(EXTRA_POSTER).orEmpty()
                        )
                    )
                }
            }
            ACTION_PAUSE -> setPaused(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_RESUME -> setResumed(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_CANCEL -> cancelTask(intent.getLongExtra(EXTRA_ID, -1L))
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
        }

        ensureWorker()
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ queue

    private fun addTask(task: DownloadTask) {
        if (taskList.none { it.id == task.id }) {
            taskList.add(task)
            publish(force = true)
            persist()
        }
    }

    private fun setPaused(id: Long) {
        val task = taskList.find { it.id == id } ?: return
        if (task.status == DownloadStatus.RUNNING) {
            task.status = DownloadStatus.PAUSED
            abortTaskId = id
        } else if (task.status == DownloadStatus.QUEUED) {
            task.status = DownloadStatus.PAUSED
        }
        publish(force = true)
        persist()
    }

    private fun setResumed(id: Long) {
        val task = taskList.find { it.id == id } ?: return
        if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
            task.status = DownloadStatus.QUEUED
            task.error = null
            publish(force = true)
            persist()
            ensureWorker()
        }
    }

    private fun cancelTask(id: Long) {
        val task = taskList.find { it.id == id } ?: return
        if (task.status == DownloadStatus.RUNNING) {
            task.status = DownloadStatus.PAUSED // stop the loop before we delete
            abortTaskId = id
        }
        deletePartFile(task)
        taskList.removeAll { it.id == id }
        publish(force = true)
        persist()
    }

    private fun ensureWorker() {
        if (worker?.isActive == true) return
        worker = scope.launch { runPump() }
    }

    private suspend fun runPump() {
        while (scope.isActive) {
            val next = taskList.firstOrNull { it.status == DownloadStatus.QUEUED }
            if (next == null) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (taskList.none { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }) {
                        stopEverything()
                    }
                }
                return
            }
            next.status = DownloadStatus.RUNNING
            publish(force = true)

            try {
                if (next.isHls) downloadHls(next) else downloadDirect(next)
                if (next.status == DownloadStatus.RUNNING) {
                    next.status = DownloadStatus.COMPLETED
                    next.totalBytes = next.downloadedBytes.coerceAtLeast(next.totalBytes)
                    taskList.removeAll { it.id == next.id }
                }
            } catch (e: DownloadAborted) {
                // Pause/cancel already set the correct state; keep the .part file
                // so resuming never re-downloads what we already have. The pump
                // keeps running so the rest of the queue still drains.
                if (abortTaskId == next.id) abortTaskId = -1L
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${next.title}", e)
                next.status = DownloadStatus.FAILED
                next.error = e.message
            } finally {
                publish(force = true)
                persist()
            }
        }
    }

    // -------------------------------------------------------------- downloads

    private suspend fun downloadDirect(task: DownloadTask) {
        val dir = storageDir()
        val safeTitle = sanitize(task.title)
        val target = File(dir, "$safeTitle.mp4")
        val part = File(dir, "$safeTitle.mp4.part")

        var existing = if (part.exists()) part.length() else 0L
        val requestBuilder = Request.Builder().url(task.streamUrl)
        applyHeaders(requestBuilder, task)
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")

        client.newCall(requestBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            val body = resp.body ?: throw Exception("Empty response")

            val append = resp.code == 206 && existing > 0
            if (!append) {
                existing = 0L
                task.downloadedBytes = 0L
            }
            val contentLength = body.contentLength()
            task.totalBytes = if (contentLength > 0) existing + contentLength else 0L

            RandomAccessFile(part, "rw").use { raf ->
                if (!append) raf.setLength(0)
                raf.seek(existing)
                body.byteStream().use { input ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        if (!scope.isActive || task.status != DownloadStatus.RUNNING || abortTaskId == task.id) {
                            throw DownloadAborted()
                        }
                        val read = input.read(buffer)
                        if (read < 0) break
                        raf.write(buffer, 0, read)
                        task.downloadedBytes += read
                        publish()
                    }
                }
            }
        }

        if (part.length() == 0L) throw Exception("Downloaded file is empty")
        finalizeFile(part, target)
        if (task.totalBytes <= 0) task.totalBytes = task.downloadedBytes
    }

    private suspend fun downloadHls(task: DownloadTask) {
        val headers = headersFor(task)
        val dir = storageDir()
        val safeTitle = sanitize(task.title)
        val target = File(dir, "$safeTitle.mp4")
        val part = File(dir, "$safeTitle.mp4.part")

        // Resolve the media playlist once (master -> variant) and remember it so a
        // resume does not have to re-resolve or re-download earlier segments.
        if (task.mediaUrl.isBlank() || task.totalSegments == 0) {
            val masterUrl = task.streamUrl
            val master = fetchText(masterUrl, headers)
            var mediaUrl = masterUrl
            if (master.contains("#EXT-X-STREAM-INF")) {
                val variant = master.lines().map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
                    ?: throw Exception("No variant stream in master playlist")
                mediaUrl = masterUrl.toHttpUrlOrNull()?.resolve(variant)?.toString() ?: variant
            }
            task.mediaUrl = mediaUrl
        }

        val mediaContent = fetchText(task.mediaUrl, headers)
        val mediaBase = task.mediaUrl.substringBeforeLast('/') + "/"
        val segments = mediaContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { uri ->
                if (uri.startsWith("http")) uri
                else task.mediaUrl.toHttpUrlOrNull()?.resolve(uri)?.toString()
                    ?: java.net.URI(mediaBase).resolve(uri).toString()
            }
        if (segments.isEmpty()) throw Exception("No video segments found")

        task.totalSegments = segments.size
        if (task.segmentIndex > segments.size) task.segmentIndex = 0
        // If the persisted index is stale (e.g. the .part file was cleared) the
        // resumed file would be missing segments, so restart from zero.
        if (task.segmentIndex > 0 && (!part.exists() || part.length() == 0L)) {
            task.segmentIndex = 0
            task.downloadedBytes = 0L
        }

        RandomAccessFile(part, "rw").use { raf ->
            if (task.segmentIndex == 0) {
                raf.setLength(0)
                task.downloadedBytes = 0L
            } else {
                raf.seek(raf.length())
            }

            for (i in task.segmentIndex until segments.size) {
                if (!scope.isActive || task.status != DownloadStatus.RUNNING || abortTaskId == task.id) {
                    throw DownloadAborted()
                }
                val bytes = fetchSegmentBytes(segments[i], headers)
                    ?: throw Exception("Segment $i failed")
                raf.write(bytes)
                task.segmentIndex = i + 1
                task.downloadedBytes += bytes.size
                publish()
            }
        }

        if (part.length() == 0L) throw Exception("Downloaded file is empty")
        finalizeFile(part, target)
        task.totalBytes = task.downloadedBytes
    }

    private fun finalizeFile(part: File, target: File) {
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
    }

    private fun fetchText(url: String, headers: Map<String, String>): String {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} for $url")
            return resp.body?.string() ?: throw Exception("Empty body for $url")
        }
    }

    private fun fetchSegmentBytes(url: String, headers: Map<String, String>): ByteArray? {
        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                val builder = Request.Builder().url(url)
                headers.forEach { (k, v) -> builder.header(k, v) }
                client.newCall(builder.build()).execute().use { resp ->
                    if (resp.isSuccessful) return resp.body?.bytes()
                }
            } catch (e: Exception) {
                if (attempts >= 3) return null
                Thread.sleep(800)
            }
        }
        return null
    }

    private fun applyHeaders(builder: Request.Builder, task: DownloadTask) {
        headersFor(task).forEach { (k, v) -> builder.header(k, v) }
    }

    /** Builds the request headers the CDN expects (UA always; Referer/Cookie as needed). */
    private fun headersFor(task: DownloadTask): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        val host = task.streamUrl.lowercase()
        val isAnifamHost = host.contains("dramahot") || host.contains("zokoanime") ||
            host.contains("megaplay")
        // Match the player's User-Agent per provider: MovieBox CDNs reject the
        // plain okhttp UA, while the anifam/dramahot CDN expects it.
        headers["User-Agent"] = if (isAnifamHost) {
            "okhttp/4.12.0"
        } else {
            "MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)"
        }

        val cookies = task.cookies
        if (cookies.isNotEmpty()) {
            val looksLikeHeaderBag = cookies.contains("=") &&
                (cookies.contains("Referer=") || cookies.contains("Origin=") || cookies.contains(";"))
            if (looksLikeHeaderBag) {
                cookies.split(";").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) headers[parts[0].trim()] = parts[1].trim()
                }
            } else {
                headers["Cookie"] = cookies
            }
        }

        // The dramahot/zokoanime HLS CDN returns 403 without a Referer.
        if (isAnifamHost && !headers.containsKey("Referer")) {
            headers["Referer"] = "https://zokoanime.video/"
        }
        return headers
    }

    // -------------------------------------------------------------- helpers

    private fun storageDir(): File {
        try {
            val pubDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Flixvora"
            )
            if (pubDir.exists() || pubDir.mkdirs()) {
                val test = File(pubDir, ".test_${System.currentTimeMillis()}")
                if (test.createNewFile()) {
                    test.delete()
                    return pubDir
                }
            }
        } catch (_: Throwable) {}
        try {
            val appExt = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            if (appExt != null) {
                val appDir = File(appExt, "Flixvora")
                if (appDir.exists() || appDir.mkdirs()) return appDir
            }
        } catch (_: Throwable) {}
        val internalDir = File(filesDir, "Flixvora")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    private fun sanitize(title: String): String = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")

    private fun deletePartFile(task: DownloadTask) {
        try {
            File(storageDir(), "${sanitize(task.title)}.mp4.part").delete()
        } catch (_: Throwable) {}
    }

    private fun publish(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastPublish < 400) return
        lastPublish = now
        _snapshot.value = DownloadSnapshot(_snapshot.value.revision + 1, taskList.toList())
        updateNotification()
    }

    // -------------------------------------------------------------- persistence

    private fun persist() {
        try {
            val arr = JSONArray()
            taskList.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("streamUrl", t.streamUrl)
                    put("cookies", t.cookies)
                    put("isHls", t.isHls)
                    put("posterUrl", t.posterUrl)
                    put("downloadedBytes", t.downloadedBytes)
                    put("totalBytes", t.totalBytes)
                    put("status", if (t.status == DownloadStatus.RUNNING) DownloadStatus.QUEUED else t.status)
                    put("mediaUrl", t.mediaUrl)
                    put("segmentIndex", t.segmentIndex)
                    put("totalSegments", t.totalSegments)
                })
            }
            File(filesDir, STATE_FILE).writeText(arr.toString())
        } catch (e: Exception) {
            Log.w(TAG, "persist failed: ${e.message}")
        }
    }

    private fun loadPersisted(): List<DownloadTask> {
        return try {
            val file = File(filesDir, STATE_FILE)
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val storedStatus = o.optInt("status")
                if (storedStatus == DownloadStatus.COMPLETED) return@mapNotNull null
                // A task that was mid-download when the process died is stored as
                // QUEUED and resumes automatically; an explicitly paused or failed
                // task stays paused so it does not start on its own.
                val restoredStatus = if (storedStatus == DownloadStatus.PAUSED || storedStatus == DownloadStatus.FAILED) {
                    DownloadStatus.PAUSED
                } else {
                    DownloadStatus.QUEUED
                }
                DownloadTask(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    streamUrl = o.optString("streamUrl"),
                    cookies = o.optString("cookies"),
                    isHls = o.optBoolean("isHls"),
                    posterUrl = o.optString("posterUrl"),
                    downloadedBytes = o.optLong("downloadedBytes"),
                    totalBytes = o.optLong("totalBytes"),
                    status = restoredStatus,
                    mediaUrl = o.optString("mediaUrl"),
                    segmentIndex = o.optInt("segmentIndex"),
                    totalSegments = o.optInt("totalSegments")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadPersisted failed: ${e.message}")
            emptyList()
        }
    }

    // -------------------------------------------------------------- notification

    private fun buildNotification(task: DownloadTask?): Notification {
        val active = task ?: taskList.firstOrNull { it.status == DownloadStatus.RUNNING }
        val text = when {
            active == null -> "Preparing downloads…"
            active.totalBytes > 0 -> "${active.progress}% • ${active.title}"
            else -> "Downloading ${active.title}"
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flixvora downloads")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_flixvora)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (active != null && active.totalBytes > 0) {
            builder.setProgress(100, active.progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.notify(NOTIFICATION_ID, buildNotification(null))
        } catch (_: Throwable) {}
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while episodes download in the background"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.createNotificationChannel(channel)
        }
    }

    private fun stopEverything() {
        persist()
        releaseWakeLock()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Throwable) {}
        stopSelf()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Flixvora:DownloadWakeLock").apply {
                setReferenceCounted(false)
                acquire(2 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "wakeLock failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {}
        wakeLock = null
    }
}
