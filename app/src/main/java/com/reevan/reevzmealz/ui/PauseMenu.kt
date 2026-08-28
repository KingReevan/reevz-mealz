package com.reevan.reevzmealz.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The pause screen: where the sections you do not open every day live.
 *
 * Money Spent and Settings were the two least-used of six bottom-bar tabs, and six tabs left each
 * about 68dp on a phone. Moving them here takes the bar down to four at roughly 103dp each. They
 * are still ordinary sections — this only changes how they are reached.
 *
 * A pause menu rather than an overflow dropdown because the app is themed as a game, and because
 * the top-right of the header is already spoken for by the sin bar.
 */
@Composable
fun PauseMenu(
    onOpenSection: (AppSection) -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "== PAUSED ==",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AppSection.pauseMenuSections.forEach { section ->
            PauseMenuItem(
                label = section.title.uppercase(),
                icon = section.icon,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onOpenSection(section) },
            )
        }

        PauseMenuItem(
            label = "RESUME",
            icon = null,
            accent = MaterialTheme.colorScheme.secondary,
            onClick = onResume,
        )
    }
}

/** One chunky, hard-edged menu row with a ▶ cursor, the way a game menu marks its options. */
@Composable
private fun PauseMenuItem(
    label: String,
    icon: Int?,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, accent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accent,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
