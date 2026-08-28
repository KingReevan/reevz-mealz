package com.reevan.reevzmealz.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.util.foodSubtitle

/**
 * Pick a food from the Foods section to assign to [slotType].
 *
 * [foods] should already exclude what is in the slot, so nothing listed here is a no-op tap.
 *
 * The search box filters that list by name as you type. It is always shown rather than appearing
 * past some number of foods, so the sheet does not change shape as the list grows. Filtering is
 * done here rather than in a ViewModel because the list is already in memory and both callers
 * would otherwise need to carry the same query state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerSheet(
    slotType: MealType,
    foods: List<Food>,
    anyFoodsExist: Boolean,
    onDismiss: () -> Unit,
    onPick: (Food) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    // Reset every time the sheet opens: leaving composition discards this.
    var query by rememberSaveable { mutableStateOf("") }

    val visible = remember(foods, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            foods
        } else {
            foods.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Add to " + slotType.label.lowercase(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            // Only worth a search box once there is something to search through.
            if (anyFoodsExist) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search foods") },
                    singleLine = true,
                    // Deliberately not auto-focused: the keyboard would cover the list, and most
                    // picks are a single tap without searching at all.
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            TextButton(onClick = { query = "" }) {
                                Text(
                                    text = "✕",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            !anyFoodsExist ->
                                "No foods yet. Create them in the Foods section first."

                            query.isNotBlank() -> "Nothing matches \"" + query.trim() + "\"."

                            else -> "Everything you have is already in this slot."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(items = visible, key = { it.id }) { food ->
                        FoodPickerRow(food = food, onClick = { onPick(food) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodPickerRow(food: Food, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
    }
}
