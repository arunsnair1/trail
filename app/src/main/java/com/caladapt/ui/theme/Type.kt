package com.caladapt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

// ── Rajdhani from Google Fonts (downloadable) ──────────────────────────────

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = com.caladapt.R.array.com_google_android_gms_fonts_certs
)

private val rajdhaniFont = GoogleFont("Rajdhani")

val Rajdhani = FontFamily(
    Font(googleFont = rajdhaniFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = rajdhaniFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = rajdhaniFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = rajdhaniFont, fontProvider = fontProvider, weight = FontWeight.Bold),
)

// ── Global letter-spacing token ────────────────────────────────────────────

private val defaultLetterSpacing = 0.4.sp

// ── Typography scale ───────────────────────────────────────────────────────

val CalAdaptTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 58.sp,
        letterSpacing = defaultLetterSpacing
    ),
    displayMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 48.sp,
        letterSpacing = defaultLetterSpacing
    ),
    displaySmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = defaultLetterSpacing
    ),
    headlineLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = defaultLetterSpacing
    ),
    headlineMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = defaultLetterSpacing
    ),
    headlineSmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = defaultLetterSpacing
    ),
    titleLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = defaultLetterSpacing
    ),
    titleMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = defaultLetterSpacing
    ),
    titleSmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = defaultLetterSpacing
    ),
    bodyLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = defaultLetterSpacing
    ),
    bodyMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = defaultLetterSpacing
    ),
    bodySmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = defaultLetterSpacing
    ),
    labelLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp
    )
)
