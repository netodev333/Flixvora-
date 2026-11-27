package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.flixvora.data.PlayerSession

/**
 * Real Media3 playback surface. The previous overlay was a still image with a
 * timer; this one attaches the resolved catalogue URL to ExoPlayer so the
 * stream the user picked actually plays.
 */
@Composable
fun RealPlayerOverlay(
    session: PlayerSession,
    onClose: () -> Unit,
    onEnterPip: () -> Unit,
    onNotify: (String) -> Unit,
    onPlayerError: () -> Boolean = { false },
    onProgress: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var error by remember(session.streamUrl) { mutableStateOf<String?>(null) }
    var buffering by remember(session.streamUrl) { mutableStateOf(true) }

    val player = remember(session.streamUrl) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ExoPlayerLib/2.18.7")
            .setAllowCrossProtocolRedirects(true)
        if (session.headers.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(session.headers)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val itemBuilder = MediaItem.Builder().setUri(session.streamUrl)
                session.subtitleUrl?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    val mime = if (subtitle.endsWith(".vtt", ignoreCase = true)) {
                        MimeTypes.TEXT_VTT
                    } else {
                        MimeTypes.APPLICATION_SUBRIP
                    }
                    itemBuilder.setSubtitleConfigurations(
                        listOf(
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle))
                                .setMimeType(mime)
                                .setLanguage("en")
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        )
                    )
                }
                setMediaItem(itemBuilder.build())
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                // A dead mirror should not end the session: advance to the next
                // resolved source and only surface the error when all are tried.
                if (!onPlayerError()) {
                    error = e.message ?: "Playback failed"
                    buffering = false
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose {
            onProgress(player.currentPosition, player.duration.coerceAtLeast(0))
            player.removeListener(listener)
            player.release()
        }
    }

    androidx.compose.runtime.LaunchedEffect(player, session.subjectId) {
        if (session.subjectId.isBlank() || session.isLive) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(10_000)
            val duration = player.duration
            if (duration > 0) {
                onProgress(player.currentPosition, duration)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close player", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    session.subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEnterPip) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = "Picture in picture",
                    tint = Color.White
                )
            }
        }

        if (buffering && error == null) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        error?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Playback error", color = Color.White, fontWeight = FontWeight.Bold)
                Text(message, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
    }
}
