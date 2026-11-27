package com.flixvora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixvora.data.DownloadStatus
import com.flixvora.data.DownloadTask
import com.flixvora.ui.theme.*

@Composable
fun ChunkedDownloadSection(
    tasks: List<DownloadTask>,
    networkOnline: Boolean,
    wifiOnly: Boolean,
    onToggleNetwork: () -> Unit,
    onToggleTaskStatus: (String) -> Unit,
    onRemoveTask: (String) -> Unit
) {
    val accent = LocalAccentTheme.current
    var expandedTaskId by remember { mutableStateOf(tasks.firstOrNull()?.id) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // WorkManager status header
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
                Text("WorkManager · 6 Parallel Chunks", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (networkOnline) "${if (wifiOnly) "Wi-Fi auto-sync" else "All-network sync"} active" else "Paused · Byte offsets saved",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            OutlinedButton(
                onClick = onToggleNetwork,
                border = BorderStroke(1.dp, if (networkOnline) LiveCrimson.copy(alpha = 0.5f) else accent.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (networkOnline) Icons.Default.WifiOff else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (networkOnline) LiveCrimson else accent.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (networkOnline) "Drop Net" else "Reconnect", color = if (networkOnline) LiveCrimson else accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        tasks.forEach { task ->
            val expanded = expandedTaskId == task.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.surface)
                    .border(1.dp, if (task.status == DownloadStatus.COMPLETED) accent.primary.copy(alpha = 0.4f) else BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${task.subtitle} · ${task.sizeMb} MB · ${task.quality}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.status != DownloadStatus.COMPLETED) {
                            IconButton(onClick = { onToggleTaskStatus(task.id) }) {
                                Icon(
                                    imageVector = if (task.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Pause Resume",
                                    tint = accent.primary
                                )
                            }
                        }
                        IconButton(onClick = { onRemoveTask(task.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary)
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    color = accent.primary,
                    trackColor = BgElevated,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedTaskId = if (expanded) null else task.id },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${task.progress}% · ${task.status.name}", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (expanded) "Hide 6 Chunks" else "Inspect 6 Chunks", color = TextSecondary, fontSize = 11.sp)
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        task.chunks.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                pair.forEach { ch ->
                                    val range = (ch.endMb - ch.startMb).coerceAtLeast(1)
                                    val pct = ((ch.downloadedMb * 100) / range).coerceIn(0, 100)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.canvas)
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Chunk #${ch.chunkIndex + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text("$pct%", color = accent.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        LinearProgressIndicator(
                                            progress = { pct / 100f },
                                            color = accent.primary,
                                            trackColor = BgElevated,
                                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                        )
                                        Text("${ch.startMb}-${ch.endMb} MB", color = TextTertiary, fontSize = 9.sp)
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
