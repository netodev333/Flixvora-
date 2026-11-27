package com.flixvora.ui.screens
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.flixvora.ui.components.ComingSoonRow
import com.flixvora.ui.components.HeroSpotlightCarousel
import com.flixvora.ui.components.PersonalizedForYouRow
import com.flixvora.ui.state.FlixvoraUiState
import com.flixvora.ui.theme.*

@Composable
fun HomeScreen(
    uiState: FlixvoraUiState,
    onRefresh: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onSelectOtt: (String) -> Unit,
    onToggleMood: (String?) -> Unit,
    onSyncTrakt: () -> Unit,
    onToggleReminder: (ComingSoonRelease) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onPlayTitle: (MediaTitle) -> Unit,
    onDownloadTitle: (MediaTitle) -> Unit,
    onOpenDetail: (MediaTitle) -> Unit
) {
    val accent = LocalAccentTheme.current
    val safeCatalog = uiState.safeCatalog

    val categories = remember { listOf("Trending", "Movies", "TV Shows", "Anime", "Asian") }
    val displayCatalog = if (uiState.searchQuery.isNotBlank()) uiState.searchResults else safeCatalog
    val genres = remember(displayCatalog) {
        listOf("Trending") + displayCatalog.flatMap { it.genres }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
    }
    val otts = remember(displayCatalog) {
        listOf("All") + displayCatalog.map { it.ottProvider }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val activeMood = remember(uiState.selectedMoodId) {
        SampleCatalog.moods.firstOrNull { it.id == uiState.selectedMoodId }
    }

    val filteredCatalog = remember(
        displayCatalog,
        uiState.selectedCategory,
        uiState.selectedGenre,
        uiState.selectedOtt,
        uiState.selectedMoodId,
        uiState.searchQuery
    ) {
        displayCatalog.filter { item ->
            val catOk = when (uiState.selectedCategory) {
                "Movies" -> item.category == MediaCategory.MOVIE
                "TV Shows" -> item.category == MediaCategory.SERIES
                "Anime" -> item.category == MediaCategory.ANIME
                else -> true
            }
            val genreOk = uiState.selectedGenre == "Trending" || item.genres.contains(uiState.selectedGenre)
            val ottOk = uiState.selectedOtt == "All" || item.ottProvider == uiState.selectedOtt
            val moodOk = activeMood == null || item.genres.any { activeMood.matchGenres.contains(it) }
            val q = uiState.searchQuery.trim()
            val searchOk = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.genres.any { it.contains(q, ignoreCase = true) } ||
                item.ottProvider.contains(q, ignoreCase = true)
            catOk && genreOk && ottOk && moodOk && searchOk
        }
    }

    val continueWatching = remember(safeCatalog) {
        safeCatalog.filter { it.continueProgress != null }
    }

    val personalizedRecs = remember(
        safeCatalog,
        uiState.watchlist,
        uiState.downloads,
        uiState.traktConnected
    ) {
        SampleCatalog.computePersonalizedRecommendations(
            catalog = safeCatalog,
            watchlist = uiState.watchlist,
            downloads = uiState.downloads,
            traktConnected = uiState.traktConnected
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (uiState.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Loading live catalogue…", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        if (!uiState.loadError.isNullOrBlank() && !uiState.isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.surface)
                        .border(1.dp, LiveCrimson.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        uiState.loadError ?: "",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Retry", color = accent.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // MovieBox Top Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search movies, anime, TV or providers...", color = TextTertiary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent.primary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = accent.surface,
                    unfocusedContainerColor = accent.surface,
                    focusedBorderColor = accent.primary,
                    unfocusedBorderColor = BorderHairline,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Top Category Tabs
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val selected = uiState.selectedCategory == cat
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.primary,
                            selectedLabelColor = accent.onPrimary,
                            containerColor = accent.surface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = BorderHairline,
                            selectedBorderColor = accent.primary
                        )
                    )
                }
            }
        }

        // Hero Spotlight Carousel
        if (uiState.searchQuery.isBlank()) {
            item {
                HeroSpotlightCarousel(
                    slides = safeCatalog.take(3),
                    watchlist = uiState.watchlist,
                    onPlay = onPlayTitle,
                    onDownload = onDownloadTitle,
                    onToggleWatchlist = onToggleWatchlist,
                    onOpenDetail = onOpenDetail
                )
            }
        }

        // Mood Discovery Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Explore by Mood", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SampleCatalog.moods, key = { it.id }) { mood ->
                        val active = uiState.selectedMoodId == mood.id
                        Column(
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) accent.primary.copy(alpha = 0.16f) else accent.surface)
                                .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(14.dp))
                                .clickable { onToggleMood(mood.id) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(mood.accentTag, color = accent.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(mood.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(mood.subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Real, titled catalogue rails straight from the API
        if (uiState.searchQuery.isBlank() && uiState.rails.isNotEmpty()) {
            items(uiState.rails, key = { it.id }) { rail ->
                CatalogRailRow(rail = rail, onOpenDetail = onOpenDetail)
            }
        }

        // Genre & OTT Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres) { genre ->
                        val active = uiState.selectedGenre == genre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) accent.primary else accent.surface)
                                .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(10.dp))
                                .clickable { onSelectGenre(genre) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = genre,
                                color = if (active) accent.onPrimary else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(otts) { ott ->
                        val active = uiState.selectedOtt == ott
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) BgElevated else accent.canvas)
                                .border(1.dp, if (active) accent.primary else BorderHairline, RoundedCornerShape(10.dp))
                                .clickable { onSelectOtt(ott) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ott,
                                color = if (active) accent.primary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Continue Watching Row
        if (continueWatching.isNotEmpty() && uiState.searchQuery.isBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Continue Watching", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(continueWatching, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .width(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.surface)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                                    .clickable { onPlayTitle(item) }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(114.dp)) {
                                    AsyncImage(
                                        model = item.backdropUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(accent.primary)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = accent.onPrimary,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (item.continueProgress ?: 0) / 100f },
                                        color = accent.primary,
                                        trackColor = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${item.continueEpisode ?: "Feature"} · ${item.continueProgress}%", color = accent.primary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Personalized For You (replaces generic Trending row)
        if (uiState.searchQuery.isBlank()) {
            item {
                PersonalizedForYouRow(
                    recommendations = personalizedRecs,
                    traktConnected = uiState.traktConnected,
                    onSyncTrakt = onSyncTrakt,
                    onSelectTitle = onOpenDetail
                )
            }
        }

        // Filtered Poster Grid (2-column pairs inside LazyColumn)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotBlank()) "Results (${filteredCatalog.size})" else "${uiState.selectedCategory} Catalog",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("${filteredCatalog.size} titles", color = TextSecondary, fontSize = 11.sp)
            }
        }

        if (filteredCatalog.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching titles found for current filters.", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredCatalog.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { item ->
                        PosterGridCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenDetail(item) }
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Coming Soon with live countdowns
        item {
            ComingSoonRow(
                releases = uiState.displayComingSoon,
                remindedIds = uiState.remindedReleases,
                onToggleReminder = onToggleReminder
            )
        }
    }
}

@Composable
private fun PosterGridCard(
    item: MediaTitle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = LocalAccentTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.surface)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            Text(
                text = "★ ${item.rating}",
                color = RatingGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Text(
                text = item.ottProvider,
                color = accent.bright,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.year} · ${item.category.label}", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: com.flixvora.ui.viewmodel.MovieViewModel,
    onSearchClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val trending by viewModel.trendingMovies.collectAsState()
    val mediaTitles = remember(trending) {
        trending.map { movie ->
            com.flixvora.data.MediaTitle(
                id = movie.id,
                tmdbId = movie.id.toIntOrNull() ?: 1001,
                title = movie.name,
                editorialTagline = movie.description,
                category = if (movie.isTvShow) com.flixvora.data.MediaCategory.SERIES else com.flixvora.data.MediaCategory.MOVIE,
                year = movie.year?.toIntOrNull() ?: 2024,
                duration = movie.duration.orEmpty().ifBlank { "120 min" },
                rating = movie.rating.orEmpty().ifBlank { "8.5" },
                ageRating = "16+",
                quality = "4K UHD",
                genres = movie.genres,
                ottProvider = "MovieBox",
                synopsis = movie.description,
                posterUrl = movie.poster,
                backdropUrl = movie.backdrop ?: movie.poster
            )
        }.ifEmpty { com.flixvora.data.SampleCatalog.mediaCatalog }
    }

    val mockUiState = remember(mediaTitles) {
        com.flixvora.ui.state.FlixvoraUiState(
            watchlist = emptySet(),
            downloads = emptyList()
        )
    }

    HomeScreen(
        uiState = mockUiState,
        onSearchChange = { onSearchClick() },
        onSelectCategory = {},
        onSelectGenre = {},
        onSelectOtt = {},
        onToggleMood = {},
        onSyncTrakt = {},
        onToggleReminder = {},
        onToggleWatchlist = {},
        onPlayTitle = { item ->
            val match = trending.firstOrNull { it.id == item.id }
            if (match != null) viewModel.playDirect(match)
        },
        onDownloadTitle = {},
        onOpenDetail = { item ->
            val match = trending.firstOrNull { it.id == item.id }
            if (match != null) viewModel.selectSubject(match)
        }
    )
}


@Composable
private fun CatalogRailRow(
    rail: com.flixvora.data.CatalogRail,
    onOpenDetail: (MediaTitle) -> Unit
) {
    val accent = LocalAccentTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rail.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${rail.items.size} titles", color = TextSecondary, fontSize = 11.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rail.items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .width(116.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.surface)
                        .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                        .clickable { onOpenDetail(item) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        )
                        Text(
                            text = "★ ${item.rating}",
                            color = RatingGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
