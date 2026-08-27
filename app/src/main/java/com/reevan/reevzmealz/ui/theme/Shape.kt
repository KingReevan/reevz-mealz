package com.reevan.reevzmealz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Noticeably rounder than the Material defaults (4/8/12/16/28dp).
 *
 * Rounding is the main lever for a friendly, un-severe feel that still leaves Material You's
 * dynamic colours in charge of the palette. Cards, dialogs, sheets, buttons and text fields all
 * read from these, so shaping them here reaches every screen at once.
 */
val ReevzShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
