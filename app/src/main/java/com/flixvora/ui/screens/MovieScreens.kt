package com.flixvora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.local.FavoriteEntity
import com.flixvora.data.local.HistoryEntity
import com.flixvora.data.model.Episode
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.StreamItem
import com.flixvora.data.model.SubtitleItem
import com.flixvora.ui.theme.*
import com.flixvora.ui.components.safeTopInset
import com.flixvora.ui.components.safeBottomInset
import com.flixvora.ui.components.AuroraBackground
import com.flixvora.ui.components.BrandGradient
import com.flixvora.ui.components.bounceClick
import com.flixvora.ui.components.glassSurface
import com.flixvora.ui.viewmodel.MovieViewModel
import com.flixvora.ui.viewmodel.ActiveDownload
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.net.Uri
import android.content.Intent
import com.flixvora.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures


fun downloadVideo(context: android.content.Context, url: String, title: String, posterUrl: String = "", showToast: Boolean = true) {
    val postToast: (String) -> Unit = { msg ->
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
        }
    }

    if (url.isEmpty()) {
        if (showToast) postToast("Stream URL is empty")
        return
    }
    try {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        
        if (posterUrl.isNotEmpty()) {
            val safeShowName = Regex("(.*?)(_S\\d+E\\d+.*|$)").find(safeTitle)?.groupValues?.get(1)?.trim() ?: safeTitle

            // Direct HTTP download for poster (avoids racing with DownloadManager on the same file)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
                var conn: java.net.HttpURLConnection? = null
                try {
                    val flixvoraDir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Flixvora")
                    if (!flixvoraDir.exists()) flixvoraDir.mkdirs()
                    val titlePoster = java.io.File(flixvoraDir, "$safeTitle.jpg")
                    val showPoster = if (safeShowName.isNotEmpty() && safeShowName != safeTitle) java.io.File(flixvoraDir, "$safeShowName.jpg") else null

                    conn = (java.net.URL(posterUrl).openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "okhttp/4.12.0")
                        setRequestProperty("accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    }
                    if (conn.responseCode in 200..299) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        if (bytes.isNotEmpty()) {
                            if (!titlePoster.exists()) titlePoster.writeBytes(bytes)
                            if (showPoster != null && !showPoster.exists()) showPoster.writeBytes(bytes)
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    conn?.disconnect()
                }
            }
        }

        val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
        val flixvoraDir = java.io.File(moviesDir, "Flixvora")
        if (!flixvoraDir.exists()) flixvoraDir.mkdirs()

        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
        val fileName = "$safeTitle.mp4"
        request.setTitle(title)
        request.setDescription("Downloading...")
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MOVIES, "Flixvora/$fileName")
        manager.enqueue(request)
        if (showToast) {
            postToast("Download started...")
        }
    } catch (e: Exception) {
        if (showToast) {
            postToast("Failed to start download: ${e.message}")
        }
    }
}

/**
 * Downloads a direct (progressive) video file to the Flixvora storage dir and
 * returns the finished [java.io.File], or null on failure. Blocking on purpose:
 * the download queue calls this from a single worker so files arrive one by one.
 */
fun loadMediaOnCast(
    castSession: com.google.android.gms.cast.framework.CastSession?,
    streamUrl: String,
    subject: MovieSubject,
    viewModel: com.flixvora.ui.viewmodel.MovieViewModel,
    context: android.content.Context? = null
) {
    if (castSession == null || !castSession.isConnected || streamUrl.isEmpty()) return
    try {
        val remoteMediaClient = castSession.remoteMediaClient ?: return

        // Prefer H.264 / universally compatible stream for Google Cast if available in playInfo
        val availableStreams = viewModel.playInfo?.streamList ?: emptyList()
        val preferredStream = availableStreams.find { s ->
            val u = s.url.lowercase()
            u.isNotEmpty() && !u.contains("b164fbfb43477929") && !u.contains("/h265/") && !u.contains("h265") && !u.contains("hevc")
        } ?: viewModel.selectedStream

        val rawTargetUrl = preferredStream?.url?.ifEmpty { streamUrl } ?: streamUrl
        val targetCookie = preferredStream?.cookie ?: viewModel.selectedStream?.cookie ?: ""

        // Resolve phone's LAN IP address for Cast receiver
        val wifiIp = com.flixvora.cast.CastForegroundService.getWifiIpAddress()
        val castUrl = if (wifiIp != "127.0.0.1") {
            val encodedTarget = java.net.URLEncoder.encode(rawTargetUrl, "UTF-8")
            val encodedCookie = java.net.URLEncoder.encode(targetCookie, "UTF-8")
            com.flixvora.data.network.LocalMovieBoxServer.buildCastProxyUrl(
                wifiIp = wifiIp,
                port = 3000,
                encodedTargetUrl = encodedTarget,
                encodedCookie = encodedCookie
            )
        } else {
            rawTargetUrl
        }

        // If media is already loaded for this url, don't reload
        val currentMedia = remoteMediaClient.mediaInfo
        if (currentMedia?.contentId == castUrl || currentMedia?.contentUrl == castUrl ||
            currentMedia?.contentId == rawTargetUrl || currentMedia?.contentUrl == rawTargetUrl) {
            return
        }

        val movieMetadata = com.google.android.gms.cast.MediaMetadata(
            if (subject.isTvShow) com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_TV_SHOW
            else com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_MOVIE
        ).apply {
            val title = if (subject.isTvShow) {
                "${subject.name} - S${viewModel.selectedSeason}E${viewModel.currentEpisode?.episodeNumber ?: 1}"
            } else {
                subject.name
            }
            putString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE, title)
            if (subject.isTvShow) {
                putString(com.google.android.gms.cast.MediaMetadata.KEY_SERIES_TITLE, subject.name)
                putInt(com.google.android.gms.cast.MediaMetadata.KEY_SEASON_NUMBER, viewModel.selectedSeason)
                putInt(com.google.android.gms.cast.MediaMetadata.KEY_EPISODE_NUMBER, viewModel.currentEpisode?.episodeNumber ?: 1)
            }
            if (subject.poster.isNotEmpty()) {
                addImage(com.google.android.gms.common.images.WebImage(android.net.Uri.parse(subject.poster)))
            }
        }

        val isM3u8 = rawTargetUrl.contains(".m3u8", ignoreCase = true)
        val isHevc = rawTargetUrl.contains("/h265/", ignoreCase = true) || rawTargetUrl.contains("hevc", ignoreCase = true)
        val mimeType = when {
            isM3u8 -> "application/x-mpegURL"
            isHevc -> "video/mp4; codecs=\"hev1.1.6.L93.B0, mp4a.40.2\""
            else -> "video/mp4"
        }

        val mediaInfo = com.google.android.gms.cast.MediaInfo.Builder(castUrl)
            .setStreamType(com.google.android.gms.cast.MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(movieMetadata)
            .build()

        val startPosition = viewModel.playbackProgressMs.coerceAtLeast(0L)
        val loadOptions = com.google.android.gms.cast.MediaLoadOptions.Builder()
            .setAutoplay(true)
            .setPlayPosition(startPosition)
            .build()

        android.util.Log.i("MovieScreens", "Loading media on Cast: url=$castUrl mimeType=$mimeType pos=$startPosition")
        val pendingResult = remoteMediaClient.load(mediaInfo, loadOptions)
        pendingResult.setResultCallback { result ->
            if (result.status.isSuccess) {
                android.util.Log.i("MovieScreens", "Cast media load command acknowledged successfully")
                if (context != null) {
                    val mediaTitle = if (subject.isTvShow) "${subject.name} S${viewModel.selectedSeason}E${viewModel.currentEpisode?.episodeNumber ?: 1}" else subject.name
                    com.flixvora.cast.CastForegroundService.start(context, mediaTitle)
                }
            } else {
                android.util.Log.w("MovieScreens", "Cast media load rejected: ${result.status.statusCode} ${result.status.statusMessage}")
            }
        }

        // Pause local player since TV is now playing
        viewModel.isPlaying = false
    } catch (e: Exception) {
        android.util.Log.e("MovieScreens", "Failed to cast media to TV", e)
    }
}

object VideoCacheManager {
    @Volatile
    private var simpleCache: androidx.media3.datasource.cache.SimpleCache? = null

    @Synchronized
    fun getCache(context: android.content.Context): androidx.media3.datasource.cache.SimpleCache {
        simpleCache?.let { return it }
        val cacheDir = java.io.File(context.cacheDir, "media_video_cache")
        val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024) // 300MB fast LRU disk cache
        val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
        return androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider).also { simpleCache = it }
    }

    /**
     * Token-agnostic cache key matching MovieBox's w.h0(url).
     * Extracts only scheme + authority + path, stripping signed token queries (?Expires, ?Signature, ?sign, ?t).
     * Guarantees 100% cache hits even across session token renewals.
     */
    fun getCacheKey(uri: android.net.Uri): String {
        val path = uri.path ?: ""
        return if (uri.scheme != null && uri.authority != null && path.isNotEmpty()) {
            "${uri.scheme}://${uri.authority}$path"
        } else {
            uri.toString()
        }
    }
}

@Composable
fun SplashScreen(
    isReady: Boolean,
    onFinish: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "splash_fade_in"
    )
    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 700,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(isReady) {
        val startTime = System.currentTimeMillis()
        val minDisplayTimeMs = 900L  // Brief branded moment, not a stall
        val maxWaitTimeMs = 3000L    // Safety timeout while content loads

        while (isActive) {
            val elapsed = System.currentTimeMillis() - startTime
            if (isReady && elapsed >= minDisplayTimeMs) {
                onFinish()
                break
            }
            if (elapsed >= maxWaitTimeMs) {
                onFinish()
                break
            }
            delay(50L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(PlasmaRed.copy(alpha = 0.30f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.42f),
                    radius = 0.9f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(PlasmaViolet.copy(alpha = 0.26f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0.62f, 0.72f),
                    radius = 0.9f
                )
            )
            .background(ObsidianDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // Brand mark with the signature plasma glow
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(BrandGradient)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_popup_logo),
                    contentDescription = "Flixvora Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Cinematic app wordmark
            Text(
                text = "FLIXVORA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "STREAM ENDLESS CINEMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Slim, premium loading indicator
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentPrimary,
                    trackColor = Color.Transparent,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Loading your library",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = TextTertiary
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MovieBoxApp(viewModel: MovieViewModel) {
    val trending by viewModel.trendingMovies.collectAsState()
    val categoryMovies by viewModel.categoryMovies.collectAsState()
    val isContentReady = remember(trending, categoryMovies, viewModel.isDashboardLoading) {
        (trending.isNotEmpty() || categoryMovies.isNotEmpty()) && !viewModel.isDashboardLoading
    }

    var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) with
                fadeOut(animationSpec = tween(400))
            },
            label = "splash_to_main_transition"
        ) { isSplashVisible ->
            if (isSplashVisible) {
                SplashScreen(
                    isReady = isContentReady,
                    onFinish = { showSplash = false }
                )
            } else {
                MainContent(viewModel)
            }
        }
    }
}

@Composable
fun ForceUpdateFullScreen(
    updateInfo: com.flixvora.ui.viewmodel.MovieViewModel.AppUpdateInfo,
    isDownloading: Boolean,
    progress: Int,
    statusText: String,
    hasCachedApk: Boolean,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onGoToDownloads: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF14040C), ObsidianDark, Color(0xFF101114))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Brand mark with plasma glow
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(BrandGradient)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_popup_logo),
                    contentDescription = "Flixvora",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Update Required to Continue",
                fontFamily = FontFamily.SansSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandGradient)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "v${updateInfo.latestVersionName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What's New in this update:",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateInfo.changelog.ifEmpty { "Critical improvements, stream playback fixes, and security updates." },
                        color = Color(0xFFA9ABB3),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (isDownloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { if (progress > 0) progress / 100f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentPrimary,
                        trackColor = Color.White.copy(alpha = 0.10f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (statusText.isNotEmpty()) "Downloading: $statusText" else "Downloading update…",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Show Install button if APK is already cached, otherwise Download
                if (hasCachedApk) {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(BrandGradient),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Install",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Install Update",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(BrandGradient),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update Now",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onGoToDownloads,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Offline Downloads",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Offline Downloads",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    updateInfo: com.flixvora.ui.viewmodel.MovieViewModel.AppUpdateInfo,
    isDownloading: Boolean,
    progress: Int,
    statusText: String,
    hasCachedApk: Boolean,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, OutlineSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand mark with plasma glow
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_popup_logo),
                        contentDescription = "Flixvora",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Update Available",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandGreen.copy(alpha = 0.14f))
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${updateInfo.latestVersionName}",
                        color = BrandGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                if (updateInfo.changelog.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceOverlay.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OutlineSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateInfo.changelog,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A new update is available with improvements and fixes.",
                        color = Color(0xFFA9ABB3),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { if (progress > 0) progress / 100f else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentPrimary,
                            trackColor = Color.White.copy(alpha = 0.10f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (statusText.isNotEmpty()) "Downloading: $statusText" else "Downloading update…",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Show Install button if APK is already cached, otherwise Download
                    if (hasCachedApk) {
                        Button(
                            onClick = onInstallClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = BrandGreenInk
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Install",
                                tint = BrandGreenInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Install Update",
                                color = BrandGreenInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onUpdateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = BrandGreenInk
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download",
                                tint = BrandGreenInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Download & Install",
                                color = BrandGreenInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!updateInfo.isForceUpdate) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Later",
                                color = Color(0xFFA9ABB3),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MbTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    // Flat bar, but the active tab carries a short accent rule above the icon
    // and eases its colour in. A hard colour swap reads as a glitch at 60fps.
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) BrandGreen else TextTertiary,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "tab_tint"
    )
    val indicatorWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 16.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.7f,
            stiffness = 900f
        ),
        label = "tab_indicator"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = 6.dp, bottom = 8.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrandGreen else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
fun MainContent(viewModel: MovieViewModel) {
    var activeTab by remember { mutableStateOf("home") } // home, search, watchlist, history, account
    val updateInfo = viewModel.availableUpdateInfo
    val isForceUpdateActive = updateInfo?.isForceUpdate == true
    var forceUpdateAllowDownloadsOnly by remember { mutableStateOf(false) }

    var homeSearchActive by remember { mutableStateOf(false) }
    val homeSearchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(homeSearchActive) {
        if (homeSearchActive) {
            kotlinx.coroutines.delay(60)
            runCatching { homeSearchFocusRequester.requestFocus() }
        }
    }
    BackHandler(enabled = homeSearchActive) {
        homeSearchActive = false
        viewModel.searchMovies("")
    }

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    // Back navigation across tabs
    BackHandler(enabled = activeTab == "manage_categories" || activeTab == "history") {
        activeTab = "settings"
    }

    BackHandler(enabled = activeTab != "home" && activeTab != "manage_categories" && activeTab != "history") {
        activeTab = "home"
    }

    // Cancel loading stream links on back
    BackHandler(enabled = viewModel.isPlaybackLoading) {
        viewModel.isPlaybackLoading = false
    }

    // Double-tap back on home screen to exit app safely
    BackHandler(
        enabled = activeTab == "home" &&
            viewModel.selectedSubject == null &&
            viewModel.playInfo == null &&
            viewModel.playingDownloadedFile == null &&
            !viewModel.isPlaybackLoading
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (viewModel.playInfo == null && viewModel.playingDownloadedFile == null && !viewModel.isLiveTvPlaying) {
                    // Flat opaque tab bar, edge to edge, hairline top rule — the
                    // demo has no floating dock and no rounded pill.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceBase)
                            .navigationBarsPadding()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(OutlineSubtle)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MbTabItem("Home", Icons.Filled.Home, activeTab == "home", "nav_home") { activeTab = "home" }
                            MbTabItem("Search", Icons.Filled.Search, activeTab == "search", "nav_search") { activeTab = "search" }
                            MbTabItem("Live TV", Icons.Filled.LiveTv, activeTab == "livetv", "nav_livetv") { activeTab = "livetv" }
                            MbTabItem("Downloads", Icons.Filled.Download, activeTab == "download", "nav_download") { activeTab = "download" }
                            MbTabItem("Me", Icons.Filled.Person, activeTab == "settings", "nav_me") { activeTab = "settings" }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (viewModel.isLiveTvPlaying) PaddingValues(0.dp) else innerPadding)
            ) {
                when (activeTab) {
                    "home" -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            HomeScreen(viewModel, onSearchClick = { activeTab = "search" }, onNavigate = { activeTab = it })
                            if (homeSearchActive) {
                                // Opaque search surface that appears immediately: the home
                                // feed must never show through, not even during a fade. The
                                // aurora glow is the backdrop, and the plasma wash keeps the
                                // search bar on the same gradient it had on the home hero.
                                AuroraBackground {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            PlasmaRed.copy(alpha = 0.32f),
                                                            PlasmaViolet.copy(alpha = 0.18f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        SearchContent(
                                            viewModel = viewModel,
                                            showSearchBar = true,
                                            focusRequester = homeSearchFocusRequester,
                                            onBack = {
                                                homeSearchActive = false
                                                viewModel.searchMovies("")
                                            },
                                            contentPadding = PaddingValues(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 12.dp,
                                                bottom = 16.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "search" -> SearchTabScreen(viewModel)
                    "livetv" -> LiveTvScreen(viewModel)
                    "watchlist" -> WatchlistScreen(
                        viewModel,
                        onBack = { activeTab = "home" }
                    )
                    "download" -> DownloadScreen(viewModel)
                    "settings" -> SettingsScreen(
                        viewModel, 
                        onNavigateToHistory = { activeTab = "history" },
                        onNavigateToWatchlist = { activeTab = "watchlist" },
                        onNavigateToManageCategories = { activeTab = "manage_categories" }
                    )
                    "manage_categories" -> ManageCategoriesScreen(
                        viewModel,
                        onBack = { activeTab = "settings" }
                    )
                    "history" -> HistoryScreen(
                        viewModel,
                        onBack = { activeTab = "settings" }
                    )
                    "rankings" -> RankingsScreen(viewModel, onBack = { activeTab = "home" })
                    "genres" -> GenreFilterScreen(viewModel, onBack = { activeTab = "home" })
                    "shorts" -> ShortsScreen(viewModel, onBack = { activeTab = "home" })
                    "sports" -> SportsScreen(viewModel, onBack = { activeTab = "home" })
                    "games" -> GamesScreen(viewModel, onBack = { activeTab = "home" })
                }
            }
        }



        // Detail screen slide-up overlay
        AnimatedVisibility(
            visible = viewModel.selectedSubject != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
        ) {
            DetailScreen(viewModel)
        }

        // Simulated Video Player overlay
        AnimatedVisibility(
            visible = viewModel.playInfo != null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            PlayerScreen(viewModel)
        }

        // Offline Download Video Player overlay
        AnimatedVisibility(
            visible = viewModel.playingDownloadedFile != null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            val file = viewModel.playingDownloadedFile
            if (file != null) {
                OfflinePlayerScreen(
                    fileToPlay = file,
                    onDismiss = { viewModel.playingDownloadedFile = null }
                )
            }
        }

        // Playback loading overlay
        if (viewModel.isPlaybackLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // block all clicks while loading
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CinematicRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching stream links...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // In-App Smart/Force Update Screen
        val updateInfo = viewModel.availableUpdateInfo
        val hasCachedApk = viewModel.hasValidCachedUpdate()
        if (updateInfo != null && viewModel.playInfo == null) {
            if (updateInfo.isForceUpdate) {
                if (activeTab != "download") {
                    ForceUpdateFullScreen(
                        updateInfo = updateInfo,
                        isDownloading = viewModel.isDownloadingUpdate,
                        progress = viewModel.updateDownloadProgress,
                        statusText = viewModel.updateStatusMessage,
                        hasCachedApk = hasCachedApk,
                        onUpdateClick = {
                            viewModel.downloadAndInstallUpdate(context, updateInfo.downloadUrl, updateInfo.sha256)
                        },
                        onInstallClick = {
                            viewModel.installCachedUpdate(context)
                        },
                        onGoToDownloads = {
                            activeTab = "download"
                        }
                    )
                }
            } else {
                AppUpdateDialog(
                    updateInfo = updateInfo,
                    isDownloading = viewModel.isDownloadingUpdate,
                    progress = viewModel.updateDownloadProgress,
                    statusText = viewModel.updateStatusMessage,
                    hasCachedApk = hasCachedApk,
                    onUpdateClick = {
                        viewModel.downloadAndInstallUpdate(context, updateInfo.downloadUrl, updateInfo.sha256)
                    },
                    onInstallClick = {
                        viewModel.installCachedUpdate(context)
                    },
                    onDismiss = {
                        viewModel.dismissUpdate()
                    }
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1B1F),
                Color(0xFF28292E),
                Color(0xFF1A1B1F),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun DashboardShimmerScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Shimmer Hero Banner Placeholder
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .shimmerEffect()
            ) {
                // Bottom shading gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianDark)
                            )
                        )
                )
                
                // Text lines placeholders
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // Horizontal Row Placeholder 1
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 12.dp)
                        .width(140.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) {
                        Column(modifier = Modifier.width(110.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips placeholder
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // Grid items placeholder
        items(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MovieGridItem(
    movie: MovieSubject,
    modifier: Modifier = Modifier.width(110.dp),
    showRating: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val optimalPosterUrl = androidx.compose.runtime.remember(movie.poster) {
                com.flixvora.util.ImageOptimizer.getOptimalUrl(movie.poster, 360)
            }
            val placeholderColor = androidx.compose.runtime.remember(movie.averageHue) {
                com.flixvora.util.ImageOptimizer.parseAverageHue(movie.averageHue)
            }
            val placeholderPainter = androidx.compose.runtime.remember(movie.blurHash, placeholderColor) {
                com.flixvora.util.ImageOptimizer.getBlurHashPainter(movie.blurHash, 20, 20)
                    ?: androidx.compose.ui.graphics.painter.ColorPainter(placeholderColor)
            }

            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(optimalPosterUrl)
                    .memoryCacheKey(optimalPosterUrl)
                    .diskCacheKey(optimalPosterUrl)
                    .size(width = 330, height = 480) // Decode at exact 110dp x 160dp card resolution (~3x density) like Vega
                    .precision(coil.size.Precision.INEXACT) // Fast decoding without full uncompressed bitmap in RAM
                    .crossfade(false)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!movie.year.isNullOrBlank() && movie.year != "Not available") {
            Text(
                text = movie.year,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SearchContent(
    viewModel: MovieViewModel,
    showSearchBar: Boolean = true,
    focusRequester: FocusRequester? = null,
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val results by viewModel.searchResults.collectAsState()
    var searchInput by remember { mutableStateOf(viewModel.searchQuery) }
    var isSearchSubmitted by remember { mutableStateOf(false) }
    val searchListState = rememberLazyListState()

    LaunchedEffect(viewModel.customCountryIso) {
        viewModel.loadSearchSuggestions()
    }

    // Auto-load more results when scrolling near the bottom of search results
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = searchListState.layoutInfo.totalItemsCount
            val lastVisible = searchListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && viewModel.canSearchLoadMore && !viewModel.isSearchLoadingMore) {
            viewModel.loadMoreSearchResults()
        }
    }

    BackHandler(enabled = searchInput.isNotEmpty()) {
        searchInput = ""
        isSearchSubmitted = false
        viewModel.searchMovies("")
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(contentPadding)
    ) {
        if (showSearchBar) {
        // Search bar morphs in: springs up from the top with a slight overshoot,
        // and its border warms up as the user types. Back button rides alongside.
        val barEnter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            barEnter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
        }
        val borderAlpha by animateFloatAsState(
            targetValue = if (searchInput.isNotEmpty()) 1f else 0.35f,
            animationSpec = tween(220),
            label = "search_border"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer {
                    alpha = barEnter.value
                    translationY = (1f - barEnter.value) * -30f
                    scaleX = 0.82f + 0.18f * barEnter.value
                    scaleY = 0.90f + 0.10f * barEnter.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
        ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
        OutlinedTextField(
            value = searchInput,
            onValueChange = {
                searchInput = it
                isSearchSubmitted = false
                viewModel.searchMovies(it)
            },
            placeholder = { Text("Search movies, shows…", color = TextSecondary) },
            leadingIcon = {
                IconButton(onClick = {
                    if (searchInput.isNotBlank()) {
                        isSearchSubmitted = true
                        viewModel.searchMovies(searchInput, immediate = true)
                        focusManager.clearFocus()
                    }
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (searchInput.isNotBlank()) CinematicRed else TextSecondary)
                }
            },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = {
                        searchInput = ""
                        isSearchSubmitted = false
                        viewModel.searchMovies("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchInput.isNotBlank()) {
                        isSearchSubmitted = true
                        viewModel.searchMovies(searchInput, immediate = true)
                        focusManager.clearFocus()
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = CinematicRed
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("search_text_input")
                .glassSurface(RoundedCornerShape(16.dp), tint = 1.6f)
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(
                            PlasmaRed.copy(alpha = borderAlpha),
                            PlasmaViolet.copy(alpha = borderAlpha * 0.85f),
                            PlasmaCyan.copy(alpha = borderAlpha * 0.7f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        )
        }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions by viewModel.searchSuggestions.collectAsState()

        if (viewModel.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinematicRed)
            }
        } else if (!isSearchSubmitted || searchInput.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = if (searchInput.isEmpty()) "Trending Searches" else "Suggestions",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                
                itemsIndexed(suggestions) { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSurface(RoundedCornerShape(12.dp))
                            .bounceClick {
                                searchInput = suggestion
                                isSearchSubmitted = true
                                viewModel.searchMovies(suggestion, immediate = true)
                                focusManager.clearFocus()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "${index + 1}",
                                color = if (index < 3) CinematicRed else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.width(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Suggestion",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchInput.isEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = "Trending",
                                tint = if (index < 3) CinematicRed else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, contentDescription = "No Results", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No results found for \"$searchInput\"", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                state = searchListState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSurface(RoundedCornerShape(14.dp))
                            .bounceClick {
                                focusManager.clearFocus()
                                viewModel.selectSubject(movie)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val searchThumb = androidx.compose.runtime.remember(movie.poster) {
                            com.flixvora.util.ImageOptimizer.getOptimalUrl(movie.poster, 180)
                        }
                        AsyncImage(
                            model = searchThumb,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(com.flixvora.util.ImageOptimizer.parseAverageHue(movie.averageHue, androidx.compose.ui.graphics.Color.DarkGray)),
                            error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray),
                            modifier = Modifier
                                .size(width = 60.dp, height = 90.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (movie.genres.isNotEmpty()) {
                                Text(movie.genres.joinToString(", "), fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!movie.rating.isNullOrBlank() && movie.rating != "Not available") {
                                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(movie.rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(movie.year ?: "", fontSize = 12.sp, color = TextSecondary)
                                if (movie.isTvShow) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("TV", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.isSearchLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CinematicRed,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTabScreen(viewModel: MovieViewModel) {
    val results by viewModel.searchResults.collectAsState()
    var query by remember { mutableStateOf("") }
    var activeGenre by remember { mutableStateOf("All") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    val genres = remember {
        listOf(
            "All", "Action", "Drama", "Comedy", "Thriller", "Romance",
            "Horror", "Sci-Fi", "Fantasy", "Crime", "Anime", "Animation"
        )
    }

    // Filtering happens locally over the already-fetched page set; the demo's
    // chips narrow the visible grid without issuing a new query.
    val visible = remember(results, activeGenre) {
        if (activeGenre == "All") results
        else results.filter { m -> m.genres.any { it.equals(activeGenre, ignoreCase = true) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(safeTopInset() + 10.dp))

        Text(
            text = "Search",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))

        com.flixvora.ui.components.MbSearchBar(
            query = query,
            onQueryChange = {
                query = it
                viewModel.searchMovies(it)
            }
        )
        Spacer(Modifier.height(10.dp))

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(genres) { genre ->
                com.flixvora.ui.components.MbGenreChip(
                    label = genre,
                    selected = activeGenre == genre,
                    onClick = { activeGenre = genre }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "${visible.size} results",
            fontSize = 11.sp,
            color = TextTertiary
        )
        Spacer(Modifier.height(10.dp))

        if (visible.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.isBlank()) "Search for a movie or show" else "No results found",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                state = gridState,
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible.size) { index ->
                    val movie = visible[index]
                    com.flixvora.ui.components.MbPosterCard(
                        movie = movie,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.selectSubject(movie)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WatchlistScreen(viewModel: MovieViewModel, onBack: () -> Unit = {}) {
    val favorites by viewModel.favoritesList.collectAsState()

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Watchlist", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = "Watchlist empty", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your watchlist is empty", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Maps entities back to subjects
                                val subjectId = if (viewModel.isAniFam) {
                                    if (movie.id.startsWith("anifam_")) movie.id else "anifam_${movie.id}"
                                } else {
                                    movie.id
                                }
                                val subject = MovieSubject(
                                    id = subjectId,
                                    name = movie.name,
                                    poster = movie.poster,
                                    isTvShow = movie.isTvShow,
                                    rating = movie.rating,
                                    year = movie.year
                                )
                                viewModel.selectSubject(subject)
                            }
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.poster,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop, placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray), error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray),
                            modifier = Modifier
                                .size(width = 50.dp, height = 75.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text((movie.year ?: "Not available") + " • " + if (movie.isTvShow) "TV Show" else "Movie", fontSize = 13.sp, color = TextSecondary)
                        }
                        IconButton(onClick = {
                            val subjectId = if (viewModel.isAniFam) {
                                if (movie.id.startsWith("anifam_")) movie.id else "anifam_${movie.id}"
                            } else {
                                movie.id
                            }
                            val subject = MovieSubject(id = subjectId, name = movie.name, poster = movie.poster)
                            viewModel.toggleFavorite(subject)
                        }) {
                            Icon(Icons.Filled.BookmarkRemove, contentDescription = "Remove", tint = CinematicRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MovieViewModel, onBack: () -> Unit = {}) {
    val history by viewModel.historyList.collectAsState()
    var expandedSubjectId by remember { mutableStateOf<String?>(null) }

    val groupedHistory = remember(history) {
        history.groupBy { it.subjectId }.map { entry ->
            entry.value.first() to entry.value
        }
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue Watching", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (groupedHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayCircleOutline, contentDescription = "History empty", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No playback history available", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedHistory) { (item, episodesList) ->
                    val isExpanded = expandedSubjectId == item.subjectId

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.isTvShow) {
                                        expandedSubjectId = if (isExpanded) null else item.subjectId
                                    } else {
                                        // Movie: play directly
                                        val subject = MovieSubject(
                                            id = item.subjectId,
                                            name = item.name,
                                            poster = item.poster,
                                            isTvShow = item.isTvShow
                                        )
                                        viewModel.selectSubject(subject)
                                        viewModel.selectMovieDirect()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.poster,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop, placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray), error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray),
                                modifier = Modifier
                                    .size(width = 60.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (item.isTvShow) {
                                    Text(
                                        text = "${episodesList.size} episodes watched • Tap to view",
                                        fontSize = 13.sp,
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    val percentage = if (item.totalTime > 0) (item.seeTime.toFloat() / item.totalTime.toFloat()) else 0f
                                    val formattedProgress = formatTime(item.seeTime) + " / " + formatTime(item.totalTime)
                                    Text(formattedProgress, fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = percentage.coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = CinematicRed,
                                        trackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                episodesList.forEach { viewModel.deleteHistoryItem(it.id) }
                            }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete All", tint = TextSecondary)
                            }
                        }

                        if (item.isTvShow && isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 16.dp, end = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                episodesList.forEach { epItem ->
                                    val percentage = if (epItem.totalTime > 0) (epItem.seeTime.toFloat() / epItem.totalTime.toFloat()) else 0f
                                    val formattedProgress = formatTime(epItem.seeTime) + " / " + formatTime(epItem.totalTime)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val subject = MovieSubject(
                                                    id = epItem.subjectId,
                                                    name = epItem.name,
                                                    poster = epItem.poster,
                                                    isTvShow = epItem.isTvShow
                                                )
                                                viewModel.selectSubject(subject)
                                                val episode = Episode(
                                                    episodeId = epItem.id,
                                                    title = "Season ${epItem.season} Episode ${epItem.episode}",
                                                    episodeNumber = epItem.episode,
                                                    seasonNumber = epItem.season
                                                )
                                                viewModel.selectEpisode(episode)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Season ${epItem.season} Ep ${epItem.episode}", fontSize = 13.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(formattedProgress, fontSize = 11.sp, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = percentage.coerceIn(0f, 1f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(1.dp)),
                                                color = CinematicRed,
                                                trackColor = Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteHistoryItem(epItem.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabScreen(viewModel: MovieViewModel) {
    var hostText by remember { mutableStateOf(viewModel.mockHost) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Science, contentDescription = "Laboratory", tint = AccentAmber, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("BFF Laboratory Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Sandbox & geofencing simulator config", fontSize = 13.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Simulate Region Carrier Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Overrides SIM parameters in API handshake headers", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(16.dp))

                // Region choices matching protocol Section 9
                val regions = listOf(
                    Triple("90101", "us", "United States (Global)"),
                    Triple("40401", "in", "India (Hindi Dubs)"),
                    Triple("62101", "ng", "Nigeria (Nollywood)")
                )

                regions.forEach { reg ->
                    val isSelected = viewModel.customMcc == reg.first && viewModel.customCountryIso == reg.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.applyCustomSandbox(reg.first, reg.second) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.applyCustomSandbox(reg.first, reg.second) },
                            colors = RadioButtonDefaults.colors(selectedColor = CinematicRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(reg.third, color = TextPrimary, fontSize = 14.sp)
                            Text("MCC: ${reg.first} | ISO: ${reg.second}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BFF Proxy Host Configuration", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Route API traffic through your custom cloud instance", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("Server Host URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CinematicRed,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = CinematicRed,
                        cursorColor = CinematicRed
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("mock_host_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultHost = "http://127.0.0.1:3000"
                            hostText = defaultHost
                            viewModel.updateMockHost(defaultHost)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Local Server", color = AccentAmber, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            hostText = "http://127.0.0.1:3000"
                            viewModel.updateMockHost("http://127.0.0.1:3000")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Default Local", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateMockHost(hostText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Apply Host", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.resetSandbox() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-Lock Sandbox & Exit Lab", color = CinematicRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlayerScreen(viewModel: MovieViewModel) {
    val playInfo = viewModel.playInfo ?: return
    val subject = viewModel.selectedSubject ?: return
    val context = LocalContext.current

    val selectedStream = viewModel.selectedStream
    val rawUrl = selectedStream?.url ?: ""
    val streamUrl = if (rawUrl == "https://macdn.aoneroom.com/other/2026/09/04/b164fbfb4347792950bdfbfb563d39d9.mp4") "" else rawUrl
    val selectedCookie = selectedStream?.cookie ?: ""
    val subtitleUrl = viewModel.selectedSubtitle?.url

    var activeSheet by remember { mutableStateOf<String?>(null) }
    var isDownloadSelectionMode by remember { mutableStateOf(false) }
    var selectedDownloadQuality by remember { mutableStateOf(viewModel.defaultDownloadQuality.ifEmpty { "1080p" }) }
    val selectedDownloadEpisodes = remember { androidx.compose.runtime.mutableStateListOf<com.flixvora.data.model.Episode>() }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isFullscreen = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val playerHeight = minOf(screenWidth * (9f / 16f), screenHeight * 0.32f)
    var isControllerVisible by remember { mutableStateOf(true) }
    // v7 BOUNCE: fullscreen episode picker lives behind a top-right pill instead
    // of a permanent side panel.
    var showFsEpisodeMenu by remember { mutableStateOf(false) }

    // Sleep timer: 0 = off. Counts down while playing, then pauses playback.
    var sleepTimerMinutes by remember { mutableStateOf(0) }
    var sleepTimerRemainingMs by remember { mutableStateOf(0L) }

    // Double-tap Gesture Indicator States
    var doubleTapActionText by remember { mutableStateOf<String?>(null) }
    var doubleTapActionSide by remember { mutableStateOf<String?>(null) } // "left", "center", "right"
    var gestureAnimTrigger by remember { mutableStateOf(0L) }

    BackHandler {
        if (activeSheet != null) {
            activeSheet = null
        } else if (isDownloadSelectionMode) {
            isDownloadSelectionMode = false
        } else if (isFullscreen) {
            val activity = context as? android.app.Activity
            if (activity != null) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                com.flixvora.ui.components.setSystemBarsVisible(activity, true)
            }
        } else {
            viewModel.playInfo = null
            val activity = context as? android.app.Activity
            if (activity != null) {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                com.flixvora.ui.components.setSystemBarsVisible(activity, true)
            }
        }
    }

    // Google Cast Session State & Listener
    var isCastConnected by remember { mutableStateOf(false) }
    val castContext = remember {
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(context)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(castContext, streamUrl) {
        val sessionManager = castContext?.sessionManager
        val castListener = object : com.google.android.gms.cast.framework.SessionManagerListener<com.google.android.gms.cast.framework.CastSession> {
            override fun onSessionStarted(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {
                isCastConnected = true
                loadMediaOnCast(session, streamUrl, subject, viewModel, context)
            }
            override fun onSessionResumed(session: com.google.android.gms.cast.framework.CastSession, wasSuspended: Boolean) {
                isCastConnected = true
                loadMediaOnCast(session, streamUrl, subject, viewModel, context)
            }
            override fun onSessionEnded(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
                isCastConnected = false
                com.flixvora.cast.CastForegroundService.stop(context)
            }
            override fun onSessionSuspended(session: com.google.android.gms.cast.framework.CastSession, reason: Int) {
                isCastConnected = false
                com.flixvora.cast.CastForegroundService.stop(context)
            }
            override fun onSessionStarting(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionStartFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
                isCastConnected = false
                com.flixvora.cast.CastForegroundService.stop(context)
            }
            override fun onSessionEnding(session: com.google.android.gms.cast.framework.CastSession) {}
            override fun onSessionResuming(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
                isCastConnected = false
                com.flixvora.cast.CastForegroundService.stop(context)
            }
        }

        // Check current session immediately
        val currentSession = sessionManager?.currentCastSession
        if (currentSession != null && currentSession.isConnected) {
            isCastConnected = true
            loadMediaOnCast(currentSession, streamUrl, subject, viewModel, context)
        }

        sessionManager?.addSessionManagerListener(castListener, com.google.android.gms.cast.framework.CastSession::class.java)
        onDispose {
            sessionManager?.removeSessionManagerListener(castListener, com.google.android.gms.cast.framework.CastSession::class.java)
        }
    }

    // Initialize Media3 ExoPlayer
    val exoPlayer = remember {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setMediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
                if (mimeType.startsWith("video/")) {
                    decoders.sortedWith { d1, d2 ->
                        val n1 = d1.name.lowercase()
                        val n2 = d2.name.lowercase()
                        val sw1 = n1.contains("google") || n1.contains("c2.android") || n1.contains("sw")
                        val sw2 = n2.contains("google") || n2.contains("c2.android") || n2.contains("sw")
                        when {
                            !sw1 && sw2 -> -1 // Priority 1: Hardware decoders (Adreno, Exynos, Mali, Tensor)
                            sw1 && !sw2 -> 1  // Priority 2: Software fallback
                            else -> 0
                        }
                    }
                } else {
                    decoders
                }
            }
        }
        
        // Dynamic Hardware-Based Buffering (Vega-Style)
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamBytes = memoryInfo.totalMem
        val heapLimitBytes = activityManager.memoryClass * 1024L * 1024L
        
        val safeBufferBytes = minOf(
            (heapLimitBytes * 0.35).toLong(),
            (totalRamBytes / 32)
        ).toInt()
        val targetBufferBytes = safeBufferBytes.coerceAtLeast(32 * 1024 * 1024) // Fallback to 32MB min
        
        val allocator = androidx.media3.exoplayer.upstream.DefaultAllocator(true, androidx.media3.common.C.DEFAULT_BUFFER_SEGMENT_SIZE)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                25000,   // minBufferMs (25 seconds minimum buffer before idle)
                120000,  // maxBufferMs (2 minutes max buffer limit)
                500,     // bufferForPlaybackMs (Only 0.5s needed to start instantly, matching MovieBox np.d.java!)
                1500     // bufferForPlaybackAfterRebufferMs (Only 1.5 seconds needed to recover)
            )
            .setTargetBufferBytes(targetBufferBytes) // Dynamically calculated safe limit
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30000, true) // Keep 30 seconds of previous video in RAM
            .build()

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, false)
            .build().apply {
                volume = 1.0f
                playWhenReady = viewModel.isPlaying
            }
    }

    // Sync native playlist next/prev buttons transitions with ViewModel selections
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                viewModel.isPlaying = playWhenReady
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null && mediaItem.localConfiguration?.uri?.toString() == "http://dummy.mp4") {
                    val targetEpisodeId = mediaItem.mediaId
                    val targetEpisode = viewModel.episodesList.find { it.episodeId == targetEpisodeId }
                    if (targetEpisode != null) {
                        viewModel.selectEpisode(targetEpisode)
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    var currentProgressMs by remember { mutableStateOf(viewModel.playbackProgressMs) }
    var durationMs by remember { mutableStateOf(viewModel.mediaDurationMs) }

    // Update stream and subtitle configuration
    LaunchedEffect(streamUrl, subtitleUrl) {
        if (streamUrl.isNotEmpty()) {
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent(if (viewModel.isAniFam) "okhttp/4.12.0" else "MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)")
                .setAllowCrossProtocolRedirects(true)
            
            val requestHeaders = mutableMapOf<String, String>()
            if (viewModel.isAniFam || streamUrl.contains("dramahot.top") || streamUrl.contains("zokoanime")) {
                requestHeaders["Referer"] = "https://zokoanime.video/"
                requestHeaders["Origin"] = "https://zokoanime.video"
            }
            if (selectedCookie.isNotEmpty()) {
                if (selectedCookie.contains("=") && (selectedCookie.contains("Referer=") || selectedCookie.contains("Origin="))) {
                    selectedCookie.split(";").forEach { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) {
                            requestHeaders[parts[0].trim()] = parts[1].trim()
                        }
                    }
                } else {
                    requestHeaders["Cookie"] = selectedCookie
                }
            }
            if (requestHeaders.isNotEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(requestHeaders)
            }

            // Wrap with on-disk LRU media cache for zero-stutter seeks and zero repeated downloads
            val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setCache(VideoCacheManager.getCache(context))
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setCacheKeyFactory { VideoCacheManager.getCacheKey(it.uri) }
                .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(streamUrl))
            if (!subtitleUrl.isNullOrEmpty()) {
                val mimeType = if (subtitleUrl.endsWith(".vtt", ignoreCase = true)) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(mimeType)
                    .setLanguage(viewModel.selectedSubtitle?.language ?: "en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
            }

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                cacheDataSourceFactory,
                androidx.media3.extractor.DefaultExtractorsFactory()
            )
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())

            val currentEpisode = viewModel.currentEpisode
            val episodes = viewModel.episodesList
            if (subject.isTvShow && episodes.isNotEmpty() && currentEpisode != null) {
                val currentIndex = episodes.indexOfFirst { it.episodeId == currentEpisode.episodeId }
                if (currentIndex != -1) {
                    val mediaSources = episodes.map { ep ->
                        if (ep.episodeId == currentEpisode.episodeId) {
                            mediaSource
                        } else {
                            mediaSourceFactory.createMediaSource(
                                MediaItem.Builder()
                                    .setMediaId(ep.episodeId)
                                    .setUri(Uri.parse("http://dummy.mp4"))
                                    .build()
                            )
                        }
                    }
                    exoPlayer.setMediaSources(mediaSources, currentIndex, viewModel.playbackProgressMs)
                } else {
                    exoPlayer.setMediaSource(mediaSource)
                }
            } else {
                exoPlayer.setMediaSource(mediaSource)
            }
            exoPlayer.prepare()
            if (viewModel.playbackProgressMs > 0) {
                exoPlayer.seekTo(viewModel.playbackProgressMs)
            }
            if (viewModel.isPlaying) {
                exoPlayer.play()
            }
        }
    }

    // Play/Pause sync
    LaunchedEffect(viewModel.isPlaying) {
        if (viewModel.isPlaying != exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = viewModel.isPlaying
        }
    }



    // Buffering listener
    var isBuffering by remember { mutableStateOf(false) }
    // Auto-play next episode: seconds remaining in the countdown (null = hidden)
    var autoNextCountdown by remember { mutableStateOf<Int?>(null) }
    var autoNextEnabled by remember { mutableStateOf(true) }
    val hasNextEpisode = viewModel.currentEpisode != null && run {
        val idx = viewModel.episodesList.indexOfFirst { it.episodeId == viewModel.currentEpisode?.episodeId }
        idx != -1 && idx < viewModel.episodesList.size - 1
    }
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == androidx.media3.common.Player.STATE_BUFFERING)
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    if (autoNextEnabled && hasNextEpisode) {
                        autoNextCountdown = 8
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Drive the auto-next countdown, then advance to the next episode.
    LaunchedEffect(autoNextCountdown, viewModel.currentEpisode?.episodeId) {
        val start = autoNextCountdown ?: return@LaunchedEffect
        if (start <= 0) {
            autoNextCountdown = null
            viewModel.playNextEpisode()
            return@LaunchedEffect
        }
        delay(1000)
        autoNextCountdown = start - 1
    }

    // Auto-hide controller when playing
    LaunchedEffect(isControllerVisible, viewModel.isPlaying) {
        if (isControllerVisible && viewModel.isPlaying) {
            delay(4000)
            isControllerVisible = false
        }
    }

    // Speed sync
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    LaunchedEffect(selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    // Dynamic resolution constraint selector
    LaunchedEffect(selectedStream) {
        val q = selectedStream?.quality ?: "Auto"
        val (maxWidth, maxHeight) = when {
            q.contains("4k", ignoreCase = true) || q.contains("2160") -> Pair(3840, 2160)
            q.contains("2k", ignoreCase = true) || q.contains("1440") -> Pair(2560, 1440)
            q.contains("1080") -> Pair(1920, 1080)
            q.contains("720") -> Pair(1280, 720)
            q.contains("480") -> Pair(854, 480)
            q.contains("360") -> Pair(640, 360)
            q.contains("240") -> Pair(426, 240)
            else -> Pair(Integer.MAX_VALUE, Integer.MAX_VALUE)
        }
        try {
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
            builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false)
            if (viewModel.isAniFam) {
                val isDub = viewModel.selectedResource?.resourceId?.lowercase()?.contains("dub") == true ||
                            viewModel.selectedResource?.name?.lowercase()?.contains("dub") == true
                if (isDub) {
                    builder.setPreferredAudioLanguage("en")
                } else {
                    builder.setPreferredAudioLanguage("ja")
                }
            }
            if (q == "Auto") {
                builder.clearVideoSizeConstraints()
                builder.setForceHighestSupportedBitrate(false)
            } else {
                builder.setMaxVideoSize(maxWidth, maxHeight)
                // Force ExoPlayer to pick the highest possible quality within this cap immediately
                // instead of waiting for adaptive bandwidth estimation to trigger a switch
                builder.setForceHighestSupportedBitrate(true)
            }
            exoPlayer.trackSelectionParameters = builder.build()
        } catch (e: Exception) {
            // Log fallback or safety
        }
    }

    // Dynamic audio track preference sync for AniFam
    LaunchedEffect(viewModel.selectedResource) {
        if (viewModel.isAniFam) {
            try {
                val isDub = viewModel.selectedResource?.resourceId?.lowercase()?.contains("dub") == true ||
                            viewModel.selectedResource?.name?.lowercase()?.contains("dub") == true
                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false)
                if (isDub) {
                    builder.setPreferredAudioLanguage("en")
                } else {
                    builder.setPreferredAudioLanguage("ja")
                }
                exoPlayer.trackSelectionParameters = builder.build()
            } catch (_: Exception) {}
        }
    }

    // Listen to player progress ticking
    LaunchedEffect(viewModel.isPlaying) {
        if (viewModel.isPlaying) {
            while (true) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0) {
                    currentProgressMs = pos
                    durationMs = dur
                    viewModel.updatePlaybackProgress(pos, dur)
                }
                delay(1000)
            }
        }
    }

    // Sleep timer countdown — pauses playback when it reaches zero.
    LaunchedEffect(sleepTimerMinutes, viewModel.isPlaying) {
        if (sleepTimerMinutes <= 0) {
            sleepTimerRemainingMs = 0L
            return@LaunchedEffect
        }
        if (sleepTimerRemainingMs <= 0L) {
            sleepTimerRemainingMs = sleepTimerMinutes * 60_000L
        }
        while (sleepTimerRemainingMs > 0L && viewModel.isPlaying) {
            delay(1000)
            sleepTimerRemainingMs -= 1000L
        }
        if (sleepTimerRemainingMs <= 0L) {
            exoPlayer.playWhenReady = false
            viewModel.isPlaying = false
            sleepTimerMinutes = 0
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            // The 1s ticker stops as soon as playback pauses, so a seek made
            // while paused (or a back press right after one) was never written.
            // Flush the final position before the player goes away.
            try {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos > 0 && dur > 0) viewModel.updatePlaybackProgress(pos, dur)
            } catch (_: Exception) {}
            exoPlayer.release()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val activity = context as? android.app.Activity
    val isInPip = activity?.isInPictureInPictureMode ?: false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
        ) {

            // Player Container (Always present in tree at index 0)
        Box(
            modifier = if (isFullscreen || isInPip) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(playerHeight)
            }
        ) {
            if (streamUrl.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx).inflate(com.flixvora.R.layout.texture_player_view, null) as androidx.media3.ui.PlayerView
                        view.keepScreenOn = true
                        view.isHapticFeedbackEnabled = false
                        view.isSoundEffectsEnabled = false
                        view.layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        view.apply {
                            subtitleView?.setViewType(androidx.media3.ui.SubtitleView.VIEW_TYPE_CANVAS)
                            val cleanStyle = androidx.media3.ui.CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                            subtitleView?.setStyle(cleanStyle)
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )



                // Universal Gestures Layer (Single tap toggles UI, Double tap Left = -5s, Center = Pause/Play, Right = +10s)
                val density = androidx.compose.ui.platform.LocalDensity.current
                val coroutineScope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    isControllerVisible = !isControllerVisible
                                },
                                onDoubleTap = { tapOffset: androidx.compose.ui.geometry.Offset ->
                                    val width = size.width.toFloat()
                                    val x = tapOffset.x
                                    val leftBoundary = width * 0.35f
                                    val rightBoundary = width * 0.65f

                                    when {
                                        x < leftBoundary -> {
                                            // Left side: rewind 5 seconds
                                            val current = exoPlayer.currentPosition
                                            val target = (current - 5000L).coerceAtLeast(0L)
                                            exoPlayer.seekTo(target)
                                            doubleTapActionSide = "left"
                                            doubleTapActionText = "-5s"
                                            gestureAnimTrigger = System.currentTimeMillis()
                                        }
                                        x > rightBoundary -> {
                                            // Right side: forward 10 seconds
                                            val current = exoPlayer.currentPosition
                                            val dur = exoPlayer.duration
                                            val target = if (dur > 0) (current + 10000L).coerceAtMost(dur) else current + 10000L
                                            exoPlayer.seekTo(target)
                                            doubleTapActionSide = "right"
                                            doubleTapActionText = "+10s"
                                            gestureAnimTrigger = System.currentTimeMillis()
                                        }
                                        else -> {
                                            // Center: toggle play / pause (stop video)
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                                viewModel.isPlaying = false
                                                doubleTapActionSide = "center"
                                                doubleTapActionText = "PAUSE"
                                            } else {
                                                exoPlayer.play()
                                                viewModel.isPlaying = true
                                                doubleTapActionSide = "center"
                                                doubleTapActionText = "PLAY"
                                            }
                                            gestureAnimTrigger = System.currentTimeMillis()
                                        }
                                    }
                                }
                            )
                        }
                )

                // Double-Tap Animated Feedback Badges
                LaunchedEffect(gestureAnimTrigger) {
                    if (gestureAnimTrigger > 0L) {
                        delay(750)
                        doubleTapActionSide = null
                        doubleTapActionText = null
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = doubleTapActionSide != null,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (doubleTapActionSide) {
                            "left" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.38f)
                                        .align(Alignment.CenterStart)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.Replay5,
                                            contentDescription = "Rewind 5s",
                                            tint = Color.White,
                                            modifier = Modifier.size(46.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = doubleTapActionText ?: "-5s",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            "right" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.38f)
                                        .align(Alignment.CenterEnd)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, Color.White.copy(alpha = 0.22f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(46.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = doubleTapActionText ?: "+10s",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            "center" -> {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            if (viewModel.isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = "Play/Pause Toggle",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = doubleTapActionText ?: "",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Compose Playback Controller Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isControllerVisible && !isInPip,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Demo centre affordance: a large play disc while paused.
                        if (!viewModel.isPlaying && !isInPip) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .clickable {
                                        exoPlayer.play()
                                        viewModel.isPlaying = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Top Bar: Back button on the left, Settings gear on the right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                                    )
                                )
                                .then(Modifier.padding(top = safeTopInset()))
                                .clickable(enabled = false) {} // block click-through
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    viewModel.playInfo = null
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        com.flixvora.ui.components.setSystemBarsVisible(activity, true)
                                    }
                                }) {
                                    Icon(
                                        Icons.Filled.ArrowBack,
                                        contentDescription = "Close Player",
                                        tint = Color.White
                                    )
                                }
                                // Demo header: title over a "year · quality" line.
                                Column {
                                    Text(
                                        text = subject.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 190.dp)
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            subject.year?.takeIf { it.isNotBlank() && it != "Not available" },
                                            viewModel.selectedStream?.quality?.takeIf { it.isNotBlank() }
                                                ?: if (subject.isTvShow) "1080p" else "4K"
                                        ).joinToString(" \u00b7 "),
                                        color = Color.White.copy(alpha = 0.62f),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            
                            // Top Right Icons: (fullscreen TV) episode pill, Download, Settings gear
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFullscreen && subject.isTvShow && viewModel.episodesList.isNotEmpty()) {
                                    FsEpisodeButton(
                                        current = viewModel.currentEpisode,
                                        season = viewModel.selectedSeason,
                                        onClick = { showFsEpisodeMenu = !showFsEpisodeMenu }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                IconButton(onClick = {
                                    val safeTitle = if (subject.isTvShow) {
                                        "${subject.name} S${viewModel.selectedSeason}E${viewModel.currentEpisode?.episodeNumber}"
                                    } else {
                                        subject.name
                                    }
                                    val bestStream = viewModel.playInfo?.streamList?.let { viewModel.selectHighestQualityStream(it) }
                                    val dlUrl = bestStream?.url?.takeIf { it.isNotEmpty() && !it.contains("b164fbfb4347792950bdfbfb563d39d9.mp4") } ?: streamUrl
                                    val cookies = bestStream?.cookie ?: ""
                                    viewModel.startDownload(context, subject, safeTitle, dlUrl, cookies)
                                }) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = "Download Video",
                                        tint = Color.White
                                    )
                                }
                                
                                IconButton(onClick = {
                                    activeSheet = "player_settings"
                                }) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Player Settings",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // v7 BOUNCE: fullscreen episode dropdown, anchored top-right
                        if (isFullscreen && subject.isTvShow && showFsEpisodeMenu) {
                            FsEpisodeMenu(
                                episodes = viewModel.episodesList,
                                currentEpisodeId = viewModel.currentEpisode?.episodeId,
                                season = viewModel.selectedSeason,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 62.dp, end = 16.dp),
                                onSelect = { ep ->
                                    showFsEpisodeMenu = false
                                    viewModel.selectEpisode(ep)
                                },
                                onDismiss = { showFsEpisodeMenu = false }
                            )
                        }

                        // Center Buffering Spinner (if loading)
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = CinematicRed,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                            )
                        }

                        // Bottom Controls Row: Play/Pause, Seekbar, Duration, PIP, Fullscreen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                                .then(Modifier.padding(bottom = safeBottomInset()))
                                .clickable(enabled = false) {} // block click-through
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause Button
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        viewModel.isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        viewModel.isPlaying = true
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Progress Slider
                            Slider(
                                value = currentProgressMs.toFloat(),
                                onValueChange = { currentProgressMs = it.toLong() },
                                onValueChangeFinished = { exoPlayer.seekTo(currentProgressMs) },
                                valueRange = 0f..maxOf(1f, durationMs.toFloat()),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            // Position / Duration text (e.g. 00:03/02:05)
                            Text(
                                text = "${formatTime(currentProgressMs)}/${formatTime(durationMs)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // PIP Button
                            IconButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            activity.enterPictureInPictureMode(
                                                android.app.PictureInPictureParams.Builder().build()
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PictureInPicture,
                                    contentDescription = "PiP Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Fullscreen Toggle Button
                            IconButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        if (!isFullscreen) {
                                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            com.flixvora.ui.components.setSystemBarsVisible(activity, false)
                                        } else {
                                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            com.flixvora.ui.components.setSystemBarsVisible(activity, true)
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                    }
                }

                // Auto-play next episode prompt (shown when an episode ends)
                val countdown = autoNextCountdown
                if (countdown != null && hasNextEpisode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Up Next",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val nextEp = run {
                                val idx = viewModel.episodesList.indexOfFirst { it.episodeId == viewModel.currentEpisode?.episodeId }
                                viewModel.episodesList.getOrNull(idx + 1)
                            }
                            Text(
                                nextEp?.title ?: "Next Episode",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        autoNextCountdown = null
                                        viewModel.playNextEpisode()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CinematicRed)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play Now ($countdown)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { autoNextCountdown = null },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }

                // Transparent click catcher overlay on player view when bottom sheet is active
                if (activeSheet != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { activeSheet = null }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CinematicRed)
                }
            }
        }

        // Portrait Details panel scroll container at bottom (only in portrait mode)
        if (!isFullscreen && !isInPip) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                    // Player Controller Panels (Subtitles, Dubs & Resolutions)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianDark)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (activeSheet != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (activeSheet) {
                                        "player_settings" -> "Playback Settings"
                                        "language" -> "Select language"
                                        "season" -> "${viewModel.seasonsList.size} seasons"
                                        "subtitle" -> "Select subtitle"
                                        "quality" -> "Select quality"
                                        "speed" -> "Playback speed"
                                        "episodes" -> "All episodes"
                                        else -> ""
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFFFFF)
                                )
                                IconButton(onClick = { activeSheet = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFFA9ABB3))
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.Gray.copy(alpha = 0.2f)
                            )
                            
                            if (activeSheet == "player_settings") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 0. Cast to TV
                                    var castButton by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.mediarouter.app.MediaRouteButton?>(null) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { castButton?.performClick() }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Cast to TV", color = Color.White, fontSize = 14.sp)
                                            AndroidView(
                                                factory = { ctx ->
                                                    val themeCtx = android.view.ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat)
                                                    androidx.mediarouter.app.MediaRouteButton(themeCtx).apply {
                                                        com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(themeCtx, this)
                                                        castButton = this
                                                        alpha = 0f
                                                    }
                                                },
                                                modifier = Modifier.size(1.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isCastConnected) "Connected (Playing on TV)" else "Not connected",
                                            color = if (isCastConnected) Color(0xFF1DD171) else CinematicRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 1. Audio Dub Language
                                    val currentDub = viewModel.selectedResource?.name ?: if (viewModel.isAniFam) "Sub" else "Original"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "language" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(if (viewModel.isAniFam) "Audio / Track" else "Audio Dub Language", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentDub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 2. Subtitles
                                    val currentSub = viewModel.selectedSubtitle?.language ?: "Off"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "subtitle" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Subtitles", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentSub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 3. Playback Speed
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "speed" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Playback Speed", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text("${selectedSpeed}x", color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 4. Video Quality
                                    val currentQuality = viewModel.selectedStream?.quality ?: "Auto"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "quality" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Video Quality", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentQuality, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 5. Episodes List
                                    if (subject.isTvShow) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { activeSheet = "episodes" }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text("Episodes List", color = Color.White, fontSize = 14.sp)
                                            }
                                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // 6. Sleep Timer
                                    val sleepLabel = when {
                                        sleepTimerMinutes <= 0 -> "Off"
                                        else -> "${(sleepTimerRemainingMs / 60000) + 1} min"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val options = listOf(0, 10, 20, 30, 45, 60)
                                                val next = options[(options.indexOf(sleepTimerMinutes).coerceAtLeast(0) + 1) % options.size]
                                                sleepTimerMinutes = next
                                                sleepTimerRemainingMs = next * 60_000L
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Sleep Timer", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(sleepLabel, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val itemsList = when (activeSheet) {
                                    "language" -> subject.resourceDetectors
                                    "season" -> viewModel.seasonsList
                                    "subtitle" -> listOf("None") + playInfo.subTitleList.map { it.language }
                                    "quality" -> playInfo.streamList
                                    "speed" -> listOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
                                    "episodes" -> viewModel.episodesList
                                    else -> emptyList()
                                }
                                
                                val cols = if (activeSheet == "episodes") 4 else 2
                                val chunked = itemsList.chunked(cols)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    chunked.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowItems.forEach { item ->
                                                val isSelected = when (activeSheet) {
                                                    "language" -> viewModel.selectedResource == item
                                                    "season" -> viewModel.selectedSeason == item
                                                    "subtitle" -> if (item == "None") viewModel.selectedSubtitle == null else viewModel.selectedSubtitle?.language == item
                                                    "quality" -> viewModel.selectedStream?.quality == (item as com.flixvora.data.model.StreamItem).quality
                                                    "speed" -> {
                                                        val currentSpeed = exoPlayer.playbackParameters.speed
                                                        val itemSpeed = when (item as String) {
                                                            "0.5x" -> 0.5f
                                                            "0.75x" -> 0.75f
                                                            "1.0x (Normal)" -> 1.0f
                                                            "1.25x" -> 1.25f
                                                            "1.5x" -> 1.5f
                                                            "2.0x" -> 2.0f
                                                            else -> 1.0f
                                                        }
                                                        Math.abs(currentSpeed - itemSpeed) < 0.05f
                                                    }
                                                    "episodes" -> viewModel.currentEpisode?.episodeId == (item as com.flixvora.data.model.Episode).episodeId
                                                    else -> false
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        when (activeSheet) {
                                                            "language" -> viewModel.selectLanguageResource(item as com.flixvora.data.model.ResourceDetector)
                                                            "season" -> viewModel.loadEpisodesForSeason(item as Int)
                                                            "subtitle" -> {
                                                                if (item == "None") {
                                                                    viewModel.selectedSubtitle = null
                                                                } else {
                                                                    val found = playInfo.subTitleList.find { it.language == item }
                                                                    if (found != null) viewModel.selectedSubtitle = found
                                                                }
                                                            }
                                                            "quality" -> {
                                                                val prevPos = exoPlayer.currentPosition
                                                                if (prevPos > 0) {
                                                                    viewModel.playbackProgressMs = prevPos
                                                                }
                                                                val streamItem = item as com.flixvora.data.model.StreamItem
                                                                viewModel.selectedStream = streamItem
                                                                viewModel.setDefaultStreamQualityPref(streamItem.quality)
                                                                android.widget.Toast.makeText(context, "Quality: ${streamItem.quality}", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                            "speed" -> {
                                                                val speedVal = when (item as String) {
                                                                    "0.5x" -> 0.5f
                                                                    "0.75x" -> 0.75f
                                                                    "1.0x (Normal)" -> 1.0f
                                                                    "1.25x" -> 1.25f
                                                                    "1.5x" -> 1.5f
                                                                    "2.0x" -> 2.0f
                                                                    else -> 1.0f
                                                                }
                                                                selectedSpeed = speedVal
                                                            }
                                                            "episodes" -> {
                                                                viewModel.selectEpisode(item as com.flixvora.data.model.Episode)
                                                            }
                                                        }
                                                        activeSheet = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) Color(0xFF115E59) else Color(0xFF28292E)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) {
                                                    Text(
                                                        text = when (activeSheet) {
                                                            "language" -> (item as com.flixvora.data.model.ResourceDetector).name
                                                            "season" -> "Season ${(item as Int).toString().padStart(2, '0')}"
                                                            "subtitle" -> item as String
                                                            "quality" -> (item as com.flixvora.data.model.StreamItem).quality
                                                            "speed" -> item as String
                                                            "episodes" -> (item as com.flixvora.data.model.Episode).episodeNumber.toString().padStart(2, '0')
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color(0xFF1DD171) else Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            if (rowItems.size < cols) {
                                                Spacer(modifier = Modifier.weight((cols - rowItems.size).toFloat()))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Title & meta header (detail screen is skipped on direct play)
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                            Text(
                                text = subject.name,
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!subject.rating.isNullOrBlank() && subject.rating != "Not available") {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(subject.rating, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                if (!subject.year.isNullOrBlank() && subject.year != "Not available") {
                                    Text(subject.year, color = TextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                if (subject.isTvShow && viewModel.episodesList.isNotEmpty()) {
                                    Text("${viewModel.episodesList.size} episodes", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            if (subject.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = subject.description,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Resource, Season and Inline Download Trigger
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            // Resource trigger button
                            if (subject.resourceDetectors.isNotEmpty()) {
                                Button(
                                    onClick = { activeSheet = "language" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28292E)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    val currentDub = viewModel.selectedResource?.name ?: if (viewModel.isAniFam) "Sub" else "Original Audio"
                                    Text(
                                        text = "$currentDub ▾",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Season trigger button (only for TV Shows, hidden for AniFam)
                            if (subject.isTvShow && viewModel.seasonsList.isNotEmpty() && !viewModel.isAniFam) {
                                Button(
                                    onClick = { activeSheet = "season" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28292E)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Season ${viewModel.selectedSeason.toString().padStart(2, '0')} ▾",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }


                            // Download Icon / Cancel button
                            if (subject.isTvShow && viewModel.episodesList.isNotEmpty()) {
                                if (isDownloadSelectionMode) {
                                    Button(
                                        onClick = {
                                            isDownloadSelectionMode = false
                                            selectedDownloadEpisodes.clear()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1B1B)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Cancel", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isDownloadSelectionMode = true
                                            selectedDownloadEpisodes.clear()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28292E)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = "Download multiple episodes",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quality Option Selector (Only visible in Download Selection Mode)
                        if (isDownloadSelectionMode && subject.isTvShow) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val activeSeasonInfo = subject.seasonList.find { it.season == viewModel.selectedSeason }
                                val seasonResolutions = activeSeasonInfo?.resolutions?.takeIf { it.isNotEmpty() }?.sorted()?.map { "${it}p" }
                                val downloadQualities = if (viewModel.isAniFam) listOf("360p", "720p", "1080p") else (seasonResolutions ?: listOf("480p", "720p", "1080p"))

                                androidx.compose.runtime.LaunchedEffect(downloadQualities) {
                                    if (downloadQualities.isNotEmpty() && !downloadQualities.any { it.equals(selectedDownloadQuality, ignoreCase = true) }) {
                                        selectedDownloadQuality = downloadQualities.last()
                                    }
                                }

                                downloadQualities.forEach { q ->
                                    val isSelected = selectedDownloadQuality.equals(q, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF115E59) else Color(0xFF26272C))
                                            .then(
                                                if (isSelected) Modifier.border(1.dp, Color(0xFF1DD171), RoundedCornerShape(8.dp)) else Modifier
                                            )
                                            .clickable {
                                                if (!selectedDownloadQuality.equals(q, ignoreCase = true)) {
                                                    selectedDownloadQuality = q
                                                    selectedDownloadEpisodes.clear()
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = q.uppercase(),
                                            color = if (isSelected) Color(0xFF1DD171) else Color(0xFFA9ABB3),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Episodes Section: Download Selection List OR Normal 4-Column Grid
                        if (subject.isTvShow) {
                            if (isDownloadSelectionMode) {
                                val activeSeasonInfo = subject.seasonList.find { it.season == viewModel.selectedSeason }
                                val currentResDigits = selectedDownloadQuality.filter { it.isDigit() }
                                val maxEpForQuality = activeSeasonInfo?.resolutionEpCounts?.get(currentResDigits) ?: activeSeasonInfo?.resolutionEpCounts?.get(selectedDownloadQuality.lowercase())
                                val availableEpisodesForQuality = androidx.compose.runtime.remember(viewModel.episodesList, selectedDownloadQuality, activeSeasonInfo) {
                                    if (maxEpForQuality != null && maxEpForQuality > 0) {
                                        viewModel.episodesList.filter { it.episodeNumber <= maxEpForQuality }
                                    } else {
                                        viewModel.episodesList
                                    }
                                }

                                // Multi-select download list — one horizontal line (scrolls sideways)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp) // generous space for sticky bottom bar + nav insets
                                ) {
                                    items(availableEpisodesForQuality, key = { it.episodeId }) { ep ->
                                        val isSelected = selectedDownloadEpisodes.any { it.episodeId == ep.episodeId }
                                        Card(
                                            modifier = Modifier
                                                .width(84.dp)
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedDownloadEpisodes.removeAll { it.episodeId == ep.episodeId }
                                                    } else {
                                                        selectedDownloadEpisodes.add(ep)
                                                    }
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFF271B1B) else Color(0xFF1A1B1F)
                                            ),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CinematicRed) else null
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "EP ${ep.episodeNumber.toString().padStart(2, '0')}",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) CinematicRed else Color.Transparent)
                                                        .then(
                                                            if (!isSelected) {
                                                                Modifier.border(2.dp, Color(0xFF6E7078), CircleShape)
                                                            } else {
                                                                Modifier
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "Selected",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Episodes horizontal row with an "All" button
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (viewModel.seasonsList.size > 1) "Episodes \u2022 Season ${viewModel.selectedSeason}" else "Episodes",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF28292E))
                                            .clickable { activeSheet = "episodes" }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.GridView, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                                ) {
                                    items(viewModel.episodesList, key = { it.episodeId }) { ep ->
                                        val isCurrentEp = viewModel.currentEpisode?.episodeId == ep.episodeId
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(112.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { viewModel.selectEpisode(ep) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF28292E))
                                            ) {
                                                if (ep.stillPath.isNotBlank()) {
                                                    AsyncImage(
                                                        model = ep.stillPath,
                                                        contentDescription = ep.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            if (isCurrentEp) CinematicRed.copy(alpha = 0.28f)
                                                            else Color.Black.copy(alpha = 0.15f)
                                                        )
                                                )
                                                Text(
                                                    text = ep.episodeNumber.toString().padStart(2, '0'),
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                                if (isCurrentEp) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .clip(CircleShape)
                                                            .background(CinematicRed)
                                                            .padding(3.dp)
                                                    ) {
                                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Now playing", tint = Color.White, modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = ep.title.ifBlank { "Episode ${ep.episodeNumber}" },
                                                color = if (isCurrentEp) Color(0xFF1DD171) else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrentEp) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Language selection row (audio tracks / dubs)
                        if (subject.resourceDetectors.isNotEmpty()) {
                            Text(
                                text = "Language",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                            ) {
                                items(subject.resourceDetectors, key = { it.resourceId }) { det ->
                                    val isSel = viewModel.selectedResource?.resourceId == det.resourceId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSel) CinematicRed else Color(0xFF28292E))
                                            .clickable(enabled = !isSel) { viewModel.selectLanguageResource(det) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = det.name.ifBlank { "Original" },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Subtitle selection row
                        val playerSubList = playInfo.subTitleList
                        if (playerSubList.isNotEmpty()) {
                            Text(
                                text = "Subtitles",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                            ) {
                                item {
                                    val isOff = viewModel.selectedSubtitle == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isOff) CinematicRed else Color(0xFF28292E))
                                            .clickable { viewModel.selectedSubtitle = null }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text("Off", color = Color.White, fontSize = 13.sp, fontWeight = if (isOff) FontWeight.Bold else FontWeight.Medium)
                                    }
                                }
                                itemsIndexed(playerSubList, key = { index, sub -> "${sub.language}_${sub.url}_$index" }) { _, sub ->
                                    val isSel = viewModel.selectedSubtitle?.url == sub.url
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSel) CinematicRed else Color(0xFF28292E))
                                            .clickable { viewModel.selectedSubtitle = sub }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(sub.language, color = Color.White, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Recommendations (discovery)
                        if (viewModel.detailRecommendations.isNotEmpty()) {
                            Text(
                                text = "More Like This",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                itemsIndexed(viewModel.detailRecommendations, key = { index, rec -> "${rec.id}_$index" }) { _, rec ->
                                    MovieGridItem(rec, modifier = Modifier.width(110.dp)) {
                                        viewModel.playDirect(rec)
                                    }
                                }
                            }
                        }
                    }
            }

            // Sticky Bottom Action Bar for Download All / Selected (Always visible on screen, no need to scroll to bottom!)
            if (activeSheet == null && isDownloadSelectionMode && subject.isTvShow) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = safeBottomInset())
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF28292E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Select All toggle
                        val activeSeasonInfoBottom = subject.seasonList.find { it.season == viewModel.selectedSeason }
                        val currentResDigitsBottom = selectedDownloadQuality.filter { it.isDigit() }
                        val maxEpForQualityBottom = activeSeasonInfoBottom?.resolutionEpCounts?.get(currentResDigitsBottom) ?: activeSeasonInfoBottom?.resolutionEpCounts?.get(selectedDownloadQuality.lowercase())
                        val availableEpisodesBottom = if (maxEpForQualityBottom != null && maxEpForQualityBottom > 0) {
                            viewModel.episodesList.filter { it.episodeNumber <= maxEpForQualityBottom }
                        } else {
                            viewModel.episodesList
                        }

                        val allSelected = selectedDownloadEpisodes.size == availableEpisodesBottom.size && availableEpisodesBottom.isNotEmpty()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (allSelected) {
                                        selectedDownloadEpisodes.clear()
                                    } else {
                                        selectedDownloadEpisodes.clear()
                                        selectedDownloadEpisodes.addAll(availableEpisodesBottom)
                                    }
                                }
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (allSelected) CinematicRed else Color.Transparent)
                                    .then(
                                        if (!allSelected) Modifier.border(2.dp, Color(0xFFA9ABB3), CircleShape) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (allSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "All Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select All",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Prominent Download Button
                        Button(
                            onClick = {
                                if (selectedDownloadEpisodes.isNotEmpty()) {
                                    val listToDownload = selectedDownloadEpisodes.toList()
                                    viewModel.downloadEpisodesBatch(context, listToDownload, targetQuality = selectedDownloadQuality)
                                    isDownloadSelectionMode = false
                                    selectedDownloadEpisodes.clear()
                                } else {
                                    Toast.makeText(context, "Please select at least 1 episode", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDownloadEpisodes.isNotEmpty()) CinematicRed else Color(0xFF3A3B42)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedDownloadEpisodes.isNotEmpty()) "Download (${selectedDownloadEpisodes.size})" else "Download",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

        } // Close screen-wrapping Column


        // Custom Slide-Up 2-Column Grid Bottom Sheet Overlay (Root Level)
        if (activeSheet != null && isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { activeSheet = null }
            ) {
                val isPlayerSheet = isFullscreen && (activeSheet == "subtitle" || activeSheet == "quality" || activeSheet == "speed" || activeSheet == "language" || activeSheet == "episodes" || activeSheet == "season" || activeSheet == "player_settings")
                Card(
                    modifier = Modifier
                        .align(if (isPlayerSheet) Alignment.Center else Alignment.BottomCenter)
                        .padding(if (isPlayerSheet) 24.dp else 0.dp)
                        .fillMaxWidth(if (isPlayerSheet) 0.6f else 1f)
                        .clickable(enabled = false) {}, // prevent click-through
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B1F)),
                    shape = if (isPlayerSheet) RoundedCornerShape(16.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeSheet) {
                                    "player_settings" -> "Playback Settings"
                                    "language" -> "Select language"
                                    "season" -> "${viewModel.seasonsList.size} seasons"
                                    "subtitle" -> "Select subtitle"
                                    "quality" -> "Select quality"
                                    "speed" -> "Playback speed"
                                    "episodes" -> "All episodes"
                                    else -> ""
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF)
                            )
                            IconButton(onClick = { activeSheet = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFFA9ABB3))
                            }
                        }
                        
                        if (activeSheet == "player_settings") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 0. Cast to TV
                                var castButton2 by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.mediarouter.app.MediaRouteButton?>(null) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { castButton2?.performClick() }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Cast to TV", color = Color.White, fontSize = 14.sp)
                                        AndroidView(
                                            factory = { ctx ->
                                                val themeCtx = android.view.ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat)
                                                androidx.mediarouter.app.MediaRouteButton(themeCtx).apply {
                                                    com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(themeCtx, this)
                                                    castButton2 = this
                                                    alpha = 0f
                                                }
                                            },
                                            modifier = Modifier.size(1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isCastConnected) "Connected (Playing on TV)" else "Not connected",
                                        color = if (isCastConnected) Color(0xFF1DD171) else CinematicRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 1. Audio Dub Language
                                val currentDub = viewModel.selectedResource?.name ?: if (viewModel.isAniFam) "Sub" else "Original"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "language" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(if (viewModel.isAniFam) "Audio / Track" else "Audio Dub Language", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentDub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 2. Subtitles
                                val currentSub = viewModel.selectedSubtitle?.language ?: "Off"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "subtitle" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Subtitles", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentSub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 3. Playback Speed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "speed" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Playback Speed", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text("${selectedSpeed}x", color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 4. Quality Resolution
                                val currentQuality = viewModel.selectedStream?.quality ?: "Auto"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "quality" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Video Quality", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentQuality, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 5. Episodes List (if TV Show)
                                if (subject.isTvShow) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "episodes" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Episodes List", color = Color.White, fontSize = 14.sp)
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }

                                // 6. Sleep Timer
                                val sleepLabelL = when {
                                    sleepTimerMinutes <= 0 -> "Off"
                                    else -> "${(sleepTimerRemainingMs / 60000) + 1} min"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val options = listOf(0, 10, 20, 30, 45, 60)
                                            val next = options[(options.indexOf(sleepTimerMinutes).coerceAtLeast(0) + 1) % options.size]
                                            sleepTimerMinutes = next
                                            sleepTimerRemainingMs = next * 60_000L
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Sleep Timer", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(sleepLabelL, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Scrollable Option Grid list (2-column or 4-column depending on sheet type)
                            val itemsList = when (activeSheet) {
                                        "language" -> subject.resourceDetectors
                                        "season" -> viewModel.seasonsList
                                        "subtitle" -> listOf("None") + playInfo.subTitleList.map { it.language }
                                        "quality" -> playInfo.streamList
                                        "speed" -> listOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
                                        "episodes" -> viewModel.episodesList
                                        else -> emptyList()
                                    }
                            
                            val cols = if (activeSheet == "episodes") 4 else 2
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val chunked = itemsList.chunked(cols)
                            items(chunked.size) { index ->
                                val rowItems = chunked[index]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        val isSelected = when (activeSheet) {
                                            "language" -> viewModel.selectedResource == item
                                            "season" -> viewModel.selectedSeason == item
                                            "subtitle" -> if (item == "None") viewModel.selectedSubtitle == null else viewModel.selectedSubtitle?.language == item
                                            "quality" -> viewModel.selectedStream?.quality == (item as com.flixvora.data.model.StreamItem).quality
                                            "speed" -> {
                                                val currentSpeed = exoPlayer.playbackParameters.speed
                                                val itemSpeed = when (item as String) {
                                                    "0.5x" -> 0.5f
                                                    "0.75x" -> 0.75f
                                                    "1.0x (Normal)" -> 1.0f
                                                    "1.25x" -> 1.25f
                                                    "1.5x" -> 1.5f
                                                    "2.0x" -> 2.0f
                                                    else -> 1.0f
                                                }
                                                Math.abs(currentSpeed - itemSpeed) < 0.05f
                                            }
                                            "episodes" -> viewModel.currentEpisode?.episodeId == (item as com.flixvora.data.model.Episode).episodeId
                                            else -> false
                                        }
                                        
                                        Button(
                                            onClick = {
                                                when (activeSheet) {
                                                    "language" -> viewModel.selectLanguageResource(item as com.flixvora.data.model.ResourceDetector)
                                                    "season" -> viewModel.loadEpisodesForSeason(item as Int)
                                                    "subtitle" -> {
                                                        if (item == "None") {
                                                            viewModel.selectedSubtitle = null
                                                        } else {
                                                            val found = playInfo.subTitleList.find { it.language == item }
                                                            if (found != null) viewModel.selectedSubtitle = found
                                                        }
                                                    }
                                                    "quality" -> {
                                                        val prevPos = exoPlayer.currentPosition
                                                        if (prevPos > 0) {
                                                            viewModel.playbackProgressMs = prevPos
                                                        }
                                                        val streamItem = item as com.flixvora.data.model.StreamItem
                                                        viewModel.selectedStream = streamItem
                                                        viewModel.setDefaultStreamQualityPref(streamItem.quality)
                                                        android.widget.Toast.makeText(context, "Quality: ${streamItem.quality}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    "speed" -> {
                                                        val speedVal = when (item as String) {
                                                            "0.5x" -> 0.5f
                                                            "0.75x" -> 0.75f
                                                            "1.0x (Normal)" -> 1.0f
                                                            "1.25x" -> 1.25f
                                                            "1.5x" -> 1.5f
                                                            "2.0x" -> 2.0f
                                                            else -> 1.0f
                                                        }
                                                        selectedSpeed = speedVal
                                                    }
                                                    "episodes" -> {
                                                        viewModel.selectEpisode(item as com.flixvora.data.model.Episode)
                                                    }
                                                }
                                                activeSheet = null
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF115E59) else Color(0xFF28292E)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(
                                                text = when (activeSheet) {
                                                    "language" -> (item as com.flixvora.data.model.ResourceDetector).name
                                                    "season" -> "Season ${(item as Int).toString().padStart(2, '0')}"
                                                    "subtitle" -> item as String
                                                    "quality" -> (item as com.flixvora.data.model.StreamItem).quality
                                                    "speed" -> item as String
                                                    "episodes" -> (item as com.flixvora.data.model.Episode).episodeNumber.toString().padStart(2, '0')
                                                    else -> ""
                                                },
                                                color = if (isSelected) Color(0xFF1DD171) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    // Handle odd elements padding
                                    if (rowItems.size < cols) {
                                        Spacer(modifier = Modifier.weight((cols - rowItems.size).toFloat()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Human readable time formatter helper (mm:ss)
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

data class VideoPlayerOption(
    val name: String,
    val packageName: String,
    val activityName: String?,
    val icon: android.graphics.drawable.Drawable?
)

fun getInstalledVideoPlayers(context: android.content.Context): List<VideoPlayerOption> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(android.net.Uri.parse("content://dummy/video.mp4"), "video/*")
    }
    val resolveInfos = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        }
    } catch (e: Exception) {
        emptyList()
    }
    
    return resolveInfos
        .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map {
            VideoPlayerOption(
                name = it.loadLabel(pm)?.toString() ?: it.activityInfo.packageName,
                packageName = it.activityInfo.packageName,
                activityName = it.activityInfo.name,
                icon = try { it.loadIcon(pm) } catch (e: Exception) { null }
            )
        }
}

fun openInExternalPlayer(context: android.content.Context, file: java.io.File, option: VideoPlayerOption?) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (option != null && option.activityName != null) {
                setClassName(option.packageName, option.activityName)
            }
        }
        if (option != null) {
            context.startActivity(intent)
        } else {
            val chooser = android.content.Intent.createChooser(intent, "Play with...")
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Unable to play: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun shareVideoFile(context: android.content.Context, file: java.io.File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Video")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not share video: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun saveVideoToCameraRoll(context: android.content.Context, file: java.io.File) {
    if (!file.exists()) {
        android.widget.Toast.makeText(context, "File does not exist", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    val appContext = context.applicationContext
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val displayName = file.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
                }

                val collection = android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = contentResolver.insert(collection, values) ?: throw Exception("Could not create MediaStore entry in DCIM/Camera")

                contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    file.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                values.clear()
                values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, values, null, null)
            } else {
                val dcimDir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM), "Camera")
                if (!dcimDir.exists()) dcimDir.mkdirs()
                val targetFile = java.io.File(dcimDir, displayName)
                file.copyTo(targetFile, overwrite = true)
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Saved!", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Failed to save: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun DownloadItemOptionsMenu(
    file: java.io.File,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(iconSize)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color(0xFF28292E),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            DropdownMenuItem(
                text = { Text("Save", color = Color.White, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    saveVideoToCameraRoll(context, file)
                }
            )

            DropdownMenuItem(
                text = { Text("Share", color = Color.White, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    shareVideoFile(context, file)
                }
            )

            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFEF4444), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

fun toggleDownloadPauseResume(
    context: android.content.Context,
    active: com.flixvora.ui.viewmodel.ActiveDownload,
    isPaused: Boolean
) {
    val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager ?: return
    var succeeded = false

    if (isPaused) {
        // --- RESUME ---
        // 1. Try AOSP hidden method: resumeDownload(long... ids)
        try {
            val method = manager.javaClass.getMethod("resumeDownload", LongArray::class.java)
            method.invoke(manager, longArrayOf(active.id))
            succeeded = true
        } catch (_: Throwable) {
            // 2. Try single-primitive method: resumeDownload(long id)
            try {
                val method = manager.javaClass.getMethod("resumeDownload", Long::class.javaPrimitiveType)
                method.invoke(manager, active.id)
                succeeded = true
            } catch (_: Throwable) {}
        }

        // 3. Try direct ContentResolver update (Downloads.Impl CONTROL_RUN = 0)
        if (!succeeded) {
            try {
                val baseUri = android.net.Uri.parse("content://downloads/my_downloads")
                val values = android.content.ContentValues().apply {
                    put("control", 0) // CONTROL_RUN
                    put("status", 190) // STATUS_PENDING
                }
                val updated = context.contentResolver.update(
                    baseUri,
                    values,
                    "_id = ?",
                    arrayOf(active.id.toString())
                )
                if (updated > 0) succeeded = true
            } catch (_: Throwable) {}
        }

        // 4. Try restartDownload(long... ids)
        if (!succeeded) {
            try {
                val method = manager.javaClass.getMethod("restartDownload", LongArray::class.java)
                method.invoke(manager, longArrayOf(active.id))
                succeeded = true
            } catch (_: Throwable) {}
        }

        // 5. Fallback for vendor-locked ROMs (Realme / ColorOS): Re-enqueue download if stalled/paused
        if (!succeeded && active.url.isNotBlank()) {
            try {
                manager.remove(active.id)
                downloadVideo(context, active.url, active.title, showToast = false)
                succeeded = true
            } catch (_: Throwable) {}
        }

        if (succeeded) {
            android.widget.Toast.makeText(context, "Resuming download...", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Could not resume download on this device", android.widget.Toast.LENGTH_SHORT).show()
        }
    } else {
        // --- PAUSE ---
        // 1. Try AOSP hidden method: pauseDownload(long... ids)
        try {
            val method = manager.javaClass.getMethod("pauseDownload", LongArray::class.java)
            method.invoke(manager, longArrayOf(active.id))
            succeeded = true
        } catch (_: Throwable) {
            // 2. Try single-primitive method: pauseDownload(long id)
            try {
                val method = manager.javaClass.getMethod("pauseDownload", Long::class.javaPrimitiveType)
                method.invoke(manager, active.id)
                succeeded = true
            } catch (_: Throwable) {}
        }

        // 3. Try direct ContentResolver update (Downloads.Impl CONTROL_PAUSED = 1)
        if (!succeeded) {
            try {
                val baseUri = android.net.Uri.parse("content://downloads/my_downloads")
                val values = android.content.ContentValues().apply {
                    put("control", 1) // CONTROL_PAUSED
                    put("status", 193) // STATUS_PAUSED_BY_APP
                }
                val updated = context.contentResolver.update(
                    baseUri,
                    values,
                    "_id = ?",
                    arrayOf(active.id.toString())
                )
                if (updated > 0) succeeded = true
            } catch (_: Throwable) {}
        }

        if (succeeded) {
            android.widget.Toast.makeText(context, "Download paused", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Pause is not supported on this device", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(viewModel: MovieViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasPermission by remember { 
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R || 
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) 
    }

    val downloadedFiles = viewModel.downloadedFiles
    val activeDownloads = viewModel.activeDownloads
    val isScanLoaded = viewModel.isDownloadScanLoaded
    var expandedShows by remember { mutableStateOf(setOf<String>()) }
    var playerPickerFile by remember { mutableStateOf<java.io.File?>(null) }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val activity = context as? android.app.Activity
            if (activity != null && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                androidx.core.app.ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        }
        
        // Background polling loop on Dispatchers.IO - keeps UI 100% fluid with zero lag
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            while (isActive) {
                val currentActive = mutableListOf<ActiveDownload>()
                if (manager != null) {
                    try {
                        val query = android.app.DownloadManager.Query().setFilterByStatus(
                            android.app.DownloadManager.STATUS_RUNNING or 
                            android.app.DownloadManager.STATUS_PENDING or 
                            android.app.DownloadManager.STATUS_PAUSED
                        )
                        manager.query(query)?.use { cursor ->
                            val idIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_ID)
                            val titleIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TITLE)
                            val soFarIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                            val reasonIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON)
                            val uriIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_URI)
                            while (cursor.moveToNext()) {
                                val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                                val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Downloading..." else "Downloading..."
                                val bytesDownloaded = if (soFarIdx >= 0) cursor.getLong(soFarIdx) else 0L
                                val bytesTotal = if (totalIdx >= 0) cursor.getLong(totalIdx) else 0L
                                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else android.app.DownloadManager.STATUS_RUNNING
                                val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else 0
                                val uri = if (uriIdx >= 0) cursor.getString(uriIdx) ?: "" else ""
                                val progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                                currentActive.add(ActiveDownload(id, title, progress, bytesDownloaded, bytesTotal, status, reason, uri))
                            }
                        }
                    } catch (e: Exception) { }
                }

                val candidateDirs = listOfNotNull(
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Flixvora"),
                    context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.let { java.io.File(it, "Flixvora") },
                    java.io.File(context.filesDir, "Flixvora"),
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Genga World")
                )
                val scanned = candidateDirs
                    .filter { it.exists() && it.isDirectory }
                    .flatMap { it.listFiles()?.toList() ?: emptyList() }
                    .filter { it.extension.equals("mp4", ignoreCase = true) }
                    .distinctBy { it.name }
                    .sortedBy { it.name }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val combined = (viewModel.inAppDownloads + viewModel.queuedDownloads + currentActive)
                        .distinctBy { it.id }
                        .sortedBy { it.id }
                    viewModel.activeDownloads = combined
                    if (viewModel.downloadedFiles.size != scanned.size || viewModel.downloadedFiles.map { it.name } != scanned.map { it.name }) {
                        viewModel.downloadedFiles = scanned
                    }
                    viewModel.isDownloadScanLoaded = true
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (!isScanLoaded && downloadedFiles.isEmpty() && activeDownloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinematicRed, modifier = Modifier.size(36.dp))
            }
        } else if (downloadedFiles.isEmpty() && activeDownloads.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Downloads",
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hasPermission) "No Downloads Yet" else "Storage Permission Required",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Downloaded media will appear here.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Demo header: plain title, a count line, then a storage meter.
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = safeTopInset() + 12.dp)) {
                    Text(
                        text = "Downloads",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${activeDownloads.size} downloading \u00b7 ${downloadedFiles.size} complete",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    MbStorageMeter(
                        context = context,
                        revision = downloadedFiles.size + activeDownloads.size
                    )
                    Spacer(Modifier.height(6.dp))
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDownloads.isNotEmpty()) {
                        item {
                            Text("Downloading...", color = CinematicRed, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        items(activeDownloads) { active ->
                            val flixvoraDir = remember {
                                val base = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                                val primary = java.io.File(base, "Flixvora")
                                if (primary.exists()) primary else java.io.File(base, "Genga World")
                            }
                            val safeActiveTitle = remember(active.title) { active.title.replace(Regex("[^a-zA-Z0-9.-]"), "_") }
                            val safeShowPrefix = remember(active.title) {
                                val match = Regex("(.*?)(_S\\d+E\\d+.*|$)").find(safeActiveTitle)
                                match?.groupValues?.get(1)?.trim() ?: safeActiveTitle
                            }
                            val activePosterFile = remember(active.title) {
                                val searchDirs = mutableListOf<java.io.File>()
                                searchDirs.add(flixvoraDir)
                                context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.let { searchDirs.add(java.io.File(it, "Flixvora")) }
                                searchDirs.add(java.io.File(context.filesDir, "Flixvora"))

                                val extensions = listOf("jpg", "webp", "png", "jpeg")
                                for (d in searchDirs.distinct()) {
                                    for (ext in extensions) {
                                        val tFile = java.io.File(d, "$safeActiveTitle.$ext")
                                        if (tFile.exists() && tFile.length() > 0) return@remember tFile
                                        val sFile = java.io.File(d, "$safeShowPrefix.$ext")
                                        if (sFile.exists() && sFile.length() > 0) return@remember sFile
                                    }
                                }
                                null
                            }

                            val posterModel: Any? = if (activePosterFile != null && activePosterFile.exists()) {
                                activePosterFile
                            } else if (active.posterUrl.isNotEmpty()) {
                                active.posterUrl
                            } else null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(com.flixvora.ui.theme.ObsidianCard, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (posterModel != null) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(posterModel)
                                            .crossfade(100)
                                            .build(),
                                        contentDescription = "Poster",
                                        modifier = Modifier.size(56.dp, 42.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp, 42.dp)
                                            .background(ObsidianDark, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = "Downloading", tint = CinematicRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = active.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { if (active.progress > 0) active.progress / 100f else 0f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = CinematicRed,
                                        trackColor = ObsidianDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val statusText = when (active.status) {
                                        android.app.DownloadManager.STATUS_PAUSED -> {
                                            if (active.reason == android.app.DownloadManager.PAUSED_WAITING_FOR_NETWORK ||
                                                active.reason == android.app.DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                                                "Waiting for network..."
                                            } else {
                                                "Paused"
                                            }
                                        }
                                        android.app.DownloadManager.STATUS_PENDING -> "Queued — waiting for its turn"
                                        else -> "Downloading..."
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${active.progress}% (${(active.downloadedBytes / (1024 * 1024))}MB / ${if (active.totalBytes > 0) (active.totalBytes / (1024 * 1024)).toString() + "MB" else "Unknown"}) • $statusText",
                                            color = if (active.status == android.app.DownloadManager.STATUS_PAUSED) Color(0xFF1DD171) else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Pause / Resume & Cancel Control Buttons
                                val isPaused = active.status == android.app.DownloadManager.STATUS_PAUSED
                                val manager = remember { context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager }

                                if (active.status != android.app.DownloadManager.STATUS_PENDING) {
                                    IconButton(
                                        onClick = {
                                            val isInApp = viewModel.inAppDownloads.any { it.id == active.id } ||
                                                viewModel.queuedDownloads.any { it.id == active.id }
                                            if (isInApp) {
                                                if (isPaused) viewModel.resumeInAppDownload(active.id)
                                                else viewModel.pauseInAppDownload(active.id)
                                            } else {
                                                toggleDownloadPauseResume(context, active, isPaused)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = if (isPaused) "Resume" else "Pause",
                                            tint = if (isPaused) Color(0xFF1DD171) else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.cancelInAppDownload(active.id)
                                        manager?.remove(active.id)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cancel Download",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (downloadedFiles.isNotEmpty()) {
                        item {
                            Text("Completed", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                        }
                        val groupedFiles = downloadedFiles.groupBy { file ->
                            val name = file.nameWithoutExtension
                            val match = Regex("(.*?)(_S\\d+E\\d+.*|$)").find(name)
                            match?.groupValues?.get(1)?.replace("_", " ")?.trim() ?: name
                        }

                        items(groupedFiles.keys.toList()) { showName ->
                            val filesForShow = groupedFiles[showName] ?: emptyList()
                            val isExpanded = expandedShows.contains(showName)
                            val flixvoraDir = remember {
                                val base = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                                val primary = java.io.File(base, "Flixvora")
                                if (primary.exists()) primary else java.io.File(base, "Genga World")
                            }
                            val safeShowPrefix = remember(showName) { showName.replace(Regex("[^a-zA-Z0-9.-]"), "_") }

                            val posterFile = remember(showName, filesForShow) {
                                val searchDirs = mutableListOf<java.io.File>()
                                filesForShow.firstOrNull()?.parentFile?.let { searchDirs.add(it) }
                                searchDirs.add(flixvoraDir)
                                context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.let { searchDirs.add(java.io.File(it, "Flixvora")) }
                                searchDirs.add(java.io.File(context.filesDir, "Flixvora"))

                                val extensions = listOf("jpg", "webp", "png", "jpeg")

                                // 1. Try show-level poster across all candidate folders
                                for (d in searchDirs.distinct()) {
                                    for (ext in extensions) {
                                        val p = java.io.File(d, "$safeShowPrefix.$ext")
                                        if (p.exists() && p.length() > 0) return@remember p
                                    }
                                }

                                // 2. Try episode-level posters for each episode in filesForShow
                                for (f in filesForShow) {
                                    val parent = f.parentFile
                                    for (ext in extensions) {
                                        if (parent != null) {
                                            val p = java.io.File(parent, "${f.nameWithoutExtension}.$ext")
                                            if (p.exists() && p.length() > 0) return@remember p
                                        }
                                        for (d in searchDirs.distinct()) {
                                            val p = java.io.File(d, "${f.nameWithoutExtension}.$ext")
                                            if (p.exists() && p.length() > 0) return@remember p
                                        }
                                    }
                                }
                                null
                            }
                            
                            Column(modifier = Modifier.fillMaxWidth().background(com.flixvora.ui.theme.ObsidianCard, RoundedCornerShape(10.dp))) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (filesForShow.size == 1) {
                                                playerPickerFile = filesForShow.first()
                                            } else {
                                                expandedShows = if (isExpanded) expandedShows - showName else expandedShows + showName
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (posterFile != null && posterFile.exists()) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(posterFile)
                                                .crossfade(100)
                                                .memoryCacheKey(posterFile.absolutePath)
                                                .build(),
                                            contentDescription = "Poster",
                                            modifier = Modifier.size(56.dp, 84.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(56.dp, 84.dp).background(ObsidianDark, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CinematicRed, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = showName,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (filesForShow.size > 1) "${filesForShow.size} Episodes" else "${(filesForShow.firstOrNull()?.length() ?: 0L) / (1024 * 1024)} MB",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (filesForShow.size > 1) {
                                        var showMenuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showMenuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "Options",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenuExpanded,
                                                onDismissRequest = { showMenuExpanded = false },
                                                shape = RoundedCornerShape(12.dp),
                                                containerColor = Color(0xFF28292E),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 8.dp
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Save All (${filesForShow.size})", color = Color.White, fontSize = 14.sp) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Filled.Save,
                                                            contentDescription = "Save All",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        showMenuExpanded = false
                                                        filesForShow.forEach { f ->
                                                            saveVideoToCameraRoll(context, f)
                                                        }
                                                    }
                                                )

                                                DropdownMenuItem(
                                                    text = { Text("Delete All (${filesForShow.size})", color = Color(0xFFEF4444), fontSize = 14.sp) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = "Delete All",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        showMenuExpanded = false
                                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                            filesForShow.forEach { f -> f.delete() }
                                                            val showPoster = java.io.File(flixvoraDir, "$safeShowPrefix.jpg")
                                                            if (showPoster.exists()) showPoster.delete()
                                                            flixvoraDir.listFiles()?.filter { it.extension.equals("jpg", ignoreCase = true) && it.nameWithoutExtension.startsWith(safeShowPrefix) }?.forEach { it.delete() }
                                                            viewModel.scanDownloadedFiles()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = TextSecondary,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    } else if (filesForShow.isNotEmpty()) {
                                        val singleFile = filesForShow.first()
                                        DownloadItemOptionsMenu(
                                            file = singleFile,
                                            iconSize = 20.dp,
                                            onDelete = {
                                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                    val singlePoster = java.io.File(flixvoraDir, "${singleFile.nameWithoutExtension}.jpg")
                                                    if (singlePoster.exists()) singlePoster.delete()
                                                    val showPoster = java.io.File(flixvoraDir, "$safeShowPrefix.jpg")
                                                    if (showPoster.exists()) showPoster.delete()
                                                    flixvoraDir.listFiles()?.filter { it.extension.equals("jpg", ignoreCase = true) && it.nameWithoutExtension.startsWith(safeShowPrefix) }?.forEach { it.delete() }
                                                    singleFile.delete()
                                                    viewModel.scanDownloadedFiles()
                                                }
                                            }
                                        )
                                    }
                                }
                                
                                if (isExpanded && filesForShow.size > 1) {
                                    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                                        filesForShow.forEach { file ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .background(ObsidianDark, RoundedCornerShape(6.dp))
                                                    .clickable { playerPickerFile = file }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CinematicRed, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val epMatch = Regex("_S(\\d+)E(\\d+)").find(file.nameWithoutExtension)
                                                    val epName = if (epMatch != null) "Season ${epMatch.groupValues[1]} Episode ${epMatch.groupValues[2]}" else file.nameWithoutExtension
                                                    Text(text = epName, color = TextPrimary, fontSize = 13.sp)
                                                    Text(text = "${(file.length() / (1024 * 1024))} MB", color = TextSecondary, fontSize = 11.sp)
                                                }
                                                DownloadItemOptionsMenu(
                                                    file = file,
                                                    iconSize = 18.dp,
                                                    onDelete = {
                                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                            if (filesForShow.size <= 1) {
                                                                // Last episode remaining: clean up posters
                                                                val epPoster = java.io.File(flixvoraDir, "${file.nameWithoutExtension}.jpg")
                                                                if (epPoster.exists()) epPoster.delete()
                                                                val showPoster = java.io.File(flixvoraDir, "$safeShowPrefix.jpg")
                                                                if (showPoster.exists()) showPoster.delete()
                                                                flixvoraDir.listFiles()?.filter { it.extension.equals("jpg", ignoreCase = true) && it.nameWithoutExtension.startsWith(safeShowPrefix) }?.forEach { it.delete() }
                                                            }
                                                            // When multiple episodes exist, ONLY delete the video file, preserving posters for the remaining episodes
                                                            file.delete()
                                                            viewModel.scanDownloadedFiles()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        val pickedFile = playerPickerFile
        if (pickedFile != null) {
            val fileToPlay = pickedFile
            val externalPlayers = remember(context) { getInstalledVideoPlayers(context) }
            val appIcon = remember(context) {
                try { context.packageManager.getApplicationIcon(context.packageName) } catch (e: Exception) { null }
            }

            ModalBottomSheet(
                onDismissRequest = { playerPickerFile = null },
                containerColor = com.flixvora.ui.theme.ObsidianCard,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Play With",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val epMatch = Regex("_S(\\d+)E(\\d+)").find(fileToPlay.nameWithoutExtension)
                    val displayName = if (epMatch != null) {
                        val showPrefix = fileToPlay.nameWithoutExtension.substringBefore("_S").replace("_", " ")
                        "$showPrefix • Season ${epMatch.groupValues[1]} Episode ${epMatch.groupValues[2]}"
                    } else {
                        fileToPlay.nameWithoutExtension.replace("_", " ")
                    }
                    Text(
                        text = displayName,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    // 1. Flixvora Internal Player
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDark)
                            .clickable {
                                viewModel.playingDownloadedFile = fileToPlay
                                playerPickerFile = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appIcon != null) {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = "Flixvora",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(CinematicRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CinematicRed, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Flixvora Player",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "IN-APP",
                                    color = CinematicRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .background(CinematicRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Built-in cinema player with controls",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = CinematicRed)
                    }

                    if (externalPlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Other Installed Players",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        externalPlayers.forEach { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianDark)
                                    .clickable {
                                        playerPickerFile = null
                                        openInExternalPlayer(context, fileToPlay, player)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (player.icon != null) {
                                    AsyncImage(
                                        model = player.icon,
                                        contentDescription = player.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(com.flixvora.ui.theme.ObsidianCard, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "External player",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // System Chooser Option
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianDark)
                            .clickable {
                                playerPickerFile = null
                                openInExternalPlayer(context, fileToPlay, null)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(com.flixvora.ui.theme.ObsidianCard, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "More", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Dialog / More Apps...",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Choose from standard Android popup",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        
    }
}

@Composable
fun OfflinePlayerScreen(fileToPlay: java.io.File, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val epMatch = Regex("_S(\\d+)E(\\d+)").find(fileToPlay.nameWithoutExtension)
    val displayTitle = if (epMatch != null) {
        val showPrefix = fileToPlay.nameWithoutExtension.substringBefore("_S").replace("_", " ")
        "$showPrefix - S${epMatch.groupValues[1]} E${epMatch.groupValues[2]}"
    } else {
        fileToPlay.nameWithoutExtension.replace("_", " ")
    }

    var isOfflineControllerVisible by remember { mutableStateOf(true) }
    var isOfflinePlaying by remember { mutableStateOf(true) }
    var currentOfflineProgressMs by remember { mutableStateOf(0L) }
    var offlineDurationMs by remember { mutableStateOf(0L) }
    var isOfflineBuffering by remember { mutableStateOf(false) }

    // Double-Tap Gesture Indicator States
    var offlineDoubleTapSide by remember { mutableStateOf<String?>(null) }
    var offlineDoubleTapText by remember { mutableStateOf<String?>(null) }
    var offlineGestureTrigger by remember { mutableStateOf(0L) }

    val offlineConfiguration = androidx.compose.ui.platform.LocalConfiguration.current
    val isOfflineFullscreen = offlineConfiguration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val offlineActivity = context as? android.app.Activity
    val isOfflineInPip = offlineActivity?.isInPictureInPictureMode ?: false

    BackHandler {
        if (isOfflineFullscreen) {
            offlineActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            offlineActivity?.let { com.flixvora.ui.components.setSystemBarsVisible(it, true) }
        } else {
            offlineActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            offlineActivity?.let { com.flixvora.ui.components.setSystemBarsVisible(it, true) }
            onDismiss()
        }
    }

    val exoPlayer = remember(fileToPlay) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(fileToPlay)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(fileToPlay) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isOfflineBuffering = (state == androidx.media3.common.Player.STATE_BUFFERING)
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isOfflinePlaying = playWhenReady
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            offlineActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            offlineActivity?.let { com.flixvora.ui.components.setSystemBarsVisible(it, true) }
        }
    }

    // Keep system bars hidden when in landscape fullscreen
    LaunchedEffect(isOfflineFullscreen) {
        if (offlineActivity != null) {
            if (isOfflineFullscreen) {
                com.flixvora.ui.components.setSystemBarsVisible(offlineActivity, false)
            } else {
                com.flixvora.ui.components.setSystemBarsVisible(offlineActivity, true)
            }
        }
    }

    // Progress ticker
    LaunchedEffect(isOfflinePlaying) {
        if (isOfflinePlaying) {
            while (true) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0) {
                    currentOfflineProgressMs = pos
                    offlineDurationMs = dur
                }
                delay(1000)
            }
        }
    }

    // Auto-hide controller
    LaunchedEffect(isOfflineControllerVisible, isOfflinePlaying) {
        if (isOfflineControllerVisible && isOfflinePlaying) {
            delay(4000)
            isOfflineControllerVisible = false
        }
    }

    // Reset gesture animation
    LaunchedEffect(offlineGestureTrigger) {
        if (offlineGestureTrigger > 0L) {
            delay(750)
            offlineDoubleTapSide = null
            offlineDoubleTapText = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx).inflate(com.flixvora.R.layout.texture_player_view, null) as androidx.media3.ui.PlayerView
                view.keepScreenOn = true
                view.isHapticFeedbackEnabled = false
                view.isSoundEffectsEnabled = false
                view.useController = false
                view.apply {
                    subtitleView?.setViewType(androidx.media3.ui.SubtitleView.VIEW_TYPE_CANVAS)
                    val cleanStyle = androidx.media3.ui.CaptionStyleCompat(
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        android.graphics.Color.BLACK,
                        null
                    )
                    subtitleView?.setStyle(cleanStyle)
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Universal Gestures Layer (Single tap toggles UI, Double tap Left = -5s, Center = Pause/Play, Right = +10s)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            isOfflineControllerVisible = !isOfflineControllerVisible
                        },
                        onDoubleTap = { tapOffset: androidx.compose.ui.geometry.Offset ->
                            val width = size.width.toFloat()
                            val x = tapOffset.x
                            val leftBoundary = width * 0.35f
                            val rightBoundary = width * 0.65f

                            when {
                                x < leftBoundary -> {
                                    // Left: rewind 5s
                                    val current = exoPlayer.currentPosition
                                    val target = (current - 5000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    offlineDoubleTapSide = "left"
                                    offlineDoubleTapText = "-5s"
                                    offlineGestureTrigger = System.currentTimeMillis()
                                }
                                x > rightBoundary -> {
                                    // Right: forward 10s
                                    val current = exoPlayer.currentPosition
                                    val dur = exoPlayer.duration
                                    val target = if (dur > 0) (current + 10000L).coerceAtMost(dur) else current + 10000L
                                    exoPlayer.seekTo(target)
                                    offlineDoubleTapSide = "right"
                                    offlineDoubleTapText = "+10s"
                                    offlineGestureTrigger = System.currentTimeMillis()
                                }
                                else -> {
                                    // Center: toggle play / pause
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        isOfflinePlaying = false
                                        offlineDoubleTapSide = "center"
                                        offlineDoubleTapText = "PAUSE"
                                    } else {
                                        exoPlayer.play()
                                        isOfflinePlaying = true
                                        offlineDoubleTapSide = "center"
                                        offlineDoubleTapText = "PLAY"
                                    }
                                    offlineGestureTrigger = System.currentTimeMillis()
                                }
                            }
                        }
                    )
                }
        )

        // Double-Tap Animated Feedback Badges
        androidx.compose.animation.AnimatedVisibility(
            visible = offlineDoubleTapSide != null,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (offlineDoubleTapSide) {
                    "left" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.38f)
                                .align(Alignment.CenterStart)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Replay5, contentDescription = "Rewind 5s", tint = Color.White, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = offlineDoubleTapText ?: "-5s", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "right" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.38f)
                                .align(Alignment.CenterEnd)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.22f))
                                )
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(46.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = offlineDoubleTapText ?: "+10s", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "center" -> {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (isOfflinePlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = offlineDoubleTapText ?: "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Playback Controller Overlay (matching normal player without settings & download)
        androidx.compose.animation.AnimatedVisibility(
            visible = isOfflineControllerVisible && !isOfflineInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar: Back button on the left, Title in center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .then(Modifier.padding(top = safeTopInset()))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        offlineActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        offlineActivity?.let { com.flixvora.ui.components.setSystemBarsVisible(it, true) }
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close Player", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center Buffering Spinner
                if (isOfflineBuffering) {
                    CircularProgressIndicator(
                        color = CinematicRed,
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }

                // Bottom Controls Row: Play/Pause, Seekbar, Duration, PIP, Fullscreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                        .then(Modifier.padding(bottom = safeBottomInset()))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Button
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                isOfflinePlaying = false
                            } else {
                                exoPlayer.play()
                                isOfflinePlaying = true
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isOfflinePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Progress Slider
                    Slider(
                        value = currentOfflineProgressMs.toFloat(),
                        onValueChange = { currentOfflineProgressMs = it.toLong() },
                        onValueChangeFinished = { exoPlayer.seekTo(currentOfflineProgressMs) },
                        valueRange = 0f..maxOf(1f, offlineDurationMs.toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Position / Duration text
                    Text(
                        text = "${formatTime(currentOfflineProgressMs)}/${formatTime(offlineDurationMs)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // PIP Button
                    IconButton(
                        onClick = {
                            if (offlineActivity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                offlineActivity.enterPictureInPictureMode(
                                    android.app.PictureInPictureParams.Builder().build()
                                )
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.PictureInPicture,
                            contentDescription = "PiP Mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Fullscreen Toggle Button
                    IconButton(
                        onClick = {
                            if (offlineActivity != null) {
                                if (!isOfflineFullscreen) {
                                    offlineActivity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    com.flixvora.ui.components.setSystemBarsVisible(offlineActivity, false)
                                } else {
                                    offlineActivity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    com.flixvora.ui.components.setSystemBarsVisible(offlineActivity, true)
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isOfflineFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(viewModel: MovieViewModel) {
    SearchContent(viewModel = viewModel)
}

/**
 * v7 BOUNCE fullscreen episode pill: shows the current episode and opens the
 * picker. Replaces the old always-on side panel so the video stays unobstructed.
 */
@Composable
fun FsEpisodeButton(
    current: com.flixvora.data.model.Episode?,
    season: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF101114).copy(alpha = 0.60f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(AccentPrimary, CircleShape)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = current?.let { "E${it.episodeNumber} · ${it.title}" } ?: "Episodes",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            Text(
                text = current?.let { "S$season · Episode ${it.episodeNumber}" } ?: "Season $season",
                color = TextTertiary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = "Open episodes",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * v7 BOUNCE fullscreen episode dropdown: a compact, scrim-backed list anchored
 * under the episode pill. Tapping an episode switches the stream.
 */
@Composable
fun FsEpisodeMenu(
    episodes: List<com.flixvora.data.model.Episode>,
    currentEpisodeId: String?,
    season: Int,
    modifier: Modifier = Modifier,
    onSelect: (com.flixvora.data.model.Episode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = modifier
            .width(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF161724), Color(0xFF101114)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "ALL EPISODES · S$season",
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 9.dp)
        )
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            items(episodes, key = { it.episodeId }) { ep ->
                val isNow = ep.episodeId == currentEpisodeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isNow) AccentPrimary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.03f)
                        )
                        .border(
                            1.dp,
                            if (isNow) AccentPrimary.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(13.dp)
                        )
                        .clickable { onSelect(ep) }
                        .padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1D2338), Color(0xFF101114)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ep.episodeNumber.toString(),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "E${ep.episodeNumber} · ${ep.title}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isNow) "Now playing" else ep.duration,
                            color = if (isNow) AccentPrimary else TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Storage meter for the Downloads tab: used/free split for the app's media
 * directory, drawn as a single green bar over a raised track.
 */
@Composable
private fun MbStorageMeter(context: android.content.Context, revision: Int) {
    // Recompute whenever the download count changes. Keying on a constant left
    // the bar showing the pre-delete value until the screen was re-entered.
    val stats = remember(context, revision) {
        runCatching {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                ?: context.filesDir
            val used = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val stat = android.os.StatFs(dir.absolutePath)
            val total = stat.blockCountLong * stat.blockSizeLong
            used to total
        }.getOrDefault(0L to 0L)
    }
    val usedGb = stats.first / 1_073_741_824.0
    val totalGb = stats.second / 1_073_741_824.0
    val fraction = if (stats.second > 0) (stats.first.toFloat() / stats.second) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceRaised)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Device Storage", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = String.format("%.1f GB of %.0f GB", usedGb, totalGb),
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(BrandGreen)
            )
        }
    }
}
