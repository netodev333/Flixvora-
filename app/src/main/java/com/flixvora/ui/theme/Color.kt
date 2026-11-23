package com.flixvora.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// MovieBox palette (mirrors dist/flixvora/ui-demo/index.html)
//
// Near-black neutral canvas, one vivid green accent, flat elevated surfaces.
// Deliberately restrained: the demo gets depth from poster art and hairlines,
// not from gradients or glow, so the tokens here stay flat.
// ─────────────────────────────────────────────────────────────────────────────

// Accent ramp — one green, three stops for pressed/disabled states.
val BrandGreen = Color(0xFF1DD171)        // primary accent
val BrandGreenLight = Color(0xFF2FF58B)   // hover / highlight
val BrandGreenDark = Color(0xFF07B84E)    // pressed
val BrandGreenInk = Color(0xFF06170E)     // text on green fills
val BrandBlue = Color(0xFF2166E5)         // quality / info badge

// Core canvas + surfaces
val ObsidianDark = Color(0xFF101114)      // app background
val ObsidianCard = Color(0xFF1A1B1F)      // legacy card token
val SurfaceBase = Color(0xFF101114)       // level 0
val SurfaceRaised = Color(0xFF1A1B1F)     // level 1 — cards, sheets
val SurfaceOverlay = Color(0xFF28292E)    // level 2 — chips, inputs
val SurfaceHover = Color(0xFF33343A)      // level 3 — pressed / focused
val OutlineSubtle = Color(0xFF26272C)     // hairline borders
val OutlineStrong = Color(0xFF3A3B42)

// Legacy accent aliases kept so existing call sites compile unchanged.
val PlasmaRed = BrandGreen
val PlasmaEmber = BrandGreenLight
val PlasmaGold = BrandGreen
val PlasmaViolet = BrandBlue
val PlasmaCyan = BrandGreenLight

val CinematicRed = BrandGreen
val AccentPrimary = BrandGreen
val AccentDeep = BrandGreenDark
val AccentGlow = BrandGreenLight
val AccentEmber = BrandGreenLight
val AccentGold = BrandGreen
val CrimsonRed = BrandGreenDark
val AccentAmber = BrandGreen

// Text ramp
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA9ABB3)
val TextTertiary = Color(0xFF6E7078)
val TextDisabled = Color(0xFF4A4C52)
val TextOnAccent = Color(0xFF06170E)

// Semantic feedback
val SuccessGreen = Color(0xFF1DD171)
val WarningAmber = Color(0xFFF5A524)
val ErrorRed = Color(0xFFEF4444)

// Legacy compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
