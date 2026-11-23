package com.flixvora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.PostItem
import com.flixvora.data.model.SportMatch
import com.flixvora.data.model.SportNewsItem
import com.flixvora.ui.components.MovieGridItem
import com.flixvora.ui.components.PillChip
import com.flixvora.ui.components.SectionHeader
import com.flixvora.ui.theme.OutlineSubtle
import com.flixvora.ui.theme.PlasmaGold
import com.flixvora.ui.theme.SurfaceOverlay
import com.flixvora.ui.theme.TextPrimary
import com.flixvora.ui.theme.TextSecondary
import com.flixvora.ui.theme.TextTertiary
import com.flixvora.ui.viewmodel.MovieViewModel

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(SurfaceOverlay)
                .border(1.dp, OutlineSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PlasmaGold)
    }
}

@Composable
private fun EmptyBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
fun RankingsScreen(viewModel: MovieViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.loadRankings() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Top Charts", onBack)
        if (viewModel.rankingCategories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.rankingCategories) { category ->
                    PillChip(
                        text = category.name,
                        selected = viewModel.rankingType == category.type
                    ) {
                        viewModel.loadRankings(category.type)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (viewModel.isLoadingRanking && viewModel.rankingList.isEmpty()) {
            LoadingBox()
        } else if (viewModel.rankingList.isEmpty()) {
            EmptyBox("No rankings available right now")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.rankingList) { movie ->
                    Column {
                        MovieGridItem(movie = movie, modifier = Modifier.fillMaxWidth(), showRating = true) {
                            viewModel.selectSubject(movie)
                        }
                    }
                }
            }
        }
    }
}

private val fallbackGenres = listOf(
    "Action", "Comedy", "Drama", "Thriller", "Romance", "Horror",
    "Sci-Fi", "Animation", "Crime", "Adventure", "Fantasy", "Family"
)

@Composable
fun GenreFilterScreen(viewModel: MovieViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        if (viewModel.genreFilterName == null) viewModel.loadGenreFilter(fallbackGenres.first())
    }

    val genres = viewModel.genreOptions
        .filter { it != "All" }
        .takeIf { it.isNotEmpty() } ?: fallbackGenres

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Browse by Genre", onBack)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                PillChip(text = genre, selected = viewModel.genreFilterName == genre) {
                    viewModel.loadGenreFilter(genre)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (viewModel.isLoadingGenreFilter && viewModel.genreFilterResults.isEmpty()) {
            LoadingBox()
        } else if (viewModel.genreFilterResults.isEmpty()) {
            EmptyBox("No titles for this genre")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.genreFilterResults) { movie ->
                    MovieGridItem(movie = movie, modifier = Modifier.fillMaxWidth(), showRating = true) {
                        viewModel.selectSubject(movie)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortCard(post: PostItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(SurfaceOverlay, RoundedCornerShape(14.dp))
    ) {
        val image = post.media?.cover?.url ?: post.cover?.url
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF33343A))
        ) {
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = post.content,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                post.content.ifBlank { post.title },
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val who = post.user?.nickname.orEmpty()
            val likes = post.stat?.likeCount ?: 0
            Text(
                listOfNotNull(
                    who.takeIf { it.isNotBlank() },
                    "$likes likes"
                ).joinToString(" • "),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShortsScreen(viewModel: MovieViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.loadShorts() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Shorts", onBack)
        if (viewModel.isLoadingShorts && viewModel.shortsFeed.isEmpty()) {
            LoadingBox()
        } else if (viewModel.shortsFeed.isEmpty()) {
            EmptyBox("No shorts available")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.shortsFeed) { post ->
                    ShortCard(post) {
                        post.subjectId?.takeIf { it != "0" }?.let { sid ->
                            viewModel.selectSubjectById(sid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(match: SportMatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(SurfaceOverlay, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = match.team1?.avatar,
            contentDescription = match.team1?.name,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF33343A), RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${match.team1?.name.orEmpty()} vs ${match.team2?.name.orEmpty()}",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    match.league.takeIf { it.isNotBlank() },
                    match.timeDesc.takeIf { it.isNotBlank() },
                    match.status.takeIf { it.isNotBlank() }
                ).joinToString(" • "),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AsyncImage(
            model = match.team2?.avatar,
            contentDescription = match.team2?.name,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF33343A), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun NewsRow(news: SportNewsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(SurfaceOverlay, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFF33343A), RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = news.cover?.url,
                contentDescription = news.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                news.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (news.summary.isNotBlank()) {
                Text(
                    news.summary,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SportsScreen(viewModel: MovieViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.loadSports() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Live Sports", onBack)
        if (viewModel.isLoadingSports && viewModel.sportMatches.isEmpty() && viewModel.sportNews.isEmpty()) {
            LoadingBox()
        } else if (viewModel.sportMatches.isEmpty() && viewModel.sportNews.isEmpty()) {
            EmptyBox("No live matches right now")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (viewModel.sportMatches.isNotEmpty()) {
                    item { SectionHeader("Matches") }
                    items(viewModel.sportMatches) { m ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            MatchRow(m) {}
                        }
                    }
                }
                if (viewModel.sportNews.isNotEmpty()) {
                    item { SectionHeader("Sports News") }
                    items(viewModel.sportNews) { n ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            NewsRow(n) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GamesScreen(viewModel: MovieViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { viewModel.loadGames() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("Game Center", onBack)
        if (viewModel.isLoadingGames && viewModel.gameCenterItems.isEmpty()) {
            LoadingBox()
        } else if (viewModel.gameCenterItems.isEmpty()) {
            EmptyBox("No games available")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.gameCenterItems) { game ->
                    Column(
                        modifier = Modifier
                            .clickable { viewModel.selectSubject(game) }
                            .background(SurfaceOverlay, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = game.poster,
                            contentDescription = game.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Text(
                            game.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
