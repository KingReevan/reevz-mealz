package com.reevan.reevzmealz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Monospace throughout, with wide letter spacing on headings — the typographic half of the arcade
 * look, where the palette is the other half.
 *
 * [FontFamily.Monospace] is the system's own fixed-width face, so this costs no asset and no
 * dependency. A true pixel face (Press Start 2P and friends) would need a `.ttf` dropped into
 * `res/font` and referenced here; that is an asset decision, and monospace already carries the
 * feel at every size without risking an unreadable 12sp label.
 *
 * Sizes run slightly smaller than the previous system-font scale because monospace glyphs are
 * wider — same visual weight, but "Bought Items" still fits beside the sin bar in the header.
 */
private val Mono = FontFamily.Monospace

val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.8.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.8.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
)
