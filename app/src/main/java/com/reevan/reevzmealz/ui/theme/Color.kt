package com.reevan.reevzmealz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/*
 * The retro arcade palette: hot pink, arcade yellow and electric blue on a deep night background.
 *
 * These are fixed values rather than Material You's wallpaper-derived colours, because the whole
 * point of the look is that it is *these three* colours. See ReevzMealzTheme for why dynamic
 * colour is off.
 */

// Pink — the lead colour. Slot names, buttons, a healthy sin bar.
val RetroPink = Color(0xFFFF5FA2)
val RetroPinkDeep = Color(0xFFC4105F)
val RetroPinkContainerDark = Color(0xFF7A1746)
val RetroPinkContainerLight = Color(0xFFFFC9E0)

// Blue — the second voice. Chips, selected states, the picker.
val RetroBlue = Color(0xFF43B4FF)
val RetroBlueDeep = Color(0xFF0A5FA5)
val RetroBlueContainerDark = Color(0xFF114A73)
val RetroBlueContainerLight = Color(0xFFC4E3FF)

// Yellow — the accent, and the warning band of the sin bar.
val RetroYellow = Color(0xFFFFD028)
val RetroYellowDeep = Color(0xFF8A6A00)
val RetroYellowContainerDark = Color(0xFF6B5400)
val RetroYellowContainerLight = Color(0xFFFFE188)

// Night background, in the blue-violet an arcade cabinet glows against.
private val NightBackground = Color(0xFF16132E)
private val NightSurface = Color(0xFF1E1A3C)
private val NightPanel = Color(0xFF2A2450)
private val NightOutline = Color(0xFF8478C4)
private val NightOutlineDim = Color(0xFF453D74)
private val NightText = Color(0xFFEFEAFF)
private val NightTextDim = Color(0xFFC3BBEA)

// Daylight: cream paper with a pink panel, so the same three colours still lead.
private val DayBackground = Color(0xFFFFF7E6)
private val DaySurface = Color(0xFFFFFDF7)
private val DayPanel = Color(0xFFFFE6F2)
private val DayOutline = Color(0xFF7A6E9A)
private val DayOutlineDim = Color(0xFFD8C7E2)
private val DayText = Color(0xFF1E1A3C)
private val DayTextDim = Color(0xFF5A4A66)

private val RetroRed = Color(0xFFFF5F5F)
private val RetroRedDeep = Color(0xFFC4001D)

internal val DarkScheme = darkRetroScheme()
internal val LightScheme = lightRetroScheme()

private fun darkRetroScheme() = androidx.compose.material3.darkColorScheme(
    primary = RetroPink,
    onPrimary = Color(0xFF1A0A16),
    primaryContainer = RetroPinkContainerDark,
    onPrimaryContainer = Color(0xFFFFD3E6),
    secondary = RetroBlue,
    onSecondary = Color(0xFF04121F),
    secondaryContainer = RetroBlueContainerDark,
    onSecondaryContainer = Color(0xFFC7E9FF),
    tertiary = RetroYellow,
    onTertiary = Color(0xFF241C00),
    tertiaryContainer = RetroYellowContainerDark,
    onTertiaryContainer = Color(0xFFFFE896),
    error = RetroRed,
    onError = Color(0xFF26060B),
    errorContainer = Color(0xFF7A1420),
    onErrorContainer = Color(0xFFFFD6DA),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightPanel,
    onSurfaceVariant = NightTextDim,
    outline = NightOutline,
    outlineVariant = NightOutlineDim,
)

private fun lightRetroScheme() = androidx.compose.material3.lightColorScheme(
    primary = RetroPinkDeep,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = RetroPinkContainerLight,
    onPrimaryContainer = Color(0xFF40001C),
    secondary = RetroBlueDeep,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = RetroBlueContainerLight,
    onSecondaryContainer = Color(0xFF002B4D),
    tertiary = RetroYellowDeep,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = RetroYellowContainerLight,
    onTertiaryContainer = Color(0xFF2A2000),
    error = RetroRedDeep,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410005),
    background = DayBackground,
    onBackground = DayText,
    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DayPanel,
    onSurfaceVariant = DayTextDim,
    outline = DayOutline,
    outlineVariant = DayOutlineDim,
)

private val GoodGreenLight = Color(0xFF1B7F3B)
private val GoodGreenDark = Color(0xFF5DFF9F)

/**
 * Green for "followed the plan".
 *
 * Material 3 has no success role, so this one pair is chosen by theme here rather than pulled from
 * `colorScheme`. It reads the *scheme's* own background rather than `isSystemInDarkTheme()`, so it
 * follows an explicit Light/Dark choice in Settings instead of whatever the OS is set to.
 */
@Composable
fun goodColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GoodGreenDark else GoodGreenLight

/** Container, glyph and border for the green "add" block. */
data class AddBlockColors(val container: Color, val content: Color, val border: Color)

/**
 * The green "+" block that adds a food to a slot.
 *
 * Material 3 has no green role, so this is a hand-picked set, chosen the same way [goodColor] is —
 * by the scheme's own background luminance, so it follows an explicit Light/Dark choice in Settings
 * rather than the OS. The border does the work of separating the block from the slot panel: a pale
 * green on the pale pink light-theme panel is only 1.02:1 against it, so without the border the
 * block would have no edge at all.
 */
@Composable
fun addBlockColors(): AddBlockColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        AddBlockColors(
            container = Color(0xFF14532D),
            content = Color(0xFF7BFFB0),
            border = Color(0xFF3DDC84),
        )
    } else {
        AddBlockColors(
            container = Color(0xFFC9F5DA),
            content = Color(0xFF14532D),
            border = Color(0xFF1B7F3B),
        )
    }

/**
 * The sin bar's colour at a given fraction of the allowance remaining.
 *
 * Reads like a health bar: pink while there is room, yellow once it is getting tight, red when
 * nearly spent. The thresholds are deliberately generous — the point is to notice before failing.
 */
@Composable
fun sinBarColor(fractionRemaining: Float): Color = when {
    fractionRemaining > 0.5f -> MaterialTheme.colorScheme.primary
    fractionRemaining > 0.2f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
