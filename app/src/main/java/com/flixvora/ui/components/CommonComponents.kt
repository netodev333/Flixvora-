package com.flixvora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.*
import com.flixvora.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HeroSpotlightCarousel(
    slides: List<MediaTitle>,
    watchlist: Set<String>,
    onPlay: (MediaTitle) -> Unit,
    onDownload: (MediaTitle) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onOpenDetail: (MediaTitle) -> Unit
) {
    if (slides.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(slides.size) {
        while (true) {
            delay(5000)
            index = (index + 1) % slides.size
        }
    }

    val hero = slides[index % slides.size]
    val accent = LocalAccentTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
            .clickable { onOpenDetail(hero) }
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
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            accent.canvas.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        // Slide indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slides.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == index) accent.primary
                            else Color.White.copy(alpha = 0.28f)
                        )
                        .clickable { index = i }
                )
            }
        }

        // Glass bottom info overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.canvas.copy(alpha = 0.68f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${hero.rank ?: index + 1} Spotlight · ${hero.quality}",
                    color = accent.bright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hero.ottProvider,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = hero.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "★ ${hero.rating} · ${hero.year} · ${hero.genres.take(2).joinToString(" · ")}",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlay(hero) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.primary,
                        contentColor = accent.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onDownload(hero) },
                    border = BorderStroke(1.dp, BorderHairline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = accent.primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 12.sp)
                }

                IconButton(
                    onClick = { onToggleWatchlist(hero.id) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = if (watchlist.contains(hero.id)) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Watchlist",
                        tint = if (watchlist.contains(hero.id)) accent.primary else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalizedForYouRow(
    recommendations: List<PersonalizedRecommendation>,
    traktConnected: Boolean,
    onSyncTrakt: () -> Unit,
    onSelectTitle: (MediaTitle) -> Unit
) {
    val accent = LocalAccentTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                    Text("Personalized For You", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = if (traktConnected) "Curated from Trakt scrobbles & watch history" else "Curated from watch history & saved list",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            TextButton(onClick = onSyncTrakt) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = accent.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (traktConnected) "Trakt Synced" else "Sync Trakt", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recommendations, key = { it.item.id }) { rec ->
                Column(
                    modifier = Modifier
                        .width(136.dp)
                        .clickable { onSelectTitle(rec.item) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = rec.item.posterUrl,
                            contentDescription = rec.item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                        )
                        Text(
                            text = "★ ${rec.item.rating}",
                            color = RatingGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${rec.matchPercent}% Match", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${rec.item.year}", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    Text(rec.item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(rec.reason, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ComingSoonRow(
    releases: List<ComingSoonRelease>,
    remindedIds: Set<String>,
    onToggleReminder: (ComingSoonRelease) -> Unit
) {
    val accent = LocalAccentTheme.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
            Text("Coming Soon", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(releases, key = { it.id }) { release ->
                val remainingSec = ((release.releaseEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
                val days = remainingSec / 86400
                val hours = (remainingSec % 86400) / 3600
                val mins = (remainingSec % 3600) / 60
                val secs = remainingSec % 60
                val reminded = remindedIds.contains(release.id)

                Column(
                    modifier = Modifier
                        .width(276.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(132.dp)) {
                        AsyncImage(
                            model = release.backdropUrl,
                            contentDescription = release.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, accent.surface)))
                        )
                        Text(
                            text = "${days}d ${hours}h ${mins}m ${secs}s",
                            color = accent.bright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${release.categoryLabel} · ${release.quality}",
                            color = accent.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(release.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(release.tagline, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Button(
                            onClick = { onToggleReminder(release) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reminded) accent.primary else BgElevated,
                                contentColor = if (reminded) accent.onPrimary else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(
                                imageVector = if (reminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (reminded) "Reminder Set" else "Remind Me", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesEpisodeAccordion(
    title: MediaTitle,
    onPlayEpisode: (Int, EpisodeItem) -> Unit,
    onDownloadEpisode: (Int, EpisodeItem) -> Unit
) {
    if (title.seasons.isEmpty()) return
    val accent = LocalAccentTheme.current
    var selectedSeasonIdx by remember(title.id) { mutableIntStateOf(0) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var expandedEpisodeNum by remember(title.id, selectedSeasonIdx) {
        mutableStateOf<Int?>(title.seasons.getOrNull(selectedSeasonIdx)?.episodes?.firstOrNull()?.number)
    }

    val season = title.seasons[selectedSeasonIdx.coerceIn(0, title.seasons.lastIndex)]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Episodes (${season.episodes.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderHairline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = accent.surface, contentColor = Color.White)
                ) {
                    Text(season.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(BgElevated)
                ) {
                    title.seasons.forEachIndexed { idx, s ->
                        DropdownMenuItem(
                            text = { Text("${s.label} (${s.episodes.size} ep)", color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                selectedSeasonIdx = idx
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        season.episodes.forEach { ep ->
            val isOpen = expandedEpisodeNum == ep.number
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.surface)
                    .border(
                        1.dp,
                        if (isOpen) accent.primary.copy(alpha = 0.45f) else BorderHairline,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedEpisodeNum = if (isOpen) null else ep.number }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ep ${ep.number} · ${ep.title}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${ep.duration} · ${ep.sizeMb} MB", color = TextSecondary, fontSize = 11.sp)
                    }
                    Icon(
                        imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = accent.primary
                    )
                }

                AnimatedVisibility(
                    visible = isOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(ep.synopsis, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onPlayEpisode(season.seasonNumber, ep) },
                                colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Play Ep ${ep.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onDownloadEpisode(season.seasonNumber, ep) },
                                border = BorderStroke(1.dp, BorderHairline),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compatibility Helpers & Insets for legacy components
// ─────────────────────────────────────────────────────────────────────────────
val BrandGradient: Brush = Brush.horizontalGradient(
    listOf(Color(0xFF1DD171), Color(0xFF38E586))
)

val BrandGradientDiagonal: Brush = Brush.linearGradient(
    listOf(Color(0xFF1DD171), Color(0xFF38E586))
)

@Composable
fun safeTopInset(): androidx.compose.ui.unit.Dp {
    val cutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val notch = if (cutout > status) cutout else status
    return if (notch > 12.dp) notch else 12.dp
}

@Composable
fun safeBottomInset(): androidx.compose.ui.unit.Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gesture = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()
    val bar = if (nav > gesture) nav else gesture
    return if (bar > 8.dp) bar else 8.dp
}

fun setSystemBarsVisible(activity: android.app.Activity, visible: Boolean) {
    val controller = androidx.core.view.WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    )
    if (visible) {
        controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    } else {
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legacy Interaction & UI Modifiers
// ─────────────────────────────────────────────────────────────────────────────
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.flixvora.ui.theme.BgDeep)
    ) {
        content()
    }
}

fun Modifier.glassSurface(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    tint: Float = 1f
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f * tint),
                Color.White.copy(alpha = 0.03f * tint)
            )
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.09f * tint), shape)

fun Modifier.bounceClick(onClick: () -> Unit): Modifier = this.clickable { onClick() }

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .then(if (selected) Modifier.background(BrandGradient, shape) else Modifier.glassSurface(shape))
            .bounceClick(onClick)
            .padding(horizontal = 17.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) TextOnAccent else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun MovieGridItem(
    movie: com.flixvora.data.model.MovieSubject,
    modifier: Modifier = Modifier.width(110.dp),
    showRating: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14161D))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = movie.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
