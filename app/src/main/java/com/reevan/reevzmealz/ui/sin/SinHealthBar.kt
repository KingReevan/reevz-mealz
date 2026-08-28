package com.reevan.reevzmealz.ui.sin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.ui.theme.sinBarColor
import kotlin.math.ceil

/** Cells in the bar. The allowance is configurable, so the bar shows a fraction, not one cell per sin. */
private const val SEGMENTS = 10

/**
 * Sins remaining, drawn as a game health bar: a bordered strip of cells that empties as the month
 * is spent, pink while there is room, yellow when tight, red when nearly gone.
 *
 * Cells are a fixed count rather than one per sin. The allowance defaults to 40 and is
 * configurable, so a cell-per-sin bar would be unreadably fine at 40 and would change width
 * whenever the allowance changed — this stays the same size and always means "how much of the
 * month is left".
 *
 * A partly-used cell still counts as lit, so the last sin shows one cell rather than an empty bar;
 * an empty bar therefore means exactly zero, and says GAME OVER.
 */
@Composable
fun SinHealthBar(remaining: Int, allowance: Int, modifier: Modifier = Modifier) {
    val fraction = if (allowance <= 0) 0f else (remaining.toFloat() / allowance).coerceIn(0f, 1f)
    val lit = if (remaining <= 0) 0 else ceil(fraction * SEGMENTS).toInt().coerceIn(1, SEGMENTS)
    val barColor = sinBarColor(fraction)
    val dead = remaining <= 0

    Column(
        modifier = modifier.padding(end = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = if (dead) "GAME OVER" else "SINS $remaining/$allowance",
            style = MaterialTheme.typography.labelSmall,
            color = if (dead) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(SEGMENTS) { index ->
                Cell(lit = index < lit, litColor = barColor)
            }
        }
    }
}

/** One cell of the bar: a hard-edged block, either charged or an empty socket. */
@Composable
private fun Cell(lit: Boolean, litColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .size(width = 7.dp, height = 12.dp)
            .background(
                color = if (lit) litColor else MaterialTheme.colorScheme.outlineVariant,
                shape = RectangleShape,
            ),
    ) {}
}
