package com.flixvora.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Flixvora v10.0 - UI Design System v2 Tokens
// Cinematic Dark Theme with Purple, Neon Pink & Cyber Accents
// ─────────────────────────────────────────────────────────────────────────────

// Brand Colors (flixvora-ui.zip Design System)
val FlixPurple = Color(0xFF1DD171)        // Emerald Neon Primary
val FlixPurpleDark = Color(0xFF15A859)
val FlixPurpleLight = Color(0xFF38E586)   // Emerald Bright
val FlixNeonPink = Color(0xFF89FF5B)      // Highlight
val FlixNeonBlue = Color(0xFF06B6D4)
val FlixNeonGreen = Color(0xFF1DD171)
val FlixOrange = Color(0xFFF59E0B)
val FlixRed = Color(0xFFEF4444)

// Surface & Dark Canvas (flixvora-ui.zip)
val BgDeep = Color(0xFF07080B)          // --bg-canvas
val BgDark = Color(0xFF07080B)          // --bg-canvas
val BgCard = Color(0xFF101218)          // --bg-surface
val BgCardHover = Color(0xFF171A23)     // --bg-elevated
val BgElevated = Color(0xFF171A23)      // --bg-elevated
val BgOverlay = Color(0xE607080B)       // Overlay scrim

// Borders & Glass
val BorderSubtle = Color(0x14FFFFFF)    // rgba(255,255,255,0.08)
val BorderBrand = Color(0xFF1DD171)
val BorderGlass = Color(0x14FFFFFF)     // 8% white glass border

// Core Canvas aliases
val ObsidianDark = BgDeep
val ObsidianCard = BgCard
val SurfaceBase = BgDeep
val SurfaceRaised = BgCard
val SurfaceOverlay = BgElevated
val SurfaceHover = BgCardHover
val OutlineSubtle = BorderSubtle
val OutlineStrong = Color(0xFF2E2E36)

// Accent ramp mapping (Flix Purple & Accents)
val BrandPurple = FlixPurple
val BrandPurpleDark = FlixPurpleDark
val BrandPurpleLight = FlixPurpleLight
val BrandGreen = FlixNeonGreen
val BrandGreenLight = Color(0xFF34D399)
val BrandGreenDark = Color(0xFF059669)
val BrandGreenInk = Color(0xFF06170E)
val BrandBlue = FlixNeonBlue

// Accent aliases (for compatibility across existing screens)
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

// Text ramp (flixvora-ui.zip)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF6B7280)
val TextDisabled = Color(0xFF334155)
val TextOnAccent = Color(0xFF051A0D)

// Semantic feedback
val SuccessGreen = FlixNeonGreen
val WarningAmber = FlixOrange
val ErrorRed = FlixRed

// Legacy compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
