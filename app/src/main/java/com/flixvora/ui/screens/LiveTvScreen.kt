package com.flixvora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.LiveChannel
import com.flixvora.ui.theme.*

@Composable
fun LiveTvScreen(
    channels: List<LiveChannel> = emptyList(),
    backgroundAudioEnabled: Boolean,
    onWatchChannel: (LiveChannel) -> Unit,
    onPlayAudioCommentary: (LiveChannel) -> Unit
) {
    val accent = LocalAccentTheme.current
    val liveList = channels

    if (liveList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No live channels loaded yet. Open Home and pull to refresh.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    var selectedChannel by remember(liveList) { mutableStateOf(liveList.first()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LIVE TV & EPG", color = LiveCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Broadcast & Simulcast", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LiveCrimson.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(LiveCrimson))
                    Spacer(Modifier.width(6.dp))
                    Text("${liveList.size} LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Featured Selected Channel Preview Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
            ) {
                AsyncImage(
                    model = selectedChannel.previewUrl,
                    contentDescription = selectedChannel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), accent.canvas.copy(alpha = 0.94f))))
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "CH ${selectedChannel.number} · ${selectedChannel.streamFormat}",
                        color = accent.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(selectedChannel.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Now: ${selectedChannel.currentProgram}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { selectedChannel.progressPercent / 100f },
                        color = LiveCrimson,
                        trackColor = BgElevated,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Button(
                            onClick = { onWatchChannel(selectedChannel) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Watch Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (backgroundAudioEnabled) {
                            OutlinedButton(
                                onClick = { onPlayAudioCommentary(selectedChannel) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Headphones, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Audio Only", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Interactive EPG Timeline for Selected Channel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("EPG Schedule · ${selectedChannel.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                selectedChannel.epgSlots.forEach { slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (slot.isNow) accent.primary.copy(alpha = 0.14f) else accent.canvas)
                            .border(1.dp, if (slot.isNow) accent.primary.copy(alpha = 0.45f) else BorderHairline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(slot.timeWindow, color = if (slot.isNow) accent.primary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(slot.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (slot.isNow) {
                            Text("ON AIR", color = LiveCrimson, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // All Channels Selector List
        item {
            Text("All Channels", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        items(liveList, key = { it.id }) { ch ->
            val active = ch.id == selectedChannel.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) accent.primary.copy(alpha = 0.12f) else accent.surface)
                    .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(14.dp))
                    .clickable { selectedChannel = ch }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = if (active) accent.primary else TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("CH ${ch.number} · ${ch.name}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(ch.currentProgram, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = { onWatchChannel(ch) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = accent.primary)
                }
            }
        }
    }
}

@Composable
fun LiveTvScreen(viewModel: com.flixvora.ui.viewmodel.MovieViewModel) {
    LiveTvScreen(
        backgroundAudioEnabled = true,
        onWatchChannel = { ch -> },
        onPlayAudioCommentary = { ch -> }
    )
}
