package com.flixvora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.flixvora.data.MediaTitle
import com.flixvora.ui.components.ChunkedDownloadSection
import com.flixvora.ui.state.FlixvoraUiState
import com.flixvora.ui.theme.*

@Composable
fun WatchlistScreen(
    uiState: FlixvoraUiState,
    onOpenDetail: (MediaTitle) -> Unit,
    onPlayTitle: (MediaTitle) -> Unit,
    onRemoveWatchlist: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val accent = LocalAccentTheme.current
    val saved = uiState.safeCatalog.filter { uiState.watchlist.contains(it.id) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SAVED LIBRARY", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("My Watchlist (${saved.size})", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (saved.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = LiveCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (saved.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Your watchlist is empty. Tap + on any title to save it.", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(saved, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                        .clickable { onOpenDetail(item) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(62.dp).height(86.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${item.year} · ${item.quality} · ★ ${item.rating}", color = TextSecondary, fontSize = 11.sp)
                        Text(item.ottProvider, color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { onPlayTitle(item) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = accent.primary)
                    }
                    IconButton(onClick = { onRemoveWatchlist(item.id) }) {
                        Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(
    uiState: FlixvoraUiState,
    onToggleNetwork: () -> Unit,
    onToggleTaskStatus: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val accent = LocalAccentTheme.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("OFFLINE ENGINE", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Chunked Downloads", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (uiState.downloads.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = LiveCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.downloads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No offline downloads queued. Tap Download on any title.", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            item {
                ChunkedDownloadSection(
                    tasks = uiState.downloads,
                    networkOnline = uiState.networkOnline,
                    wifiOnly = uiState.storageConfig.wifiOnly,
                    onToggleNetwork = onToggleNetwork,
                    onToggleTaskStatus = onToggleTaskStatus,
                    onRemoveTask = onRemoveTask
                )
            }
        }
    }
}
