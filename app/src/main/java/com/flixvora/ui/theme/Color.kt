package com.flixvora.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Flixvora v10.0 - UI Design System v2 Tokens
// Cinematic Dark Theme with Purple, Neon Pink & Cyber Accents
// ─────────────────────────────────────────────────────────────────────────────

// Brand Colors (Figma Design System v2)
val FlixPurple = Color(0xFF8B5CF6)
val FlixPurpleDark = Color(0xFF6D28D9)
val FlixPurpleLight = Color(0xFFA78BFA)
val FlixNeonPink = Color(0xFFEC4899)
val FlixNeonBlue = Color(0xFF06B6D4)
val FlixNeonGreen = Color(0xFF10B981)
val FlixOrange = Color(0xFFF59E0B)
val FlixRed = Color(0xFFEF4444)

// Surface & Dark Canvas
val BgDeep = Color(0xFF050507)          // Pure deep background
val BgDark = Color(0xFF0A0A0C)          // Slightly lifted dark background
val BgCard = Color(0xFF111114)          // Card surface
val BgCardHover = Color(0xFF18181D)     // Card hover/highlight
val BgElevated = Color(0xFF16161B)      // Elevated surface (modals/sheets)
val BgOverlay = Color(0xE6050507)       // Overlay scrim (90% deep)

// Borders & Glass
val BorderSubtle = Color(0xFF1F1F23)
val BorderBrand = Color(0xFF8B5CF6)
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

// Text ramp
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9CA3AF)
val TextTertiary = Color(0xFF6B7280)
val TextDisabled = Color(0xFF374151)
val TextOnAccent = Color(0xFFFFFFFF)

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
