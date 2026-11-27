package com.flixvora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.flixvora.data.network.OttPlatform
import com.flixvora.data.network.TmdbOttService
import com.flixvora.data.model.CategorySection
import com.flixvora.ui.components.safeTopInset
import com.flixvora.data.model.MovieSubject
import com.flixvora.ui.components.DashboardShimmerScreen
import com.flixvora.ui.components.MovieGridItem
import com.flixvora.ui.components.bounceClick
import com.flixvora.ui.components.glassSurface
import com.flixvora.ui.theme.*
import com.flixvora.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(viewModel: MovieViewModel, onSearchClick: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val trending by viewModel.trendingMovies.collectAsState()
    val categoryMovies by viewModel.categoryMovies.collectAsState()
    val categorySections by viewModel.categorySections.collectAsState()
    val watchHistory by viewModel.historyList.collectAsState()
    val continueWatching = remember(watchHistory) {
        watchHistory
            .filter { it.totalTime > 0 && it.seeTime > 0 && it.seeTime < it.totalTime }
            // Sort here rather than trusting the Room query order: distinctBy
            // keeps the first row per show, so it must be the newest one.
            .sortedByDescending { it.timestamp }
            .distinctBy { it.subjectId }
            .take(12)
    }

    // Watch Party sessions are derived from trending so the rail is populated
    // even before any real party backend exists; the first two read as live.
    val partySessions = remember(trending) {
        trending.take(6).mapIndexed { index, movie ->
            com.flixvora.ui.components.PartySession(
                title = movie.name,
                poster = movie.poster,
                isLive = index < 2,
                statusText = if (index < 2) "${6 + index * 4} min in" else "Starts 9:30 PM",
                people = 3 + index
            )
        }
    }

    var selectedCatalogSection by remember { mutableStateOf<CategorySection?>(null) }
    var selectedOttPlatform by remember { mutableStateOf<OttPlatform?>(null) }

    val validSections = remember(categorySections) {
        categorySections.filter { sec ->
            sec.items.isNotEmpty() &&
            !sec.type.contains("BANNER", ignoreCase = true) &&
            !sec.title.contains("Banner", ignoreCase = true) &&
            !sec.title.equals("Categories", ignoreCase = true) &&
            !sec.title.contains("Star", ignoreCase = true) &&
            !sec.title.contains("Actor", ignoreCase = true) &&
            !sec.title.contains("Short", ignoreCase = true) &&
            !sec.title.contains("Join Us", ignoreCase = true) &&
            !sec.title.contains("Watch on TV", ignoreCase = true) &&
            !sec.title.contains("Get Free Premium", ignoreCase = true) &&
            !sec.title.contains("Diwali", ignoreCase = true) &&
            !sec.title.contains("Offer", ignoreCase = true) &&
            !sec.type.contains("SINGLE_IMAGE", ignoreCase = true) &&
            !sec.type.contains("FILTER", ignoreCase = true) &&
            !sec.type.contains("ACTOR", ignoreCase = true) &&
            !sec.type.contains("STAR", ignoreCase = true)
        }
    }

    val animeRails by viewModel.animeRails.collectAsState()

    val bannerMovies = remember(categorySections, categoryMovies, validSections) {
        val bannerSec = categorySections.firstOrNull { it.type.contains("BANNER", ignoreCase = true) || it.title.contains("Banner", ignoreCase = true) }
        val filteredBannerItems = bannerSec?.items?.filter { b ->
            b.id.isNotBlank() && b.id != "0" &&
            !b.name.contains("Diwali", ignoreCase = true) &&
            !b.name.contains("Offer", ignoreCase = true) &&
            !b.name.contains("Premium", ignoreCase = true) &&
            !b.name.contains("TV", ignoreCase = true) &&
            !b.name.contains("Join", ignoreCase = true)
        } ?: emptyList()

        if (filteredBannerItems.isNotEmpty()) {
            filteredBannerItems.take(5)
        } else if (categoryMovies.isNotEmpty()) {
            categoryMovies.take(5)
        } else if (validSections.isNotEmpty()) {
            validSections.first().items.take(5)
        } else {
            emptyList()
        }
    }

    if (selectedCatalogSection != null) {
        AllCatalogScreen(
            section = selectedCatalogSection,
            viewModel = viewModel,
            onBack = { selectedCatalogSection = null }
        )
    } else if (selectedOttPlatform != null) {
        val ottPlatform = selectedOttPlatform
        if (ottPlatform != null) {
            OttHubScreen(
                platform = ottPlatform,
                viewModel = viewModel,
                onBack = { selectedOttPlatform = null }
            )
        }
    } else if (viewModel.isDashboardLoading) {
        DashboardShimmerScreen()
    } else {
      Box(modifier = Modifier.fillMaxSize()) {
        // Immersive mode zeroes the status-bar inset, so fall back to the
        // display cutout height instead of the old hardcoded 64dp.
        val topInset = safeTopInset()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(top = topInset + 72.dp, bottom = 100.dp)
        ) {
            // Hero Slider/Banner Section (Full-bleed Cinematic Carousel)
            if (bannerMovies.isNotEmpty()) {
                item {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { bannerMovies.size })

                    LaunchedEffect(pagerState, bannerMovies) {
                        while (bannerMovies.isNotEmpty()) {
                            kotlinx.coroutines.delay(4500)
                            val nextPage = (pagerState.currentPage + 1) % bannerMovies.size
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(288.dp)   // h-72 ≈ 288dp per reference design
                    ) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val heroMovie = bannerMovies[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.selectSubject(heroMovie) }
                            ) {
                                val optimalHeroUrl = androidx.compose.runtime.remember(heroMovie.backdrop, heroMovie.poster) {
                                    val sourceUrl = heroMovie.backdrop?.ifBlank { null } ?: heroMovie.poster
                                    com.flixvora.util.ImageOptimizer.getOptimalUrl(sourceUrl, 1080)
                                }
                                val heroAmbient = androidx.compose.runtime.remember(heroMovie.averageHue) {
                                    com.flixvora.util.ImageOptimizer.parseAverageHue(heroMovie.averageHue)
                                }
                                val heroPlaceholder = androidx.compose.runtime.remember(heroMovie.blurHash, heroAmbient) {
                                    com.flixvora.util.ImageOptimizer.getBlurHashPainter(heroMovie.blurHash, 30, 20)
                                        ?: androidx.compose.ui.graphics.painter.ColorPainter(heroAmbient)
                                }

                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(optimalHeroUrl)
                                        .precision(coil.size.Precision.EXACT)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = heroMovie.name,
                                    placeholder = heroPlaceholder,
                                    error = heroPlaceholder,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xD90A0B0F),  // from-[#0A0B0F]/85
                                                    Color.Transparent,
                                                    Color(0x4D000000)   // to-black/30
                                                ),
                                                startY = 0f,
                                                endY = Float.POSITIVE_INFINITY
                                            )
                                        )
                                )
                                // TOP progress indicator bars (reference: absolute top-2.5 inset-x-3 flex gap-1)
                                if (bannerMovies.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(bannerMovies.size) { i ->
                                            val isPast = i < page
                                            val isCurrent = i == page
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.25f))
                                            ) {
                                                if (isPast || isCurrent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .fillMaxHeight()
                                                            .background(BrandGreen)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(com.flixvora.ui.components.BrandGradient)
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (heroMovie.isTvShow) "SERIES" else "MOVIE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = heroMovie.name,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        letterSpacing = (-0.5).sp,
                                        lineHeight = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!heroMovie.rating.isNullOrBlank() && heroMovie.rating != "Not available") {
                                            com.flixvora.ui.components.RatingBadge(rating = heroMovie.rating.orEmpty())
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        val heroMeta = listOfNotNull(
                                            heroMovie.year?.takeIf { it.isNotBlank() && it != "Not available" },
                                            heroMovie.genres.take(2).joinToString(", ")
                                                .takeIf { it.isNotBlank() }
                                        )
                                        Text(
                                            text = heroMeta.joinToString("  •  "),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Play button: BrandGreen bg, TextOnAccent text
                                        Button(
                                            onClick = { viewModel.playDirect(heroMovie) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = TextOnAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Play",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TextOnAccent
                                            )
                                        }
                                        // Info button: white/12% fill
                                        OutlinedButton(
                                            onClick = { viewModel.selectSubject(heroMovie) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White.copy(alpha = 0.12f),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Info",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Top progress indicator bars inside each pager page (see inner Box below)
                        // No external bottom dots needed — reference design uses bars INSIDE hero
                    }
                }
            }

            // 1b. Genre Filter Dock (reference: rounded-xl bg-white/[0.04] border border-white/[0.08])
            item {
                var selectedGenreFilter by remember { mutableStateOf("All") }
                val genres = listOf("All", "Action", "Comedy", "Romance", "Thriller", "Sci-Fi", "Horror", "Anime", "Drama", "Adventure")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    // Genre pill row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(genres) { genre ->
                            val active = selectedGenreFilter == genre
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (active) BrandGreen
                                        else Color(0xFF14161D).copy(alpha = 0.8f)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) Color.Transparent else BorderGlass,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { selectedGenreFilter = genre }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) TextOnAccent else Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Continue Watching rail (only when partially watched titles exist)
            if (continueWatching.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        com.flixvora.ui.components.MbSectionHeader(
                            title = "Continue Watching",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(continueWatching, key = { it.id }) { entry ->
                                ContinueWatchingCard(entry) {
                                    val subject = MovieSubject(
                                        id = entry.subjectId,
                                        name = entry.name,
                                        poster = entry.poster,
                                        isTvShow = entry.isTvShow
                                    )
                                    viewModel.selectSubject(subject)
                                    if (entry.isTvShow && entry.episode > 0) {
                                        viewModel.selectEpisode(
                                            com.flixvora.data.model.Episode(
                                                episodeId = entry.id,
                                                title = "Season ${entry.season} Episode ${entry.episode}",
                                                episodeNumber = entry.episode,
                                                seasonNumber = entry.season
                                            )
                                        )
                                    } else {
                                        viewModel.selectMovieDirect()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. OTT Platform Icons row
            item {
                OttPlatformRow(
                    platforms = TmdbOttService.allPlatforms,
                    onPlatformClick = { platform ->
                        selectedOttPlatform = platform
                    }
                )
            }

            // 3b. Discover quick links (official-API verticals)
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    com.flixvora.ui.components.SectionHeader(title = "Discover")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { com.flixvora.ui.components.PillChip("Top Charts", false) { onNavigate("rankings") } }
                        item { com.flixvora.ui.components.PillChip("Genres", false) { onNavigate("genres") } }
                        item { com.flixvora.ui.components.PillChip("Shorts", false) { onNavigate("shorts") } }
                        item { com.flixvora.ui.components.PillChip("Sports", false) { onNavigate("sports") } }
                        item { com.flixvora.ui.components.PillChip("Games", false) { onNavigate("games") } }
                    }
                }
            }

            // 3c. Top 10 chart rail (ranked numerals behind posters)
            if (trending.size >= 5) {
                item {
                    com.flixvora.ui.components.TopTenSection(
                        items = trending,
                        onClick = { viewModel.selectSubject(it) }
                    )
                }
            }

            // 3d. Watch Party rail
            if (partySessions.isNotEmpty()) {
                item {
                    com.flixvora.ui.components.WatchPartySection(
                        sessions = partySessions,
                        onClick = { session ->
                            val match = trending.firstOrNull { it.name == session.title }
                            if (match != null) viewModel.selectSubject(match)
                        }
                    )
                }
            }

            // 3e. Coming Soon rail (release schedule with booked counts)
            if (trending.size >= 3) {
                item {
                    com.flixvora.ui.components.ComingSoonSection(
                        items = trending.drop(3).ifEmpty { trending },
                        onClick = { viewModel.selectSubject(it) }
                    )
                }
            }

            // 3f. Anime shelves — the second catalogue is always on, so these
            // rails live in the same feed instead of behind a provider switch.
            if (animeRails.isNotEmpty()) {
                itemsIndexed(
                    animeRails,
                    key = { _, rail -> "anime_${rail.type}_${rail.title}" }
                ) { _, rail ->
                    val railItems = rail.items.distinctBy { it.id }
                    if (railItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                        ) {
                            com.flixvora.ui.components.MbSectionHeader(
                                title = rail.title,
                                subtitle = "${railItems.size} titles",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    railItems,
                                    key = { idx, movie -> "anime_${movie.id}_$idx" }
                                ) { _, movie ->
                                    com.flixvora.ui.components.MbPosterCard(movie = movie) {
                                        viewModel.selectSubject(movie)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Category filter chips (Immediately beneath OTT Row)
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val categories by viewModel.activeCategories.collectAsState()

                    LaunchedEffect(categories) {
                        if (categories.isNotEmpty() && categories.none { it.first == viewModel.selectedCategoryTab }) {
                            viewModel.loadCategoryList(categories.first().first)
                        }
                    }

                    // Hide category tab row when only 1 category exists
                    if (categories.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            items(categories) { cat ->
                                com.flixvora.ui.components.PillChip(
                                    text = cat.second,
                                    selected = viewModel.selectedCategoryTab == cat.first,
                                    onClick = { viewModel.loadCategoryList(cat.first) }
                                )
                            }
                        }
                    }
                }
            }

            // Category Content & Thematic Sections
            if (validSections.size > 1) {
                // Multi-section feed (e.g. Home with Trending, Latest, Popular, etc.): horizontal shelves
                validSections.forEachIndexed { index, section ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (index == 0) 4.dp else 20.dp)
                        ) {
                            if (index > 0) {
                                com.flixvora.ui.components.MbSectionHeader(
                                    title = section.title,
                                    subtitle = "${section.items.size} titles",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    onMore = { selectedCatalogSection = section }
                                )
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val uniqueSectionItems = section.items.distinctBy { it.id }
                                itemsIndexed(uniqueSectionItems, key = { itemIdx, movie -> "${movie.id}_$itemIdx" }) { _, movie ->
                                    com.flixvora.ui.components.MbPosterCard(movie = movie) {
                                        viewModel.selectSubject(movie)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val singleList = if (validSections.size == 1) validSections[0].items else categoryMovies
                if (singleList.isNotEmpty()) {
                    val chunked = singleList.chunked(3)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            rowItems.forEach { movie ->
                                Box(modifier = Modifier.weight(1f)) {
                                    com.flixvora.ui.components.MbPosterCard(
                                        movie = movie,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        viewModel.selectSubject(movie)
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = "Connection issue",
                                tint = TextSecondary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Unable to load feed",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Check your connection, then try again",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadDashboardContent() },
                                colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

            // Clearance so the last rail's captions are never clipped by the
            // tab bar; the bar itself now carries the system inset.
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Opaque header block: brand row then the demo's pill search field.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(SurfaceBase)
        ) {
            Spacer(Modifier.height(topInset))
            HomeTopBar(onSearchClick = onSearchClick)
            com.flixvora.ui.components.MbSearchBar(
                query = "",
                onClick = onSearchClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
      }
    }
}

@Composable
private fun HomeTopBar(onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    // Demo header: brand lockup left, profile affordance right. The search
    // field is its own row below, not crammed into this bar.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.flixvora.ui.components.BrandMark(size = 28.dp)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "Flixvora",
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                .bounceClick(onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Sleek OTT Platform Row placed directly below Hero Banner
 */
@Composable
fun OttPlatformRow(
    platforms: List<OttPlatform>,
    onPlatformClick: (OttPlatform) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(platforms, key = { it.id }) { platform ->
                Surface(
                    onClick = { onPlatformClick(platform) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(58.dp)
                        .glassSurface(RoundedCornerShape(18.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = platform.iconRes),
                            contentDescription = platform.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun AllCatalogScreen(
    section: CategorySection? = null,
    viewModel: MovieViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val displayList = remember(section) {
        section?.items?.distinctBy { it.id } ?: emptyList()
    }

    val screenTitle = section?.title?.ifBlank { "All Content" } ?: "All Content"

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = screenTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-Column Content Grid for the selected section
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text("No movies found", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = displayList,
                    key = { index, movie -> "${movie.id}_$index" }
                ) { _, movie ->
                    MovieGridItem(movie = movie, modifier = Modifier.fillMaxWidth()) {
                        viewModel.selectSubject(movie)
                    }
                }
            }
        }
    }
}




/**
 * Continue-watching tile. Reference flixvora-ui.zip:
 * w-44 (176dp) shrink-0 text-left rounded-xl overflow-hidden bg-[#14161D] border border-white/[0.08]
 * 16:9 backdrop + play circle overlay (black/65%, brand icon) + 4dp progress bar at bottom
 * Below: title 11sp bold white + "X% left" 10sp BrandGreen in a row
 */
@Composable
private fun ContinueWatchingCard(
    entry: com.flixvora.data.local.HistoryEntity,
    onClick: () -> Unit
) {
    val fraction = if (entry.totalTime > 0) {
        (entry.seeTime.toFloat() / entry.totalTime.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val pct = (fraction * 100).toInt()

    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161D))
            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // 16:9 image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = entry.poster,
                contentDescription = entry.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dark scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
            // Play circle (black/65% bg, BrandGreen icon border)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Resume",
                    tint = BrandGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
            // Bottom progress bar: white/15% bg, BrandGreen fill, 4dp height
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(BrandGreen)
                )
            }
        }
        // Bottom info row: title + percentage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$pct%",
                color = BrandGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

// formatContinueTime removed — ContinueWatchingCard now shows percentage
