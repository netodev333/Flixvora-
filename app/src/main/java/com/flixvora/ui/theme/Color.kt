package com.flixvora.ui.theme

import androidx.compose.ui.graphics.Color

// 60% Pitch Cinema Canvas
val BgObsidian = Color(0xFF0A0B0F)
val BgSurface = Color(0xFF14161D)
val BgElevated = Color(0xFF1C1F2A)
val BgGlassScrim = Color(0xE60A0B0F)

// 30% Hairlines & Dividers
val BorderHairline = Color(0x14FFFFFF)
val BorderSubtle = Color(0xFF252834)

// Typography Ramp
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9CA3AF)
val TextTertiary = Color(0xFF6B7280)

// Semantic Feedback
val RatingGold = Color(0xFFFBBF24)
val LiveCrimson = Color(0xFFEF4444)

// Multi-Theme Accent Palettes (No brand names)
enum class FlixvoraAccentTheme(
    val id: String,
    val label: String,
    val canvas: Color,
    val surface: Color,
    val primary: Color,
    val bright: Color,
    val onPrimary: Color,
) {
    EMERALD_NEON(
        id = "emerald-neon",
        label = "Emerald Neon",
        canvas = Color(0xFF0A0B0F),
        surface = Color(0xFF14161D),
        primary = Color(0xFF1DD171),
        bright = Color(0xFF89FF5B),
        onPrimary = Color(0xFF051A0D),
    ),
    CRIMSON_BLAZE(
        id = "crimson-blaze",
        label = "Crimson Blaze",
        canvas = Color(0xFF0B090C),
        surface = Color(0xFF171319),
        primary = Color(0xFFE11D48),
        bright = Color(0xFFFB7185),
        onPrimary = Color(0xFFFFFFFF),
    ),
    OCEAN_CYAN(
        id = "ocean-cyan",
        label = "Ocean Cyan",
        canvas = Color(0xFF070C12),
        surface = Color(0xFF111924),
        primary = Color(0xFF06B6D4),
        bright = Color(0xFF67E8F9),
        onPrimary = Color(0xFF042029),
    ),
    SUNSET_AMBER(
        id = "sunset-amber",
        label = "Sunset Amber",
        canvas = Color(0xFF0E0B08),
        surface = Color(0xFF1B1611),
        primary = Color(0xFFF59E0B),
        bright = Color(0xFFFCD34D),
        onPrimary = Color(0xFF1F1202),
    ),
    ROYAL_VIOLET(
        id = "royal-violet",
        label = "Royal Violet",
        canvas = Color(0xFF0B0912),
        surface = Color(0xFF161324),
        primary = Color(0xFF8B5CF6),
        bright = Color(0xFFC4B5FD),
        onPrimary = Color(0xFFFFFFFF),
    ),
    ROSE_QUARTZ(
        id = "rose-quartz",
        label = "Rose Quartz",
        canvas = Color(0xFF0E090D),
        surface = Color(0xFF1B1219),
        primary = Color(0xFFEC4899),
        bright = Color(0xFFF9A8D4),
        onPrimary = Color(0xFFFFFFFF),
    ),
    GOLD_LUXE(
        id = "gold-luxe",
        label = "Gold Luxe",
        canvas = Color(0xFF0B0A08),
        surface = Color(0xFF171511),
        primary = Color(0xFFEAB308),
        bright = Color(0xFFFDE047),
        onPrimary = Color(0xFF1C1602),
    ),
    ARCTIC_SLATE(
        id = "arctic-slate",
        label = "Arctic Slate",
        canvas = Color(0xFF090C10),
        surface = Color(0xFF141922),
        primary = Color(0xFF38BDF8),
        bright = Color(0xFFBAE6FD),
        onPrimary = Color(0xFF041C2C),
    ),
}

// ─────────────────────────────────────────────────────────────────────────────
// Compatibility Aliases for legacy screens
// ─────────────────────────────────────────────────────────────────────────────
val FlixPurple = Color(0xFF1DD171)
val FlixPurpleDark = Color(0xFF15A859)
val FlixPurpleLight = Color(0xFF38E586)
val FlixNeonPink = Color(0xFF89FF5B)
val FlixNeonBlue = Color(0xFF06B6D4)
val FlixNeonGreen = Color(0xFF1DD171)
val FlixOrange = Color(0xFFF59E0B)
val FlixRed = Color(0xFFEF4444)

val BgDeep = Color(0xFF07080B)
val BgDark = Color(0xFF07080B)
val BgCard = Color(0xFF101218)
val BgCardHover = Color(0xFF171A23)
val BgOverlay = Color(0xE607080B)

val BorderBrand = Color(0xFF1DD171)
val BorderGlass = Color(0x14FFFFFF)

val ObsidianDark = BgObsidian
val ObsidianCard = BgSurface
val SurfaceBase = BgObsidian
val SurfaceRaised = BgSurface
val SurfaceOverlay = BgElevated
val SurfaceHover = BgElevated
val OutlineSubtle = BorderHairline
val OutlineStrong = Color(0xFF2E2E36)

val BrandPurple = FlixPurple
val BrandPurpleDark = FlixPurpleDark
val BrandPurpleLight = FlixPurpleLight
val BrandGreen = Color(0xFF1DD171)
val BrandGreenLight = Color(0xFF34D399)
val BrandGreenDark = Color(0xFF059669)
val BrandGreenInk = Color(0xFF06170E)
val BrandBlue = FlixNeonBlue

val PlasmaRed = FlixPurple
val PlasmaEmber = FlixNeonPink
val PlasmaGold = FlixOrange
val PlasmaViolet = FlixPurple
val PlasmaCyan = FlixNeonBlue

val CinematicRed = FlixPurple
val AccentPrimary = FlixPurple
val AccentDeep = FlixPurpleDark
val AccentGlow = FlixPurpleLight
val AccentEmber = FlixNeonPink
val AccentGold = FlixOrange
val CrimsonRed = FlixRed
val AccentAmber = FlixOrange

val TextDisabled = Color(0xFF334155)
val TextOnAccent = Color(0xFF051A0D)

val SuccessGreen = FlixNeonGreen
val WarningAmber = FlixOrange
val ErrorRed = FlixRed

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
