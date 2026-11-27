package com.flixvora.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.*
import com.flixvora.ui.components.SeriesEpisodeAccordion
import com.flixvora.ui.theme.*

@Composable
fun DetailScreen(
    title: MediaTitle,
    inWatchlist: Boolean,
    comments: List<CommentItem>,
    shareUrl: String,
    recommendations: List<MediaTitle> = emptyList(),
    downloadQualities: List<String> = emptyList(),
    onOpenRecommendation: (MediaTitle) -> Unit = {},
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayEpisode: (Int, EpisodeItem) -> Unit,
    onDownloadQuality: (String, Int) -> Unit,
    onDownloadEpisode: (Int, EpisodeItem) -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenWatchParty: () -> Unit,
    onOpenSources: () -> Unit,
    onPostComment: (String, Boolean) -> Unit,
    onNotify: (String) -> Unit
) {
    val accent = LocalAccentTheme.current
    val context = LocalContext.current
    var showQualitySheet by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    var markSpoiler by remember { mutableStateOf(false) }
    var revealedSpoilers by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(accent.canvas),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Hero Backdrop Header
        item {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = title.backdropUrl,
                    contentDescription = title.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), accent.canvas)))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title.title)
                                    putExtra(Intent.EXTRA_TEXT, "Watch ${title.title} on Flixvora: $shareUrl")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share ${title.title}"))
                                onNotify("Shared deep link: $shareUrl")
                            },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Deep Link", tint = accent.primary)
                        }
                        IconButton(
                            onClick = onOpenSources,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = "Sources", tint = Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = title.posterUrl,
                        contentDescription = title.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${title.ottProvider} · ${title.quality}", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(title.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("★ ${title.rating} · ${title.year} · ${title.duration} · ${title.ageRating}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title.editorialTagline, color = accent.bright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(title.synopsis, color = Color.White.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 19.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Play Now", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showQualitySheet = !showQualitySheet },
                        border = BorderStroke(1.dp, BorderHairline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = accent.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Download", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Secondary Actions: Watchlist, Watch Party, Share Link
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        border = BorderStroke(1.dp, if (inWatchlist) accent.primary else BorderHairline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(
                            if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (inWatchlist) accent.primary else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (inWatchlist) "Saved" else "Watchlist", color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenWatchParty,
                        border = BorderStroke(1.dp, BorderHairline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Watch Party", color = Color.White, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, title.title)
                                putExtra(Intent.EXTRA_TEXT, "Watch ${title.title} on Flixvora: $shareUrl")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share ${title.title}"))
                            onNotify("Share sheet opened")
                        },
                        border = BorderStroke(1.dp, BorderHairline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = accent.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Download Quality Picker
                if (showQualitySheet) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.surface)
                            .border(1.dp, accent.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Select Download Quality", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        val options = downloadQualities.ifEmpty { listOf("Auto · Best available") }
                        options.forEach { quality ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.canvas)
                                    .clickable {
                                        onDownloadQuality(quality, 0)
                                        showQualitySheet = false
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(quality, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Auto", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Cast Strip
        if (title.cast.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cast & Crew", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(title.cast) { member ->
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.surface)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(member.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(member.role, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Series / Anime Episode Accordion
        if (title.seasons.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                    SeriesEpisodeAccordion(
                        title = title,
                        onPlayEpisode = onPlayEpisode,
                        onDownloadEpisode = onDownloadEpisode
                    )
                }
            }
        }

        // Real catalogue recommendations for this title
        if (recommendations.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "More Like This",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recommendations.take(15), key = { it.id }) { rec ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent.surface)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                                    .clickable { onOpenRecommendation(rec) }
                            ) {
                                AsyncImage(
                                    model = rec.posterUrl,
                                    contentDescription = rec.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                                )
                                Text(
                                    rec.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Community Comments & Spoiler Protection
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Community Comments (${comments.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("Add a review or note...", color = TextTertiary, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onPostComment(commentDraft, markSpoiler)
                            commentDraft = ""
                            markSpoiler = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary)
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = markSpoiler, onCheckedChange = { markSpoiler = it })
                    Text("Mark comment as spoiler", color = TextSecondary, fontSize = 12.sp)
                }

                comments.forEach { c ->
                    val hidden = c.hasSpoiler && !revealedSpoilers.contains(c.id)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.surface)
                            .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                            .clickable(enabled = hidden) { revealedSpoilers = revealedSpoilers + c.id }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(c.author, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(c.timestamp, color = TextTertiary, fontSize = 10.sp)
                        }
                        if (hidden) {
                            Text("Spoiler hidden — Tap to reveal", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = c.body,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = if (hidden) Modifier.blur(6.dp) else Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(viewModel: com.flixvora.ui.viewmodel.MovieViewModel) {
    val subject = viewModel.selectedSubject ?: return
    DetailScreen(
        title = com.flixvora.data.MediaTitle(
            id = subject.id,
            tmdbId = subject.id.toIntOrNull() ?: 1001,
            title = subject.name,
            editorialTagline = subject.description,
            category = if (subject.isTvShow) com.flixvora.data.MediaCategory.SERIES else com.flixvora.data.MediaCategory.MOVIE,
            year = subject.year?.toIntOrNull() ?: 2024,
            duration = subject.duration.orEmpty().ifBlank { "120 min" },
            rating = subject.rating.orEmpty().ifBlank { "8.5" },
            ageRating = "16+",
            quality = "4K UHD",
            genres = subject.genres,
            ottProvider = "MovieBox",
            synopsis = subject.description,
            posterUrl = subject.poster,
            backdropUrl = subject.backdrop ?: subject.poster
        ),
        inWatchlist = false,
        comments = emptyList(),
        shareUrl = "https://flixvora.app/?title=${subject.id}",
        onBack = { viewModel.selectedSubject = null },
        onPlay = { viewModel.selectMovieDirect() },
        onPlayEpisode = { _, ep -> },
        onDownloadQuality = { _, _ -> },
        onDownloadEpisode = { _, _ -> },
        onToggleWatchlist = {},
        onOpenWatchParty = {},
        onOpenSources = {},
        onPostComment = { _, _ -> },
        onNotify = {}
    )
}
