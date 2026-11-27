package com.flixvora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixvora.data.ProfileType
import com.flixvora.ui.components.SmartStorageCard
import com.flixvora.ui.state.FlixvoraUiState
import com.flixvora.ui.theme.*

@Composable
fun MeProfileScreen(
    uiState: FlixvoraUiState,
    onSwitchProfile: (ProfileType) -> Unit,
    onSelectTheme: (FlixvoraAccentTheme) -> Unit,
    onToggleTvMode: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onOpenStreamResolver: () -> Unit,
    onSyncTrakt: () -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onToggleAutoDelete: (Boolean) -> Unit,
    onClearTempCache: () -> Unit,
    onClearDownloads: () -> Unit,
    onCheckUpdates: () -> Unit
) {
    val accent = LocalAccentTheme.current
    val watchHours = String.format("%.1f hrs", uiState.storageConfig.watchMinutesTotal / 60f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Member Banner + Watch Hours
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(accent.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FX", color = accent.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("Flixvora Member", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${uiState.activeProfile.label} · $watchHours watched", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Profile Switcher (Main / Kids Safe / Adult 18+ PIN)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Profiles & Parental Lock", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileType.entries.forEach { profile ->
                        val active = uiState.activeProfile == profile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) accent.primary else accent.canvas)
                                .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(12.dp))
                                .clickable { onSwitchProfile(profile) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.label.substringBefore(" "),
                                color = if (active) accent.onPrimary else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Theme & Accent Color Picker (8 Palettes, zero brand names)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = accent.primary, modifier = Modifier.size(18.dp))
                    Text("Theme & Accent Palette", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(FlixvoraAccentTheme.entries) { theme ->
                        val selected = uiState.accentTheme == theme
                        Column(
                            modifier = Modifier
                                .width(92.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) theme.primary.copy(alpha = 0.18f) else accent.canvas)
                                .border(1.5.dp, if (selected) theme.primary else BorderHairline, RoundedCornerShape(12.dp))
                                .clickable { onSelectTheme(theme) }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(theme.primary))
                            Text(theme.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Settings Toggles: Android TV 10-Foot Mode + Background Audio Service + Stream Resolver + Trakt Sync
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.surface)
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Playback & Experience Settings", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Android TV 10-Foot Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("D-Pad focus & quick channel zapping", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(checked = uiState.isTvMode, onCheckedChange = { onToggleTvMode() })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Headphones, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Background Audio Service", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("MediaSessionService screen-lock playback", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(checked = uiState.backgroundAudioEnabled, onCheckedChange = { onToggleBackgroundAudio() })
                }

                HorizontalDivider(color = BorderHairline)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenStreamResolver() }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Backup Stream Resolvers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Auto-failover HLS, DASH & ClearKey mirrors", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSyncTrakt() }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Trakt.tv Scrobble Sync", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (uiState.traktConnected) "Synced (${uiState.traktLastSync})" else "Tap to sync history & watchlist", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Text(if (uiState.traktConnected) "Connected" else "Connect", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onCheckUpdates() }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = accent.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("GitHub OTA Update Check", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Flixvora Android Compose v4.2.0", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Text("Check", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Smart Storage & Clear Temp Buffer Cache Card
        item {
            SmartStorageCard(
                storageConfig = uiState.storageConfig,
                downloads = uiState.downloads,
                onToggleWifiOnly = onToggleWifiOnly,
                onToggleAutoDelete = onToggleAutoDelete,
                onClearTempCache = onClearTempCache,
                onClearDownloads = onClearDownloads
            )
        }
    }
}
