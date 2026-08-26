package com.reevan.reevzmealz.ui.bought

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.util.formatMonthLabel
import com.reevan.reevzmealz.util.formatPaise
import com.reevan.reevzmealz.util.formatShortDate

/** Which editor the sheet is showing, if any. */
private sealed interface Editor {
    object New : Editor
    data class Existing(val item: BoughtItem) : Editor
}

/**
 * Purchase history grouped by month, newest first — a month heading carrying that month's total,
 * then one card per item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoughtItemsScreen(
    modifier: Modifier = Modifier,
    viewModel: BoughtItemsViewModel = viewModel(factory = BoughtItemsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    // Not rememberSaveable: these hold a BoughtItem, and after process death the row would need
    // re-fetching anyway. Dismissing on restore is the safe behaviour.
    var editor by remember { mutableStateOf<Editor?>(null) }
    var pendingDelete by remember { mutableStateOf<BoughtItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.loaded && state.months.isEmpty()) {
            EmptyBoughtItems()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Room for the FAB so the last card stays tappable.
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.months.forEach { month ->
                    stickyHeader(key = "month-" + month.monthKey) {
                        MonthHeader(
                            label = formatMonthLabel(month.anyMillisInMonth),
                            totalPaise = month.totalPaise,
                        )
                    }
                    items(items = month.items, key = { it.id }) { item ->
                        BoughtItemCard(
                            item = item,
                            onClick = { editor = Editor.Existing(item) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editor = Editor.New },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Text("Add item")
        }
    }

    val currentEditor = editor
    if (currentEditor != null) {
        BoughtItemEditorSheet(
            item = (currentEditor as? Editor.Existing)?.item,
            onDismiss = { editor = null },
            onSave = { id, name, pricePaise, boughtAt ->
                viewModel.save(id, name, pricePaise, boughtAt)
                editor = null
            },
            onDelete = { item ->
                editor = null
                pendingDelete = item
            },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete item?") },
            text = {
                Text("\"" + toDelete.name + "\" will be removed. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(toDelete)
                        pendingDelete = null
                    },
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Month name on the left, the month's spend on the right. Drawn on an opaque Surface because it
 * sticks to the top of the list and cards must not show through it.
 */
@Composable
private fun MonthHeader(label: String, totalPaise: Long) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPaise(totalPaise),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BoughtItemCard(item: BoughtItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ItemAvatar(name = item.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = formatShortDate(item.boughtAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatPaise(item.pricePaise.toLong()),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Circular initial, standing in for the icon a payments app would show. */
@Composable
private fun ItemAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        ) {
            Box(
                modifier = Modifier.height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyBoughtItems() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nothing bought yet.\nTap Add item to record a purchase.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
