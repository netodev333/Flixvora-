package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flixvora.data.StreamSource
import com.flixvora.ui.theme.*

@Composable
fun StreamResolverSheet(
    mediaTitle: String,
    tmdbId: Int,
    sources: List<StreamSource>,
    activeSourceId: String,
    onSelectSource: (StreamSource) -> Unit,
    onSimulateFailover: () -> Unit,
    onClose: () -> Unit
) {
    val accent = LocalAccentTheme.current

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(accent.surface)
                .border(1.dp, BorderHairline, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MULTI-SOURCE RESOLVER", color = accent.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Backup Stream Mirrors", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("$mediaTitle · TMDB #$tmdbId", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Button(
                onClick = onSimulateFailover,
                colors = ButtonDefaults.buttonColors(containerColor = RatingGold.copy(alpha = 0.16f), contentColor = RatingGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simulate Auto-Failover", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.id }) { src ->
                    val active = src.id == activeSourceId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) accent.primary.copy(alpha = 0.14f) else accent.canvas)
                            .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(14.dp))
                            .clickable { onSelectSource(src) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = if (active) accent.primary else TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(src.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${src.protocol} · ${src.quality} · ${src.bitrateMbps} Mbps", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${src.pingMs} ms", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (active) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
