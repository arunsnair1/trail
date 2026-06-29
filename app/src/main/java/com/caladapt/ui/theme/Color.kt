package com.caladapt.ui.theme

import androidx.compose.ui.graphics.Color

// ── FusionFit-inspired mobile palette ───────────────────────────────────────
// Adapted from the reference frontend's airy glassmorphism UI: warm off-white
// canvas, coral primary, mint secondary, sunny yellow accents, and soft ink.

// App background
val AppBackground       = Color(0xFFFFF7F4)
val AppBackgroundAlt    = Color(0xFFFFFBF8)

// Text
val TextMain            = Color(0xFF1F2029)
val TextSub             = Color(0xFF7B7D8D)

// Primary accents — token names preserved for existing app consumers
val AccentRed           = Color(0xFFFF6B81)
val AccentOrange        = Color(0xFFFFA94D)
val AccentTeal          = Color(0xFF5ED3C6)
val AccentBlue          = Color(0xFF8B7CFF)
val AccentYellow        = Color(0xFFFFD166)

// Semantic
val Success             = Color(0xFF2ECF8F)
val Error               = Color(0xFFFF4D6D)
val RingStart           = Color(0xFFE9E2DE)

// ── Glass panel constants ──────────────────────────────────────────────────
val GlassBg             = Color(0xBFFFFFFF)
val GlassBorderStart    = Color(0xD9FFFFFF)
val GlassBorderEnd      = Color(0x45FFFFFF)
val GlassInnerHighlight = Color(0x99FFFFFF)
val GlassBorder         = Color(0xB3FFFFFF)

// Hero / coloured card
val HeroGradientStart   = Color(0xFFFF6B81)
val HeroGradientMid     = Color(0xFFFFA94D)
val HeroGradientEnd     = Color(0xFF5ED3C6)
val HeroBorder          = Color(0xCCFFFFFF)

// ── Shadows ───────────────────────────────────────────────────────────────
val ShadowLow           = Color(0x1F55404A)
val ShadowHigh          = Color(0x2655404A)
val ShadowRed           = AccentRed.copy(alpha = 0.28f)

// ── Skeleton Loading ───────────────────────────────────────────────────────
val SkeletonBase        = Color(0xFFFFECE7)
val SkeletonHighlight   = Color(0xFFFFFFFF)

// ── Macro chart colours ────────────────────────────────────────────────────
val ChartProtein        = AccentOrange
val ChartCarbs          = AccentTeal
val ChartFat            = Color(0xFF9EA4B8)
val ChartCalories       = AccentRed

// Weight chart
val ChartWeightRaw      = Color(0xFFB8B2C4)
val ChartWeightEMA      = AccentTeal
val ChartTarget         = AccentOrange

// ── Badge / chip accents ───────────────────────────────────────────────────
val BadgeRedBg          = AccentRed.copy(alpha = 0.14f)
