package com.reevan.reevzmealz.ui.foods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.ui.common.FilterField
import com.reevan.reevzmealz.ui.theme.addBlockColors
import com.reevan.reevzmealz.util.foodSubtitle
import com.reevan.reevzmealz.util.knownPlaces

/** Which editor the sheet is showing, if any. */
private sealed interface Editor {
    object New : Editor
    data class Existing(val food: Food) : Editor
}

/**
 * Which foods the list is showing.
 *
 * [place] is null for [ALL], which is what lets the filter be one null check at the point of use
 * rather than a three-way branch.
 */
private enum class SourceFilter(val label: String, val place: MealPlace?) {
    ALL("All", null),
    HOME("Home", MealPlace.HOME),
    OUTSIDE("Outside", MealPlace.OUT),
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

    // These two are saveable: leaving the app and coming back should not silently widen the list
    // out to everything while the user is still reading a filtered view.
    var query by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf(SourceFilter.ALL) }

    val trimmed = query.trim()
    val visible = remember(state.foods, trimmed, source) {
        if (trimmed.isEmpty() && source == SourceFilter.ALL) {
            state.foods
        } else {
            state.foods.filter { food ->
                (trimmed.isEmpty() || food.name.contains(trimmed, ignoreCase = true)) &&
                    (source.place == null || food.source == source.place)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // No point offering filters over an empty list. Once shown they stay above the list
            // rather than scrolling away with it, so narrowing a long list is always one tap.
            if (state.foods.isNotEmpty()) {
                FoodsFilters(
                    query = query,
                    onQueryChange = { query = it },
                    source = source,
                    onSourceChange = { source = it },
                )
                HorizontalDivider()
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loaded && state.foods.isEmpty() -> EmptyFoods()

                    state.foods.isNotEmpty() && visible.isEmpty() ->
                        NoMatches(query = trimmed, source = source)

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Room for the FAB so the last row stays tappable.
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    ) {
                        items(
                            count = visible.size,
                            key = { index -> visible[index].id },
                        ) { index ->
                            // Between rows only, so the list reads as ruled rather than boxed in.
                            if (index > 0) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            val food = visible[index]
                            FoodRow(food = food, onClick = { editor = Editor.Existing(food) })
                        }
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
            Text("Add food")
        }
    }

    val currentEditor = editor
    if (currentEditor != null) {
        // Derived from the foods already in memory rather than its own query: the places *are*
        // the foods' places, so a second source of truth could only disagree with this list.
        val placesUsedBefore = remember(state.foods) {
            knownPlaces(state.foods.map { it.place })
        }
        FoodEditorSheet(
            food = (currentEditor as? Editor.Existing)?.food,
            knownPlaces = placesUsedBefore,
            onDismiss = { editor = null },
            onSave = { id, name, savedSource, pricePaise, place ->
                viewModel.save(id, name, savedSource, pricePaise, place)
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

/**
 * The search box above the Home / Outside filter.
 *
 * Stacked rather than side by side: the segmented row needs its three labels legible and the search
 * box needs room to type, and 394dp of phone will not give both on one line. Foods scrolls anyway,
 * unlike Today, so a two-line header costs nothing that matters here.
 */
@Composable
private fun FoodsFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    source: SourceFilter,
    onSourceChange: (SourceFilter) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        FilterField(
            value = query,
            onValueChange = onQueryChange,
            label = "Search foods",
            onDone = { focusManager.clearFocus() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        // The same control as Plan Meal's Week/Month and Money Spent's period switch, so that a
        // filter looks like a filter everywhere in the app.
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
        ) {
            SourceFilter.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = source == entry,
                    onClick = { onSourceChange(entry) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = SourceFilter.entries.size,
                    ),
                    label = { Text(entry.label) },
                )
            }
        }
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

/** Distinct from [EmptyFoods]: foods do exist, the filters just hid all of them. */
@Composable
private fun NoMatches(query: String, source: SourceFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                query.isNotEmpty() && source != SourceFilter.ALL ->
                    "No " + source.label.lowercase() + " food matches \"" + query + "\"."

                query.isNotEmpty() -> "Nothing matches \"" + query + "\"."

                else -> "No " + source.label.lowercase() + " food yet."
            },
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

        // Every row now ends in a tag: where an outside food came from, or simply Home. Without
        // the Home tag the homecooked rows ended in blank space while their neighbours did not.
        val place = food.place
        if (food.source == MealPlace.HOME) {
            HomeTag()
        } else if (!place.isNullOrBlank()) {
            PlaceChip(place = place)
        }
    }
}

/**
 * The green "Home" tag.
 *
 * Green because that is what was asked for, and it reuses [addBlockColors] rather than inventing a
 * fourth green. It carries a border where the blue place chip does not, and it needs one: the pale
 * green container is only 1.12:1 against the light-theme background, so without an edge the tag
 * would have no shape there at all. The border is 4.75:1, past the 3:1 a UI boundary needs.
 */
@Composable
private fun HomeTag() {
    val green = addBlockColors()
    Surface(
        color = green.container,
        contentColor = green.content,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, green.border),
        modifier = Modifier.padding(start = 12.dp),
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * Where an outside food was bought.
 *
 * Width-capped and ellipsised so a long shop name cannot squeeze the food's own name out of the
 * row. The same chip the food picker uses, so the same fact looks the same in both places.
 */
@Composable
private fun PlaceChip(place: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(start = 12.dp)
            .widthIn(max = 170.dp),
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
