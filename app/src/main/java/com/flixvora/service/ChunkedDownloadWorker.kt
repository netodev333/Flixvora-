package com.flixvora.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

class ChunkedDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val streamUrl = inputData.getString(KEY_STREAM_URL) ?: return@withContext Result.failure()
        val startByte = inputData.getLong(KEY_START_BYTE, 0L)
        val endByte = inputData.getLong(KEY_END_BYTE, 0L)

        val targetFile = File(applicationContext.filesDir, "offline/$downloadId.part")
        targetFile.parentFile?.mkdirs()

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L
        val resumeOffset = startByte + existingBytes

        if (endByte > 0L && resumeOffset >= endByte) {
            return@withContext Result.success()
        }

        var connection: HttpURLConnection? = null
        return@withContext try {
            connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                val rangeHeader = if (endByte > 0L) "bytes=$resumeOffset-$endByte" else "bytes=$resumeOffset-"
                setRequestProperty("Range", rangeHeader)
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) {
                return@withContext Result.retry()
            }

            RandomAccessFile(targetFile, "rw").use { raf ->
                raf.seek(existingBytes)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (!isStopped) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        raf.write(buffer, 0, read)
                    }
                }
            }

            if (isStopped) Result.retry() else Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_START_BYTE = "start_byte"
        const val KEY_END_BYTE = "end_byte"

        fun enqueueChunkedResume(
            context: Context,
            downloadId: String,
            streamUrl: String,
            wifiOnly: Boolean,
            startByte: Long = 0L,
            endByte: Long = 0L
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putString(KEY_DOWNLOAD_ID, downloadId)
                .putString(KEY_STREAM_URL, streamUrl)
                .putLong(KEY_START_BYTE, startByte)
                .putLong(KEY_END_BYTE, endByte)
                .build()

            val request = OneTimeWorkRequestBuilder<ChunkedDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "chunk-$downloadId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
