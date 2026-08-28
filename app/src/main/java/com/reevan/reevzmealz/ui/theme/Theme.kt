package com.reevan.reevzmealz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.reevan.reevzmealz.data.ThemeMode

/**
 * Resolves [ThemeMode] against the system setting.
 *
 * Exposed so callers can honour an explicit Light/Dark choice while System still follows the OS.
 */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * The retro arcade theme: a fixed pink / yellow / blue palette, square corners and a monospace
 * face, so every screen reads as one game HUD.
 *
 * **Material You dynamic colour is deliberately off.** It used to be on, so the palette followed
 * the wallpaper — but the look is now defined by three specific colours, and a wallpaper-derived
 * scheme would repaint the app in whatever the phone's background happens to be. Dynamic colour
 * and a named palette cannot both be in charge; the named palette wins. There is no `dynamicColor`
 * flag any more, so this cannot be half-enabled by accident.
 */
@Composable
fun ReevzMealzTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (themeMode.isDark()) DarkScheme else LightScheme,
        shapes = ReevzShapes,
        typography = Typography,
        content = content,
    )
}
