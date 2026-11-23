package com.flixvora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.local.FavoriteEntity
import com.flixvora.data.local.HistoryEntity
import com.flixvora.data.model.Episode
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.StreamItem
import com.flixvora.data.model.SubtitleItem
import com.flixvora.ui.theme.*
import com.flixvora.ui.viewmodel.MovieViewModel
import com.flixvora.ui.viewmodel.ActiveDownload
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.net.Uri
import android.content.Intent
import com.flixvora.ui.components.BrandGradient
import com.flixvora.ui.components.bounceClick
import com.flixvora.ui.components.safeTopInset
import com.flixvora.ui.components.safeBottomInset
import com.flixvora.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(viewModel: MovieViewModel) {
    val subject = viewModel.selectedSubject ?: return
    val isFav by viewModel.isFavoriteFlow(subject.id).collectAsState(false)
    val context = LocalContext.current

    BackHandler {
        viewModel.selectedSubject = null
    }

    LaunchedEffect(subject.id) {
        viewModel.loadComments(subject.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Opaque root: the detail screen is a full-screen overlay, so without
            // its own background the tab underneath showed straight through.
            .background(ObsidianDark)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Backdrop and poster cover
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    val optimalBackdropUrl = androidx.compose.runtime.remember(subject.backdrop, subject.poster) {
                        val sourceUrl = subject.backdrop?.ifBlank { null } ?: subject.poster
                        com.flixvora.util.ImageOptimizer.getOptimalUrl(sourceUrl, 1080)
                    }
                    val ambientColor = androidx.compose.runtime.remember(subject.averageHue) {
                        com.flixvora.util.ImageOptimizer.parseAverageHue(subject.averageHue)
                    }
                    val placeholderPainter = androidx.compose.runtime.remember(subject.blurHash, ambientColor) {
                        com.flixvora.util.ImageOptimizer.getBlurHashPainter(subject.blurHash, 30, 20)
                            ?: androidx.compose.ui.graphics.painter.ColorPainter(ambientColor)
                    }

                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(optimalBackdropUrl)
                            .memoryCacheKey(optimalBackdropUrl)
                            .diskCacheKey(optimalBackdropUrl)
                            .precision(coil.size.Precision.EXACT)
                            .crossfade(true)
                            .build(),
                        contentDescription = subject.name,
                        contentScale = ContentScale.Crop,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Backdrop gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        ObsidianDark
                                    )
                                )
                            )
                    )

                    // Close Button
                    IconButton(
                        onClick = { viewModel.selectedSubject = null },
                        modifier = Modifier
                            .then(Modifier.padding(top = safeTopInset()))
                            .padding(top = 16.dp, start = 16.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .testTag("detail_close_button")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            // Title block
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Demo badge strip: quality, audio and access markers sit
                    // above the title as small pills.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        MbDetailBadge(if (subject.isTvShow) "HD" else "4K", BrandGreen)
                        if (!subject.isTvShow) MbDetailBadge("DOLBY", BrandBlue)
                        subject.duration?.takeIf { it.isNotBlank() && it != "Not available" }?.let {
                            MbDetailBadge(it.uppercase(), TextSecondary)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.name,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 32.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.toggleFavorite(subject) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) CinematicRed else TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!subject.rating.isNullOrBlank() && subject.rating != "Not available" && subject.rating != "0" && subject.rating != "0.0") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(subject.rating, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        if (!subject.year.isNullOrBlank() && subject.year != "Not available") {
                            Text(subject.year, color = TextSecondary, fontSize = 14.sp)
                        }
                        if (!subject.duration.isNullOrBlank() && subject.duration != "Not available") {
                            Text(subject.duration, color = TextSecondary, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genres row — outlined pills, matching the demo's tag strip.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subject.genres.take(4).forEach { gen ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(1.dp, OutlineSubtle, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = gen,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Audio Language / Dubs row
                    val audioDisplay = when {
                        subject.dubs.isNotEmpty() -> subject.dubs.joinToString(", ")
                        subject.resourceDetectors.isNotEmpty() -> subject.resourceDetectors.joinToString(", ") { it.name }
                        else -> "Not available"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Audio Language", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio: $audioDisplay",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Social actions: like, rate, share
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleLike(subject.id) }
                        ) {
                            Icon(
                                imageVector = if (viewModel.isSubjectLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (viewModel.isSubjectLiked) CinematicRed else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Like", fontSize = 12.sp, color = TextSecondary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleWatchlist(subject.id) }
                        ) {
                            Icon(
                                imageVector = if (viewModel.isInWatchlist) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (viewModel.isInWatchlist) AccentAmber else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (viewModel.isInWatchlist) "Saved" else "Watchlist",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.shareSubject(subject.id, subject.name, context) }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description text
                    Text(
                        text = subject.description.ifEmpty { "No description available for this item." },
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    // Only sections backed by catalog data are shown. Stills,
                    // production history and trivia have no field in the API, so
                    // inventing them would show the viewer fiction as fact.
                    Spacer(modifier = Modifier.height(8.dp))
                    com.flixvora.ui.components.QualityLadderSection(
                        streams = viewModel.detailStreams,
                        onPick = { quality ->
                            android.widget.Toast.makeText(
                                context,
                                "Start playback first, then pick $quality from Downloads",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    com.flixvora.ui.components.DetailsTableSection(
                        rows = buildList {
                            subject.year?.takeIf { it.isNotBlank() }?.let { add("Year" to it) }
                            if (subject.genres.isNotEmpty()) {
                                add("Genres" to subject.genres.joinToString(", "))
                            }
                            subject.duration?.takeIf { it.isNotBlank() && it != "Not available" }
                                ?.let { add("Runtime" to it) }
                            if (subject.isTvShow && subject.totalSeasons > 0) {
                                add("Seasons" to subject.totalSeasons.toString())
                            }
                            if (subject.dubs.isNotEmpty()) {
                                add("Audio" to subject.dubs.joinToString(", "))
                            }
                            subject.rating?.takeIf { it.isNotBlank() }?.let { add("Rating" to "$it / 10") }
                        }
                    )

                    // Live Cast Section
                    if (subject.cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(subject.cast) { actor ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(ObsidianCard)
                                    ) {
                                        if (actor.avatar.isNotEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = actor.avatar,
                                                contentDescription = actor.name,
                                                contentScale = ContentScale.Crop, placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray), error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = "Avatar placeholder",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = actor.name,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = actor.role.ifEmpty { "Cast" },
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // More Like This Section
                    if (viewModel.detailRecommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "More Like This",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(viewModel.detailRecommendations) { movie ->
                                MovieGridItem(movie) {
                                    viewModel.selectSubject(movie)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons — demo layout: wide green Play, then a
                    // ghost "My List" pill and a square save affordance.
                    if (!subject.isTvShow) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.selectMovieDirect() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.flixvora.ui.components.BrandGradient)
                                    .testTag("play_movie_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        "Play Now",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceRaised)
                                    .border(1.dp, OutlineSubtle, RoundedCornerShape(10.dp))
                                    .bounceClick { viewModel.toggleFavorite(subject) }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "My List",
                                        tint = if (isFav) BrandGreen else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFav) "Saved" else "My List",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFav) BrandGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Seasons",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Season Selector Horizontal chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            items(viewModel.seasonsList) { s ->
                                FilterChip(
                                    selected = viewModel.selectedSeason == s,
                                    onClick = { viewModel.loadEpisodesForSeason(s) },
                                    label = { Text("Season $s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CinematicRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = ObsidianCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // TV Show Episodes List Section
            if (subject.isTvShow) {
                if (viewModel.isLoadingEpisodes) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = CinematicRed,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Loading episodes...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else if (viewModel.episodesList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes available for this season",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(viewModel.episodesList) { ep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .clickable { viewModel.selectEpisode(ep) }
                                .background(ObsidianCard, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ep.stillPath.ifEmpty { subject.poster },
                                contentDescription = ep.title,
                                contentScale = ContentScale.Crop, placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray), error = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color.DarkGray),
                                modifier = Modifier
                                    .size(width = 80.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Episode ${ep.episodeNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                                Text(
                                    text = ep.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val context = LocalContext.current
                            IconButton(
                                onClick = { viewModel.downloadSingleEpisode(context, subject, ep) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "Download Episode",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play", tint = CinematicRed, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Community comments (official subject-api/comment/list)
            item {
                CommentsSection(viewModel = viewModel, subjectId = subject.id)
            }

            // Bottom space, plus the system bar the sheet now sits above.
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun CommentsSection(viewModel: MovieViewModel, subjectId: String) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Comments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (viewModel.isLoadingComments) {
                CircularProgressIndicator(color = CinematicRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Add a comment…", color = TextTertiary, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        viewModel.submitComment(subjectId, draft)
                        draft = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send comment", tint = CinematicRed)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val allComments = viewModel.localComments + viewModel.comments

        if (allComments.isEmpty() && !viewModel.isLoadingComments) {
            Text(
                "No comments yet. Be the first to comment.",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            allComments.forEach { comment ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    AsyncImage(
                        model = comment.avatar,
                        contentDescription = comment.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            comment.userName.ifBlank { "User" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(comment.content, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

/** Small outlined marker used for quality/audio badges on the detail hero. */
@Composable
private fun MbDetailBadge(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            letterSpacing = 0.4.sp
        )
    }
}
