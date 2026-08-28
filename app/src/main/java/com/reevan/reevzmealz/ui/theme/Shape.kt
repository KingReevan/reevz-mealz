package com.reevan.reevzmealz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Square corners, because pixels are square.
 *
 * These were the roundest thing in the app — the previous look leaned on rounding for a friendly
 * feel. A retro game HUD is built from hard-edged blocks, so the same lever now points the other
 * way. Cards, dialogs, sheets, buttons, chips and text fields all read from these, which is why
 * setting them here re-shapes every screen at once.
 *
 * `extraLarge` keeps a 2dp corner: it is what the bottom sheet uses, and a completely square sheet
 * against a square screen edge loses the seam between them.
 */
val ReevzShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(2.dp),
)
