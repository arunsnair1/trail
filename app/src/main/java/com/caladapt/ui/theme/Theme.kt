package com.caladapt.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GlassDarkScheme = darkColorScheme(
    primary            = AccentRed,           // violet-400
    onPrimary          = Color.White,
    primaryContainer   = AccentRed.copy(alpha = 0.15f),
    onPrimaryContainer = AccentRed,
    secondary          = AccentOrange,        // amber-400
    onSecondary        = Color.Black,
    secondaryContainer = AccentOrange.copy(alpha = 0.15f),
    onSecondaryContainer = AccentOrange,
    tertiary           = AccentTeal,          // cyan-400
    onTertiary         = Color.Black,
    tertiaryContainer  = AccentTeal.copy(alpha = 0.15f),
    onTertiaryContainer = AccentTeal,
    background         = AppBackground,
    onBackground       = TextMain,
    surface            = AppBackgroundAlt,
    onSurface          = TextMain,
    surfaceVariant     = Color.White.copy(alpha = 0.08f),
    onSurfaceVariant   = TextSub,
    error              = Error,
    onError            = Color.White,
    outline            = GlassBorder,
    outlineVariant     = GlassBorder,
)

@Composable
fun CalAdaptTheme(
    darkTheme: Boolean = true, // Force dark
    content: @Composable () -> Unit
) {
    val colorScheme = GlassDarkScheme

    // Dark status bar matching the deep navy background
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppBackground.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CalAdaptTypography,
        content = content
    )
}
