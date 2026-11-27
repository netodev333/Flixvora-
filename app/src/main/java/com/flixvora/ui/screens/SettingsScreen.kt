package com.flixvora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.flixvora.R
import com.flixvora.ui.theme.*
import com.flixvora.ui.viewmodel.MovieViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MovieViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToWatchlist: () -> Unit = {},
    onNavigateToManageCategories: () -> Unit = {}
) {
    val context = LocalContext.current
    val history by viewModel.historyList.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()

    val groupedHistory = remember(history) {
        history.groupBy { it.subjectId }.map { it.value.first() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 20.dp)
    ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                MbProfileHeader(
                    watchedCount = groupedHistory.size,
                    savedCount = favorites.size,
                    // Sum every history row, not one per subject: deduplicating
                    // first threw away the watch time of all but the newest entry.
                    watchHours = formatWatchHours(history.sumOf { it.seeTime })
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Section Label: MEDIA LIBRARY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BrandGreen)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "MEDIA LIBRARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }

                // Group 1: Media Library Card (Watch History + Watchlist)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Watch History Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToHistory() }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = "Watch History",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Watch History",
                                                fontSize = 15.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (groupedHistory.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${groupedHistory.size}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (groupedHistory.isNotEmpty()) "${groupedHistory.size} recently watched" else "View your watch history and progress",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Navigate",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // History Posters Carousel
                            if (groupedHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(groupedHistory.take(5)) { item ->
                                        Box(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF28292E))
                                                .clickable { onNavigateToHistory() }
                                        ) {
                                            AsyncImage(
                                                model = item.poster,
                                                contentDescription = item.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                                            startY = 25f
                                                        )
                                                    )
                                            )
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        // My Watchlist Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToWatchlist() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "My Watchlist",
                                    tint = CinematicRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "My Watchlist",
                                            fontSize = 15.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (favorites.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(CinematicRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${favorites.size}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CinematicRed
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (favorites.isNotEmpty()) "${favorites.size} saved" else "View your saved movies & series",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Navigate",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section Label: PREFERENCES
                Text(
                    text = "PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.8f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                // Group 2: Content & Feed Preferences (Location + Manage Categories)
                var showLocationDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Location Selection Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLocationDialog = true }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Location",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Location",
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = when (viewModel.customCountryIso) {
                                            "us" -> "United States"
                                            "br" -> "Brazil"
                                            "ru" -> "Russia"
                                            else -> "India"
                                        },
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Navigate",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        // Manage Categories Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToManageCategories() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.List,
                                    contentDescription = "Manage Categories",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Manage Categories",
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Add or remove Home Feed tabs",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Navigate",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        AdultContentRow(
                            enabled = viewModel.adultContentEnabled,
                            onToggle = { viewModel.applyAdultContentSetting(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "PLAYBACK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.8f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                var showPlaybackDialog by remember { mutableStateOf(false) }
                var showSubtitleDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsValueRow(
                            icon = Icons.Filled.HighQuality,
                            title = "Playback Settings",
                            value = "${viewModel.defaultStreamQuality} streaming - ${viewModel.defaultDownloadQuality} downloads",
                            onClick = { showPlaybackDialog = true }
                        )
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        SettingsValueRow(
                            icon = Icons.Filled.Subtitles,
                            title = "Subtitle Language",
                            value = viewModel.subtitleLanguage,
                            onClick = { showSubtitleDialog = true }
                        )
                    }
                }

                if (showPlaybackDialog) {
                    ChoiceDialog(
                        title = "Playback quality",
                        options = listOf("Auto", "480p", "720p", "1080p"),
                        selected = viewModel.defaultStreamQuality,
                        onSelect = { viewModel.setDefaultStreamQualityPref(it) },
                        onDismiss = { showPlaybackDialog = false }
                    )
                }

                if (showSubtitleDialog) {
                    ChoiceDialog(
                        title = "Subtitle language",
                        options = listOf("English", "Hindi", "Spanish", "Portuguese", "Russian"),
                        selected = viewModel.subtitleLanguage,
                        onSelect = { viewModel.setSubtitleLanguagePref(it) },
                        onDismiss = { showSubtitleDialog = false }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section Label: ABOUT & COMMUNITY
                Text(
                    text = "ABOUT & COMMUNITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.8f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                // Group 3: Community & Updates
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Check for Updates Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !viewModel.isCheckingUpdate) {
                                    viewModel.checkForUpdates(isManualCheck = true) { hasUpdate, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = "Check for Updates",
                                    tint = CinematicRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Check for Updates",
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val appVersion = remember {
                                        try {
                                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                                                ?: com.flixvora.BuildConfig.VERSION_NAME
                                        } catch (e: Exception) { com.flixvora.BuildConfig.VERSION_NAME }
                                    }
                                    Text(
                                        text = if (viewModel.isCheckingUpdate) "Checking latest version..." else "Current: v$appVersion",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (viewModel.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CinematicRed,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Check",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (showLocationDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationDialog = false },
                        title = { Text("Select Region", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                val regions = listOf(
                                    Triple("90101", "us", "United States"),
                                    Triple("40401", "in", "India"),
                                    Triple("72402", "br", "Brazil"),
                                    Triple("25001", "ru", "Russia")
                                )
                                regions.forEach { reg ->
                                    val isSelected = viewModel.customMcc == reg.first && viewModel.customCountryIso == reg.second
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.applyCustomSandbox(reg.first, reg.second)
                                                showLocationDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.applyCustomSandbox(reg.first, reg.second)
                                                showLocationDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = CinematicRed)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(reg.third, color = TextPrimary, fontSize = 16.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLocationDialog = false }) {
                                Text("Close", color = TextSecondary)
                            }
                        },
                        containerColor = ObsidianCard
                    )
                }


                Spacer(modifier = Modifier.height(28.dp))

                Spacer(modifier = Modifier.height(80.dp))
            }
    }
}

/** Generic "label - current value" settings row used by the playback rows. */
@Composable
private fun SettingsValueRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = value, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Single-select list dialog shared by the playback and subtitle rows. */
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.equals(selected, ignoreCase = true),
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        },
        containerColor = ObsidianCard
    )
}

/**
 * Adult (18+) opt-in. Off by default and buried in Settings rather than offered
 * in onboarding: enabling it mixes adult rows into the normal rails, so the
 * confirmation step matters more than discoverability.
 */
@Composable
private fun AdultContentRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (enabled) onToggle(false) else showConfirm = true }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = "Adult content",
                tint = if (enabled) CinematicRed else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Adult content (18+)",
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) "Mixed into your feeds" else "Hidden from all feeds",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = { if (it) showConfirm = true else onToggle(false) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CinematicRed
            )
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Show adult content?") },
            text = {
                Text(
                    "18+ titles will be mixed into your Home and category feeds. " +
                        "You can turn this off at any time from Settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onToggle(true)
                }) { Text("I am 18 or older") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Whole-hour integer division showed "0" for anyone under an hour of viewing.
 * One decimal keeps the stat meaningful from the first few minutes.
 */
private fun formatWatchHours(totalMillis: Long): String {
    val hours = totalMillis / 3_600_000.0
    return if (hours >= 10) hours.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", hours)
}

/**
 * Demo-style profile block for the "Me" tab: circular avatar, name, tenure line
 * and a three-up stat strip. There is no account system in this app, so the
 * identity is local and the numbers come from real on-device history.
 */
@Composable
private fun MbProfileHeader(watchedCount: Int, savedCount: Int, watchHours: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(com.flixvora.ui.components.BrandGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Flixvora",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Local library \u00b7 no sign-in",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceRaised)
                .border(1.dp, OutlineSubtle, RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MbStat(watchedCount.toString(), "Watched")
            }
            StatDivider()
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MbStat(savedCount.toString(), "Saved")
            }
            StatDivider()
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MbStat(watchHours, "Hrs watched")
            }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(24.dp)
            .background(OutlineSubtle)
    )
}

@Composable
private fun MbStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen
        )
        Text(text = label, fontSize = 9.5.sp, color = TextTertiary)
    }
}
