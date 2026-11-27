package com.flixvora.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flixvora.data.model.MovieSubject
import com.flixvora.ui.theme.AccentAmber
import com.flixvora.ui.theme.BrandBlue
import com.flixvora.ui.theme.BrandGreen
import com.flixvora.ui.theme.AccentEmber
import com.flixvora.ui.theme.AccentPrimary
import com.flixvora.ui.theme.ObsidianDark
import com.flixvora.ui.theme.OutlineSubtle
import com.flixvora.ui.theme.PlasmaCyan
import com.flixvora.ui.theme.PlasmaEmber
import com.flixvora.ui.theme.PlasmaGold
import com.flixvora.ui.theme.PlasmaRed
import com.flixvora.ui.theme.PlasmaViolet
import com.flixvora.ui.theme.SurfaceBase
import com.flixvora.ui.theme.SurfaceOverlay
import com.flixvora.ui.theme.SurfaceRaised
import com.flixvora.ui.theme.TextOnAccent
import com.flixvora.ui.theme.TextPrimary
import com.flixvora.ui.theme.TextSecondary
import com.flixvora.ui.theme.TextTertiary

/** Brand accent ramp (emerald → bright emerald) matching flixvora-ui.zip design tokens. */
val BrandGradient: Brush = Brush.horizontalGradient(
    listOf(Color(0xFF1DD171), Color(0xFF38E586))
)

/** Diagonal variant for badges and hero flourishes. */
val BrandGradientDiagonal: Brush = Brush.linearGradient(
    listOf(Color(0xFF1DD171), Color(0xFF38E586))
)

/** Violet → cyan counter-gradient used for secondary highlights. */
val CoolGradient: Brush = Brush.horizontalGradient(
    listOf(Color(0xFF06B6D4), Color(0xFF38E586))
)

/** The BOUNCE spring: a touch of overshoot so surfaces feel alive. */
val BounceSpring = androidx.compose.animation.core.spring<Float>(
    dampingRatio = 0.42f,
    stiffness = 620f
)

/** Fills a shape with the brand accent ramp. */
fun Modifier.brandGradient(): Modifier = background(BrandGradient)

/**
 * Conic brand mark: a plasma ring (red → ember → gold → pink → violet → cyan)
 * around a dark inset tile carrying the play glyph. Mirrors the launcher icon
 * so the app chrome and the icon read as one identity.
 */
@Composable
fun BrandMark(size: Dp = 32.dp, cornerRadius: Dp = size * 0.3f) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.sweepGradient(
                    listOf(PlasmaRed, PlasmaEmber, PlasmaGold, Color(0xFF1DD171), PlasmaViolet, PlasmaCyan, PlasmaRed)
                )
            )
            .padding(size * 0.06f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius - size * 0.05f))
                .background(Brush.verticalGradient(listOf(Color(0xFF15161F), Color(0xFF101114)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                contentDescription = "Flixvora",
                tint = Color.White,
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}

/**
 * Solid canvas background using the deep dark surface (#07080B).
 * No ambient glow blooms — the emerald theme uses clean darkness.
 */
@Composable
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

/** Translucent frosted panel with a hairline border — the BOUNCE glass layer. */
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

/** Pill-shaped glass chip used for category filters. */
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

/**
 * Elevated card surface with a hairline border and a faint top highlight, so
 * containers read as lit panels instead of flat rectangles on the dark canvas.
 */
fun Modifier.cinematicSurface(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(listOf(SurfaceOverlay, SurfaceRaised))
    )
    .border(1.dp, OutlineSubtle, shape)


fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                SurfaceOverlay,
                androidx.compose.ui.graphics.Color(0xFF28292E),
                SurfaceOverlay,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun DashboardShimmerScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Shimmer Hero Banner Placeholder
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .shimmerEffect()
            ) {
                // Bottom shading gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianDark)
                            )
                        )
                )

                // Text lines placeholders
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Shimmer Category Row Placeholders (3 Rows)
        items(3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // Category Title placeholder
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .width(140.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scroll of card placeholders
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieGridItem(
    movie: MovieSubject,
    modifier: Modifier = Modifier.width(110.dp),
    showRating: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceOverlay)
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        ) {
            val optimalPosterUrl = androidx.compose.runtime.remember(movie.poster) {
                com.flixvora.util.ImageOptimizer.getOptimalUrl(movie.poster, 360)
            }
            val placeholderColor = androidx.compose.runtime.remember(movie.averageHue) {
                com.flixvora.util.ImageOptimizer.parseAverageHue(movie.averageHue)
            }
            val placeholderPainter = androidx.compose.runtime.remember(movie.blurHash, placeholderColor) {
                com.flixvora.util.ImageOptimizer.getBlurHashPainter(movie.blurHash, 20, 20)
                    ?: androidx.compose.ui.graphics.painter.ColorPainter(placeholderColor)
            }

            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(optimalPosterUrl)
                    .memoryCacheKey(optimalPosterUrl)
                    .diskCacheKey(optimalPosterUrl)
                    .size(width = 330, height = 480)
                    .precision(coil.size.Precision.INEXACT)
                    .crossfade(false)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom scrim keeps the badge legible on bright posters.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            if (showRating && !movie.rating.isNullOrBlank() && movie.rating != "Not available") {
                RatingBadge(
                    rating = movie.rating.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.name,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            letterSpacing = (-0.1).sp
        )
        if (!movie.year.isNullOrBlank() && movie.year != "Not available") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = movie.year,
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }
    }
}

/** Small pill used to surface a title's rating over poster artwork. */
@Composable
fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = rating,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}

/** Section header with an optional trailing action, used across shelf layouts. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandGradient)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPrimary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** Subtle scale-down on press for a tactile, native feel without layout shift. */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = BounceSpring,
        label = "press_scale"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * BOUNCE entrance: content springs up into place on first composition, with a
 * small overshoot. `index` staggers a list so cards cascade instead of popping
 * in all at once.
 */
fun Modifier.springIn(index: Int = 0, from: Float = 34f): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(12) * 55).toLong())
        visible = true
    }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.55f,
            stiffness = 420f
        ),
        label = "spring_in"
    )
    graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * from * density
        val s = 0.86f + 0.14f * progress
        scaleX = s
        scaleY = s
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Minimum top inset while the system bars are hidden.
 *
 * Immersive mode reports a zero status-bar inset, which would let titles slide
 * under a camera cutout. A transparent window still reports the display cutout,
 * so that value is used when it is the larger of the two. The floor keeps the
 * layout stable on devices with no notch at all.
 */
@Composable
fun safeTopInset(): Dp {
    val cutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val notch = if (cutout > status) cutout else status
    return if (notch > 12.dp) notch else 12.dp
}

/** Bottom counterpart of [safeTopInset], for the gesture/nav area. */
@Composable
fun safeBottomInset(): Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gesture = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()
    val bar = if (nav > gesture) nav else gesture
    // Three-button navigation reports a real height; gesture navigation reports
    // only the thin swipe strip, so keep a small floor for thumb reach.
    return if (bar > 8.dp) bar else 8.dp
}

/**
 * Shows or hides both system bars for a window. Replaces the deprecated
 * `systemUiVisibility` flags, which silently stopped taking effect on newer
 * Android versions and left the bars stuck in whatever state they were in.
 */
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
    controller.isAppearanceLightStatusBars = false
    controller.isAppearanceLightNavigationBars = false
}
