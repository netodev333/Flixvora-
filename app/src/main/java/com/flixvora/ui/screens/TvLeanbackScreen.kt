package com.flixvora.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.LiveChannel
import com.flixvora.data.MediaTitle
import com.flixvora.data.SampleCatalog
import com.flixvora.ui.theme.*

@Composable
fun TvLeanbackScreen(
    catalog: List<MediaTitle>,
    onPlayTitle: (MediaTitle) -> Unit,
    onWatchChannel: (LiveChannel) -> Unit,
    onExitTvMode: () -> Unit
) {
    val accent = LocalAccentTheme.current
    var focusedIndex by remember { mutableIntStateOf(0) }
    var activeChannelIdx by remember { mutableIntStateOf(0) }
    val hero = catalog.getOrElse(focusedIndex) { catalog.first() }
    val currentChannel = SampleCatalog.liveChannels[activeChannelIdx % SampleCatalog.liveChannels.size]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(accent.canvas)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight -> {
                            focusedIndex = (focusedIndex + 1) % catalog.size.coerceAtLeast(1)
                            true
                        }
                        Key.DirectionLeft -> {
                            focusedIndex = (focusedIndex - 1 + catalog.size).coerceAtLeast(0) % catalog.size.coerceAtLeast(1)
                            true
                        }
                        Key.ChannelUp -> {
                            activeChannelIdx = (activeChannelIdx + 1) % SampleCatalog.liveChannels.size
                            true
                        }
                        Key.ChannelDown -> {
                            activeChannelIdx = (activeChannelIdx - 1 + SampleCatalog.liveChannels.size) % SampleCatalog.liveChannels.size
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            onPlayTitle(hero)
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 10-Foot Leanback Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = accent.primary, modifier = Modifier.size(28.dp))
                    Column {
                        Text("FLIXVORA 10-FOOT TV", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("D-Pad Remote Focus & Quick Channel Zapping", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onExitTvMode,
                    border = BorderStroke(1.dp, accent.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Exit TV Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 10-Foot Hero Spotlight Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, accent.primary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            ) {
                AsyncImage(
                    model = hero.backdropUrl,
                    contentDescription = hero.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(accent.canvas.copy(alpha = 0.94f), Color.Transparent)))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${hero.quality} · ${hero.ottProvider}", color = accent.bright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(hero.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(hero.editorialTagline, color = TextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(
                        onClick = { onPlayTitle(hero) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play on TV (OK / Enter)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Channel Zapping Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("QUICK ZAP · CH ${currentChannel.number}", color = LiveCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(currentChannel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(currentChannel.currentProgram, color = TextSecondary, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        activeChannelIdx = (activeChannelIdx - 1 + SampleCatalog.liveChannels.size) % SampleCatalog.liveChannels.size
                    }) { Text("CH -", color = Color.White) }
                    OutlinedButton(onClick = {
                        activeChannelIdx = (activeChannelIdx + 1) % SampleCatalog.liveChannels.size
                    }) { Text("CH +", color = Color.White) }
                    Button(
                        onClick = { onWatchChannel(currentChannel) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary)
                    ) { Text("Tune", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Focusable 10-Foot Carousel Cards
        item {
            Text("D-Pad Focus Row", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(catalog, key = { _, it -> it.id }) { idx, item ->
                    var hasFocus by remember { mutableStateOf(false) }
                    val isSelected = idx == focusedIndex || hasFocus
                    Column(
                        modifier = Modifier
                            .width(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accent.surface)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) accent.primary else BorderHairline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .onFocusChanged { hasFocus = it.isFocused; if (it.isFocused) focusedIndex = idx }
                            .focusable()
                            .clickable {
                                focusedIndex = idx
                                onPlayTitle(item)
                            }
                    ) {
                        AsyncImage(
                            model = item.backdropUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(112.dp)
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(item.title, color = if (isSelected) accent.primary else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("★ ${item.rating} · ${item.quality}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
