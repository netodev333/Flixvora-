package com.flixvora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = AccentDeep,
    onPrimaryContainer = TextPrimary,
    secondary = AccentEmber,
    onSecondary = TextOnAccent,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentAmber,
    onTertiary = Color(0xFF06170E),
    background = SurfaceBase,
    onBackground = TextPrimary,
    surface = SurfaceRaised,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceOverlay,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceOverlay,
    surfaceContainerHighest = SurfaceHover,
    outline = OutlineSubtle,
    outlineVariant = OutlineStrong,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3A1B1B),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceBase
)

private val LightColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = AccentDeep,
    onPrimaryContainer = TextPrimary,
    secondary = AccentEmber,
    onSecondary = TextOnAccent,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentAmber,
    onTertiary = Color(0xFF06170E),
    background = SurfaceBase,
    onBackground = TextPrimary,
    surface = SurfaceRaised,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceOverlay,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceOverlay,
    surfaceContainerHighest = SurfaceHover,
    outline = OutlineSubtle,
    outlineVariant = OutlineStrong,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3A1B1B),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceBase
)

private val AppShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce our cinematic brand styling
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
