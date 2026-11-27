package com.flixvora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixvora.data.DownloadTask
import com.flixvora.data.SmartStorageConfig
import com.flixvora.ui.theme.*

@Composable
fun SmartStorageCard(
    storageConfig: SmartStorageConfig,
    downloads: List<DownloadTask>,
    onToggleWifiOnly: (Boolean) -> Unit,
    onToggleAutoDelete: (Boolean) -> Unit,
    onClearTempCache: () -> Unit,
    onClearDownloads: () -> Unit
) {
    val accent = LocalAccentTheme.current
    val offlineMb = downloads.sumOf { (it.sizeMb * it.progress) / 100 }
    val totalMb = offlineMb + storageConfig.totalTempCacheMb

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.surface)
            .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = accent.primary, modifier = Modifier.size(18.dp))
                Text("Storage & Cache", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("${String.format("%.2f", totalMb / 1024f)} GB / 64 GB", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Breakdown details
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Offline Media: $offlineMb MB", color = TextSecondary, fontSize = 11.sp)
            Text("Media Buffer: ${storageConfig.mediaBufferMb} MB", color = TextSecondary, fontSize = 11.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Thumbnails: ${storageConfig.thumbCacheMb} MB", color = TextSecondary, fontSize = 11.sp)
            Text("Feed & DB: ${storageConfig.feedCacheMb + storageConfig.dbCacheMb} MB", color = TextSecondary, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wi-Fi Only Downloads", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Pause WorkManager on cellular", color = TextTertiary, fontSize = 10.sp)
            }
            Switch(checked = storageConfig.wifiOnly, onCheckedChange = onToggleWifiOnly)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-Delete Watched", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Reclaim space 24h after finish", color = TextTertiary, fontSize = 10.sp)
            }
            Switch(checked = storageConfig.autoDeleteWatched, onCheckedChange = onToggleAutoDelete)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onClearTempCache,
                enabled = storageConfig.totalTempCacheMb > 0,
                colors = ButtonDefaults.buttonColors(containerColor = accent.primary.copy(alpha = 0.16f), contentColor = accent.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear Cache (${storageConfig.totalTempCacheMb} MB)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (downloads.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearDownloads,
                    border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Clear DLs", color = LiveCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Deletes temporary media buffer & thumbnails only. Watchlist, watch history & app settings remain untouched.",
            color = TextTertiary,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}
