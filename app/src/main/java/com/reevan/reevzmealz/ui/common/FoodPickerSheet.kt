package com.reevan.reevzmealz.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.ui.theme.addBlockColors
import com.reevan.reevzmealz.util.foodSubtitle
import com.reevan.reevzmealz.util.knownPlaces
import com.reevan.reevzmealz.util.parseRupeesToPaise
import com.reevan.reevzmealz.util.suggestPlaces

/** Where the picker is in its source -> place -> food flow. */
private enum class PickerStep {
    /** Ate out, or home food? */
    SOURCE,

    /** The grid of restaurants. */
    PLACES,

    /** One restaurant's foods. */
    OUTSIDE_FOODS,

    /** Everything homecooked. */
    HOME_FOODS,

    /** Create a food that does not exist yet, and add it in one go. */
    NEW_FOOD,
}

/** Columns in the restaurant grid. Two, because a shop name needs the width of half a phone. */
private const val PLACE_COLUMNS = 2

/**
 * Pick a food to put in [slotType], in three steps: **ate out or home**, then **which
 * restaurant**, then **which of that restaurant's foods**.
 *
 * A meal eaten out almost always comes from a single restaurant, so choosing the shop first turns
 * a long flat list of every food into two taps and a short list. Home food skips the middle step,
 * since homecooked food has no place by definition.
 *
 * Nothing here is a dead end: a restaurant that is not in the grid yet, or a dish that is not in
 * that restaurant's list yet, can be created from the picker. That writes a real row to `foods`,
 * so it is available everywhere afterwards — the same as adding it in the Foods section.
 *
 * [foods] is every food, including ones the slot already holds: picking one again raises its
 * quantity rather than doing nothing, which is how a second helping is recorded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerSheet(
    slotType: MealType,
    foods: List<Food>,
    onDismiss: () -> Unit,
    onPick: (Food) -> Unit,
    onCreateAndPick: (name: String, source: MealPlace, pricePaise: Int?, place: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Saveable so a trip to another app does not drop the user back at step one. The step is an
    // enum and the place a String, both of which go into a Bundle as they are.
    var step by rememberSaveable { mutableStateOf(PickerStep.SOURCE) }
    var chosenPlace by rememberSaveable { mutableStateOf<String?>(null) }

    // Both "+ New place" and "+ New home food" arrive at NEW_FOOD with no place chosen, so which
    // kind of food is being created cannot be inferred from chosenPlace and is tracked here.
    var newFoodIsOutside by rememberSaveable { mutableStateOf(true) }

    val outsideFoods = remember(foods) { foods.filter { it.source == MealPlace.OUT } }
    val homeFoods = remember(foods) { foods.filter { it.source == MealPlace.HOME } }
    val places = remember(outsideFoods) { knownPlaces(outsideFoods.map { it.place }) }

    // Foods from the chosen restaurant. Matched case-insensitively because the grid shows one
    // spelling per place and older rows may differ in case.
    val placeFoods = remember(outsideFoods, chosenPlace) {
        val place = chosenPlace
        if (place == null) {
            emptyList()
        } else {
            outsideFoods.filter { it.place.equals(place, ignoreCase = true) }
        }
    }

    fun back() {
        step = when (step) {
            PickerStep.SOURCE -> PickerStep.SOURCE
            PickerStep.PLACES, PickerStep.HOME_FOODS -> PickerStep.SOURCE
            PickerStep.OUTSIDE_FOODS -> PickerStep.PLACES
            PickerStep.NEW_FOOD -> when {
                !newFoodIsOutside -> PickerStep.HOME_FOODS
                // Came from a restaurant's list if one was chosen, from the grid if not.
                chosenPlace != null -> PickerStep.OUTSIDE_FOODS
                else -> PickerStep.PLACES
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            StepHeader(
                title = when (step) {
                    PickerStep.SOURCE -> "Add to " + slotType.label.lowercase()
                    PickerStep.PLACES -> "Where did you eat?"
                    PickerStep.OUTSIDE_FOODS -> chosenPlace.orEmpty()
                    PickerStep.HOME_FOODS -> "Home food"
                    PickerStep.NEW_FOOD -> "New food"
                },
                onBack = if (step == PickerStep.SOURCE) null else ::back,
            )

            when (step) {
                PickerStep.SOURCE -> SourceStep(
                    onAteOut = {
                        chosenPlace = null
                        step = PickerStep.PLACES
                    },
                    onHomeFood = {
                        chosenPlace = null
                        step = PickerStep.HOME_FOODS
                    },
                )

                PickerStep.PLACES -> PlaceGrid(
                    places = places,
                    onPick = { place ->
                        chosenPlace = place
                        step = PickerStep.OUTSIDE_FOODS
                    },
                    onNewPlace = {
                        chosenPlace = null
                        newFoodIsOutside = true
                        step = PickerStep.NEW_FOOD
                    },
                )

                PickerStep.OUTSIDE_FOODS -> FoodList(
                    foods = placeFoods,
                    emptyText = "Nothing left from here.",
                    addLabel = "+ New food from here",
                    onPick = onPick,
                    onAdd = {
                        newFoodIsOutside = true
                        step = PickerStep.NEW_FOOD
                    },
                )

                PickerStep.HOME_FOODS -> FoodList(
                    foods = homeFoods,
                    emptyText = "No homecooked food yet.",
                    addLabel = "+ New home food",
                    onPick = onPick,
                    onAdd = {
                        chosenPlace = null
                        newFoodIsOutside = false
                        step = PickerStep.NEW_FOOD
                    },
                )

                PickerStep.NEW_FOOD -> NewFoodStep(
                    isOutside = newFoodIsOutside,
                    initialPlace = chosenPlace,
                    knownPlaces = places,
                    onSave = onCreateAndPick,
                )
            }
        }
    }
}

/**
 * The sheet's title, with a way back when there is somewhere to go back to.
 *
 * A visible back control rather than relying on the system back gesture: back on a
 * `ModalBottomSheet` dismisses the whole sheet, which would throw away two taps of progress.
 */
@Composable
private fun StepHeader(title: String, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Step one: two blocks, ate out or home.
 *
 * Red for outside and blue for home, the same pair the meal slots use for a food's source — so
 * the choice made here is already the colour the row will be.
 */
@Composable
private fun SourceStep(onAteOut: () -> Unit, onHomeFood: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BigChoice(
            label = "ATE OUT",
            hint = "From a restaurant",
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            border = MaterialTheme.colorScheme.error,
            onClick = onAteOut,
        )
        BigChoice(
            label = "HOME FOOD",
            hint = "Cooked at home",
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            border = MaterialTheme.colorScheme.secondary,
            onClick = onHomeFood,
        )
    }
}

@Composable
private fun BigChoice(
    label: String,
    hint: String,
    container: Color,
    content: Color,
    border: Color,
    onClick: () -> Unit,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Step two: the restaurants, as a grid of boxes.
 *
 * Built from Rows rather than a lazy grid, the same way the month picker is: the number of shops
 * is small, and a plain scrolling Column has no nesting rules to get wrong. The last row is padded
 * with empty space so a lone tile keeps its column width instead of stretching across.
 */
@Composable
private fun PlaceGrid(
    places: List<String>,
    onPick: (String) -> Unit,
    onNewPlace: () -> Unit,
) {
    val green = addBlockColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (places.isEmpty()) {
            Text(
                text = "No restaurants yet. Add the first one below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
        }

        places.chunked(PLACE_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { place ->
                    PlaceTile(place = place, onClick = { onPick(place) })
                }
                // Keeps the grid on a fixed column width when the last row is short.
                repeat(PLACE_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Surface(
            color = green.container,
            contentColor = green.content,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(2.dp, green.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onNewPlace)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+ NEW PLACE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RowScope.PlaceTile(place: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier.weight(1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = place,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Step three: the foods to choose from, with a way to add one that is not listed. */
@Composable
private fun FoodList(
    foods: List<Food>,
    emptyText: String,
    addLabel: String,
    onPick: (Food) -> Unit,
    onAdd: () -> Unit,
) {
    val green = addBlockColors()
    LazyColumn(
        modifier = Modifier.heightIn(max = 420.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (foods.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(count = foods.size, key = { index -> foods[index].id }) { index ->
            // Between rows only, so the list is ruled rather than boxed in.
            if (index > 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            val food = foods[index]
            FoodPickerRow(food = food, onClick = { onPick(food) })
        }

        item {
            Surface(
                color = green.container,
                contentColor = green.content,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(2.dp, green.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(onClick = onAdd)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = addLabel.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * The last step when nothing existing fits: name the food, and save it for good.
 *
 * This is a real write to the foods table, not a one-off — the food shows up in the Foods section
 * and in every future picker, which is the point. Price is required for outside food so a meal
 * eaten out cannot silently cost nothing.
 */
@Composable
private fun NewFoodStep(
    isOutside: Boolean,
    initialPlace: String?,
    knownPlaces: List<String>,
    onSave: (name: String, source: MealPlace, pricePaise: Int?, place: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var placeText by rememberSaveable { mutableStateOf(initialPlace.orEmpty()) }

    val parsedPrice = remember(priceText) { parseRupeesToPaise(priceText) }
    val priceValid = !isOutside || parsedPrice != null
    val canSave = name.isNotBlank() && priceValid &&
        (!isOutside || placeText.isNotBlank())

    val suggestions = remember(knownPlaces, placeText) {
        if (isOutside) suggestPlaces(knownPlaces, placeText) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isOutside) {
            OutlinedTextField(
                value = placeText,
                onValueChange = { placeText = it },
                label = { Text("Place") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (suggestions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(count = suggestions.size) { index ->
                        val place = suggestions[index]
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        ) {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clickable { placeText = place }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = place,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = if (isOutside) ImeAction.Next else ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (isOutside) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price") },
                prefix = { Text("₹") },
                singleLine = true,
                isError = priceText.isNotBlank() && parsedPrice == null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                onSave(
                    name,
                    if (isOutside) MealPlace.OUT else MealPlace.HOME,
                    if (isOutside) parsedPrice else null,
                    if (isOutside) placeText else null,
                )
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text("Save and add")
        }
    }
}

/**
 * One food to pick, with where it came from on the right.
 *
 * The place still earns its place in the row even now that the list is one restaurant's: the home
 * list shows no chip at all, and it keeps this row identical to the Foods list's.
 */
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
