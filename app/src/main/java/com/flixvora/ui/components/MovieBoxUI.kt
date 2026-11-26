package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.flixvora.data.model.MovieSubject
import com.flixvora.ui.theme.BrandBlue
import com.flixvora.ui.theme.BrandGreen
import com.flixvora.ui.theme.BrandGreenInk
import com.flixvora.ui.theme.OutlineSubtle
import com.flixvora.ui.theme.SurfaceOverlay
import com.flixvora.ui.theme.SurfaceRaised
import com.flixvora.ui.theme.TextPrimary
import com.flixvora.ui.theme.TextSecondary
import com.flixvora.ui.theme.TextTertiary
import com.flixvora.util.ImageOptimizer

/**
 * Poster card matching the MovieBox demo: 104x152 artwork window with a
 * hairline inset ring, a quality chip top-left, a green rating chip
 * bottom-right and an optional season badge top-right. Title and a
 * year-genre line sit underneath.
 */
@Composable
fun MbPosterCard(
    movie: MovieSubject,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 104.dp,
    showSeasonBadge: Boolean = movie.isTvShow,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(width)
            .bounceClick(onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width * 1.462f)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
        ) {
            val url = androidx.compose.runtime.remember(movie.poster) {
                ImageOptimizer.getOptimalUrl(movie.poster, 360)
            }
            val fallback = androidx.compose.runtime.remember(movie.averageHue) {
                ImageOptimizer.parseAverageHue(movie.averageHue)
            }
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .crossfade(false)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(fallback),
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(fallback),
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
            QualityChip(
                label = if (movie.isTvShow) "HD" else "4K",
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
            )
            if (showSeasonBadge && movie.totalSeasons > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(com.flixvora.ui.theme.FlixNeonPink)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "S${movie.totalSeasons}",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            val rating = movie.rating
            if (!rating.isNullOrBlank() && rating != "Not available") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ $rating",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.flixvora.ui.theme.FlixOrange
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = movie.name,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.1).sp
        )
        val sub = listOfNotNull(
            movie.year?.takeIf { it.isNotBlank() && it != "Not available" },
            movie.genres.take(3).joinToString(",").takeIf { it.isNotBlank() }
        ).joinToString(" \u00b7 ")
        if (sub.isNotBlank()) {
            Text(
                text = sub,
                fontSize = 8.5.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Small green-outlined quality marker used on poster corners. */
@Composable
fun QualityChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.5.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(1.dp, BrandGreen.copy(alpha = 0.45f), RoundedCornerShape(2.5.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * Continue-watching card: landscape 148x84 window, centred play affordance and
 * a green progress bar pinned to the bottom edge.
 */
@Composable
fun MbContinueCard(
    title: String,
    poster: String,
    progress: Float,
    remaining: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .bounceClick(onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
        ) {
            val url = androidx.compose.runtime.remember(poster) { ImageOptimizer.getOptimalUrl(poster, 320) }
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(BrandGreen)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = remaining, fontSize = 8.5.sp, color = TextTertiary)
    }
}

/**
 * Top-10 entry: an oversized outlined rank numeral bleeding into a 74x106
 * poster, with the title tucked underneath.
 */
@Composable
fun MbTop10Card(
    rank: Int,
    movie: MovieSubject,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() },
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = rank.toString(),
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Transparent,
            letterSpacing = (-2).sp,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp
            ).copy(
                // Outline numerals: transparent fill with a stroke reads as the
                // demo's -webkit-text-stroke treatment.
                drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(1.6f)
            ),
            modifier = Modifier.offset(x = 8.dp)
        )
        Column(modifier = Modifier.width(74.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 74.dp, height = 106.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceRaised)
            ) {
                val url = androidx.compose.runtime.remember(movie.poster) { ImageOptimizer.getOptimalUrl(movie.poster, 260) }
                AsyncImage(
                    model = url,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = movie.name,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Section header: a short accent rule anchors the title, and an optional
 * "More" affordance sits flush right. The rule replaces the boxed badge the
 * design used to carry - it marks hierarchy without adding another surface.
 */
@Composable
fun MbSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onMore: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandGreen)
            )
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.2).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (onMore != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onMore() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Pill search field. Reads as the demo's rounded grey input rather than a
 * Material text field, which brings its own padding and focus ring.
 */
@Composable
fun MbSearchBar(
    query: String,
    placeholder: String = "Search movies/ TV Shows",
    modifier: Modifier = Modifier,
    onQueryChange: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceOverlay)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        if (onQueryChange != null) {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    color = TextPrimary
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandGreen),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(text = placeholder, fontSize = 12.sp, color = TextTertiary)
                    }
                    inner()
                }
            )
        } else {
            Text(text = placeholder, fontSize = 12.sp, color = TextTertiary)
        }
    }
}

/** Genre filter pill — green fill when active, hairline outline when not. */
@Composable
fun MbGenreChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) BrandGreen else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color.Transparent else OutlineSubtle,
                RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BrandGreenInk else TextSecondary
        )
    }
}
