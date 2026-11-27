package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.AudioTrackMetadata
import com.flixvora.data.PlayerSession
import com.flixvora.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerOverlay(
    session: PlayerSession,
    backgroundAudioEnabled: Boolean,
    onClose: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSourceResolver: () -> Unit,
    onMinimizeToBackground: () -> Unit,
    onNotify: (String) -> Unit
) {
    val accent = LocalAccentTheme.current
    var isPlaying by remember(session) { mutableStateOf(true) }
    var isLocked by remember(session) { mutableStateOf(false) }
    var currentSec by remember(session) { mutableIntStateOf(32) }
    var quality by remember { mutableStateOf("4K Dolby Vision") }
    var selectedAudio by remember(session) {
        mutableStateOf(session.audioTracks.firstOrNull { it.isDefault } ?: session.audioTracks.first())
    }
    var subtitle by remember { mutableStateOf("English (OpenSubtitles)") }
    var subtitleOffsetMs by remember { mutableIntStateOf(0) }
    var activeSheet by remember { mutableStateOf("none") } // none, tracks, subtitles

    LaunchedEffect(session, isPlaying) {
        while (isPlaying && !session.isLive) {
            delay(1000)
            currentSec = if (currentSec + 1 >= session.durationSeconds) 0 else currentSec + 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = session.backdropUrl,
            contentDescription = session.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        if (!isLocked) {
            // Top Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Column {
                        Text(session.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${session.subtitle} · $quality", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenSourceResolver) {
                        Icon(Icons.Default.Dns, contentDescription = "Sources", tint = accent.primary)
                    }
                    IconButton(onClick = {
                        if (backgroundAudioEnabled) onMinimizeToBackground()
                        else onNotify("Enable Background Audio Service in Me → Settings")
                    }) {
                        Icon(Icons.Default.Headphones, contentDescription = "Audio Mode", tint = if (backgroundAudioEnabled) accent.primary else TextTertiary)
                    }
                    IconButton(onClick = onEnterPip) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                    }
                    IconButton(onClick = { isLocked = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                    }
                }
            }

            // Center Play/Pause/Seek
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!session.isLive) {
                    IconButton(
                        onClick = { currentSec = (currentSec - 10).coerceAtLeast(0) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(accent.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Pause",
                        tint = accent.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                if (!session.isLive) {
                    IconButton(
                        onClick = { currentSec = (currentSec + 10).coerceAtMost(session.durationSeconds) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White)
                    }
                }
            }

            // Skip Intro
            if (!session.isLive && currentSec <= session.introEndSeconds + 15) {
                Button(
                    onClick = {
                        currentSec = session.introEndSeconds + 20
                        onNotify("Skipped opening intro")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f), contentColor = accent.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 110.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Skip Intro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Timeline & Track Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!session.isLive) {
                    Slider(
                        value = currentSec.toFloat(),
                        onValueChange = { currentSec = it.toInt() },
                        valueRange = 0f..session.durationSeconds.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(thumbColor = accent.primary, activeTrackColor = accent.primary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { activeSheet = if (activeSheet == "subtitles") "none" else "subtitles" }) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Subtitles", color = Color.White, fontSize = 12.sp)
                        }
                        TextButton(onClick = { activeSheet = if (activeSheet == "tracks") "none" else "tracks" }) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quality & Audio", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "${selectedAudio.code} · ${selectedAudio.codec}",
                        color = accent.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = { isLocked = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f), contentColor = accent.primary),
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Unlock", fontSize = 12.sp)
            }
        }

        // Dynamic Language & Audio Track Selector Sheet
        if (activeSheet == "tracks" && !isLocked) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF101218).copy(alpha = 0.96f))
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language & Audio (${session.audioTracks.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("TMDB #${session.tmdbId}", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(session.audioTracks, key = { it.id }) { track: AudioTrackMetadata ->
                        val active = track.id == selectedAudio.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accent.primary.copy(alpha = 0.16f) else Color.Transparent)
                                .clickable {
                                    selectedAudio = track
                                    activeSheet = "none"
                                    onNotify("Audio: ${track.language} (${track.codec})")
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(track.language, color = if (active) accent.primary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${track.codec} · ${track.channels} · ${track.bitrate}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Text(track.code, color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = BorderHairline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("4K Dolby Vision", "1080p High", "720p Saver").forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { quality = q; activeSheet = "none" },
                            label = { Text(q.substringBefore(" "), fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (activeSheet == "subtitles" && !isLocked) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF101218).copy(alpha = 0.96f))
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("OpenSubtitles & Timing Sync", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                listOf("English (OpenSubtitles)", "Hindi (OpenSubtitles)", "Japanese", "Off").forEach { sub ->
                    Text(
                        text = sub,
                        color = if (subtitle == sub) accent.primary else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (subtitle == sub) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subtitle = sub }
                            .padding(vertical = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { subtitleOffsetMs = (subtitleOffsetMs - 250).coerceAtLeast(-2000) }) {
                        Text("-250ms", color = Color.White)
                    }
                    Text("${subtitleOffsetMs} ms", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { subtitleOffsetMs = (subtitleOffsetMs + 250).coerceAtMost(2000) }) {
                        Text("+250ms", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PersistentMiniPlayerBar(
    session: PlayerSession,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    val accent = LocalAccentTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.surface)
            .border(1.dp, accent.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Headphones, contentDescription = null, tint = accent.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(session.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Background Audio · ${session.subtitle}", color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Pause",
                    tint = accent.primary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }
    }
}
