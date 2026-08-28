package com.reevan.reevzmealz.ui.foods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.util.foodSubtitle

/** Which editor the sheet is showing, if any. */
private sealed interface Editor {
    object New : Editor
    data class Existing(val food: Food) : Editor
}

@Composable
fun FoodsScreen(
    modifier: Modifier = Modifier,
    viewModel: FoodsViewModel = viewModel(factory = FoodsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    // Not rememberSaveable: these hold a Food, and reopening the sheet after process death
    // would need the row re-fetched anyway. Dismissing on restore is the safe behaviour.
    var editor by remember { mutableStateOf<Editor?>(null) }
    var pendingDelete by remember { mutableStateOf<Food?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.loaded && state.foods.isEmpty()) {
            EmptyFoods()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Room for the FAB so the last row stays tappable.
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(items = state.foods, key = { it.id }) { food ->
                    FoodRow(food = food, onClick = { editor = Editor.Existing(food) })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editor = Editor.New },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Text("Add food")
        }
    }

    val currentEditor = editor
    if (currentEditor != null) {
        FoodEditorSheet(
            food = (currentEditor as? Editor.Existing)?.food,
            onDismiss = { editor = null },
            onSave = { id, name, source, pricePaise, place ->
                viewModel.save(id, name, source, pricePaise, place)
                editor = null
            },
            onDelete = { food ->
                editor = null
                pendingDelete = food
            },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete food?") },
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

@Composable
private fun EmptyFoods() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No foods yet.\nTap Add food to create your first building block.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = food.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = foodSubtitle(food.source, food.pricePaise),
                style = MaterialTheme.typography.bodySmall,
                color = if (food.source == MealPlace.OUT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        // Where it was bought, on the right. Width-capped and ellipsised so a long shop name
        // cannot squeeze the food's own name out of the row.
        val place = food.place
        if (!place.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .widthIn(max = 140.dp),
            ) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
