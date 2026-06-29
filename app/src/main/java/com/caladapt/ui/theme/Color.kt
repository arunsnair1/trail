package com.caladapt.ui.theme

import androidx.compose.ui.graphics.Color

// ── Design-System Palette — "Deep Emerald & Gold" ────────────────────────────
//
// A rich dark-mode palette built on near-black backgrounds with a curated
// emerald / amber / light blue accent family.
//
// NOTE: Token NAMES are intentionally kept from the original palette
// (AccentRed, AccentOrange …) so that every consumer file compiles
// without changes. Only the colour VALUES have been swapped.
// ────────────────────────────────────────────────────────────────────────────

// App background
val AppBackground       = Color(0xFF09090B)  // zinc-950 (very dark, almost black)
val AppBackgroundAlt    = Color(0xFF18181B)  // zinc-900

// Text
val TextMain            = Color(0xFFF4F4F5)  // zinc-100
val TextSub             = Color(0xFFA1A1AA)  // zinc-400

// Primary accents — names preserved, values updated
val AccentRed           = Color(0xFF10B981)  // emerald-500 (primary CTA)
val AccentOrange        = Color(0xFFF59E0B)  // amber-500 (secondary accent)
val AccentTeal          = Color(0xFF38BDF8)  // light-blue-400 (progress, trends)
val AccentBlue          = Color(0xFF8B5CF6)  // violet-500 (charts)
val AccentYellow        = Color(0xFFFCD34D)  // amber-300 (protein, warm glow)

// Semantic
val Success             = Color(0xFF34D399)  // emerald-400
val Error               = Color(0xFFF43F5E)  // rose-500
val RingStart           = Color(0xFF27272A)  // zinc-800 (muted ring track)

// ── Glass panel constants ──────────────────────────────────────────────────

val GlassBg             = Color(0xFA18181B)  // rgba(24,24,27,0.98) — less transparent frosted glass
val GlassBorderStart    = Color(0x26FFFFFF)  // rgba(255,255,255,0.15) — subtle top-left shimmer
val GlassBorderEnd      = Color(0x0DFFFFFF)  // rgba(255,255,255,0.05) — near-invisible bottom-right
val GlassInnerHighlight = Color(0x14FFFFFF)  // rgba(255,255,255,0.08) — faint specular
val GlassBorder         = Color(0x1FFFFFFF)  // rgba(255,255,255,0.12) — thin luminous edge

// Hero / coloured card
val HeroGradientStart   = Color(0xFFA78BFA)  // violet-400
val HeroGradientMid     = Color(0xFF818CF8)  // indigo-400
val HeroGradientEnd     = Color(0xFF6366F1)  // indigo-500
val HeroBorder          = Color(0x40FFFFFF)  // rgba(255,255,255,0.25)

// ── Shadows ───────────────────────────────────────────────────────────────
val ShadowLow           = Color.Black.copy(alpha = 0.30f)
val ShadowHigh          = Color.Black.copy(alpha = 0.50f)
val ShadowRed           = AccentRed.copy(alpha = 0.30f)  // violet glow

// ── Skeleton Loading ───────────────────────────────────────────────────────
val SkeletonBase        = Color(0xFF1E293B)   // slate-800
val SkeletonHighlight   = Color(0xFF334155)   // slate-700

// ── Macro chart colours ────────────────────────────────────────────────────

val ChartProtein        = AccentOrange         // amber-400 (#FBBF24)
val ChartCarbs          = AccentTeal           // cyan-400 (#22D3EE)
val ChartFat            = Color(0xFF94A3B8)    // slate-400 — visible on dark
val ChartCalories       = AccentRed            // violet-400

// Weight chart
val ChartWeightRaw      = Color(0xFF64748B)    // slate-500
val ChartWeightEMA      = AccentTeal           // cyan trend line
val ChartTarget         = AccentOrange         // amber target line

// ── Badge / chip accents ───────────────────────────────────────────────────

val BadgeRedBg          = AccentRed.copy(alpha = 0.15f)  // violet @ 15%
