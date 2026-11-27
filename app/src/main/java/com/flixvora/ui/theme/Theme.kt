package com.flixvora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAccentTheme = staticCompositionLocalOf { FlixvoraAccentTheme.EMERALD_NEON }

val FlixvoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlixvoraTheme(
    accentTheme: FlixvoraAccentTheme = FlixvoraAccentTheme.EMERALD_NEON,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accentTheme.primary,
        onPrimary = accentTheme.onPrimary,
        primaryContainer = accentTheme.primary.copy(alpha = 0.16f),
        onPrimaryContainer = accentTheme.bright,
        secondary = BgElevated,
        onSecondary = TextPrimary,
        background = accentTheme.canvas,
        onBackground = TextPrimary,
        surface = accentTheme.surface,
        onSurface = TextPrimary,
        surfaceVariant = BgElevated,
        onSurfaceVariant = TextSecondary,
        outline = BorderHairline,
        error = LiveCrimson,
        onError = TextPrimary
    )

    CompositionLocalProvider(LocalAccentTheme provides accentTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography = FlixvoraTypography,
            shapes = FlixvoraShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    FlixvoraTheme(content = content)
}
