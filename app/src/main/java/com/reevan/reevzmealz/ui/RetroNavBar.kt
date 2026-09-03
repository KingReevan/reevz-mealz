package com.reevan.reevzmealz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The bottom navigation bar, built by hand rather than with Material's `NavigationBar`.
 *
 * Material draws a stadium-shaped active indicator sourced from its own component tokens, not from
 * `MaterialTheme.shapes` — so the app's 0dp corners never reached it, and a rounded pill sat in the
 * middle of an otherwise square arcade HUD. Here the selected tab is a **filled square cell**, the
 * cells are separated by hairlines, and a 2dp rule closes the bar off from the content above, which
 * is the same seam the meal slots use.
 *
 * [selected] is null while the pause menu is open, so no tab claims to be the current screen.
 */
@Composable
fun RetroNavBar(
    sections: List<AppSection>,
    selected: AppSection?,
    onSelect: (AppSection) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    // Gives the dividers between cells a height to fill.
                    .height(IntrinsicSize.Min),
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    NavCell(
                        section = section,
                        isSelected = section == selected,
                        onClick = { onSelect(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavCell(
    section: AppSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .heightIn(min = 62.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(section.icon),
            contentDescription = section.title,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = section.tabLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
