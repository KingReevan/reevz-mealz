package com.reevan.reevzmealz.ui.foods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.util.paiseToEditableRupees
import com.reevan.reevzmealz.util.parseRupeesToPaise
import com.reevan.reevzmealz.util.suggestPlaces

/**
 * Create or edit a single food. Pass null for [food] to create.
 *
 * State is keyed on the food's id so reopening the sheet for a different row starts from that
 * row's values rather than the previous one's.
 *
 * [knownPlaces] are the places already used by other foods, most-used first, offered under the
 * Place field so a repeat shop is one tap rather than retyped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorSheet(
    food: Food?,
    knownPlaces: List<String>,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, source: MealPlace, pricePaise: Int?, place: String?) -> Unit,
    onDelete: (Food) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val key = food?.id

    var name by rememberSaveable(key) { mutableStateOf(food?.name ?: "") }
    var isOutside by rememberSaveable(key) {
        mutableStateOf(food?.source == MealPlace.OUT)
    }
    var priceText by rememberSaveable(key) {
        mutableStateOf(food?.pricePaise?.let { paiseToEditableRupees(it) } ?: "")
    }
    // A TextFieldValue rather than a plain String, purely so tapping a suggestion can put the
    // caret at the end of the place it inserted. The String overload of OutlinedTextField keeps
    // whatever selection it had and merely clamps it to the new length, so after typing "t" and
    // tapping "Thilak" the caret stayed at offset 1 and the next thing typed landed inside the
    // word ("cube" + "hilak").
    var placeField by rememberSaveable(key, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(food?.place ?: ""))
    }

    val parsedPrice = remember(priceText) { parseRupeesToPaise(priceText) }
    val priceInvalid = isOutside && parsedPrice == null
    val canSave = name.isNotBlank() && !priceInvalid

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (food == null) "New food" else "Edit food",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bought from outside",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = if (isOutside) "Outside" else "Homecooked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isOutside,
                    onCheckedChange = { isOutside = it },
                )
            }

            // Price and place are only meaningful for food bought outside.
            AnimatedVisibility(visible = isOutside) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price") },
                        prefix = { Text("₹") },
                        singleLine = true,
                        isError = priceInvalid,
                        supportingText = if (priceInvalid) {
                            { Text("Enter an amount like 120 or 120.50") }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // The Place field and its suggestions are one unit, with a tighter gap than
                    // the form's 16dp so the row reads as belonging to the field above it.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = placeField,
                            onValueChange = { placeField = it },
                            label = { Text("Place") },
                            placeholder = { Text("Where you bought it") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        val suggestions = remember(knownPlaces, placeField.text) {
                            suggestPlaces(knownPlaces, placeField.text)
                        }
                        // Nothing to suggest means no row at all, rather than an empty gap.
                        if (suggestions.isNotEmpty()) {
                            PlaceSuggestions(
                                suggestions = suggestions,
                                onPick = { picked ->
                                    // Caret to the end, so carrying on typing appends rather
                                    // than dropping characters into the middle of the word.
                                    placeField = TextFieldValue(
                                        text = picked,
                                        selection = TextRange(picked.length),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        food?.id,
                        name,
                        if (isOutside) MealPlace.OUT else MealPlace.HOME,
                        if (isOutside) parsedPrice else null,
                        if (isOutside) placeField.text else null,
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(if (food == null) "Create food" else "Save changes")
            }

            if (food != null) {
                TextButton(
                    onClick = { onDelete(food) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = "Delete food",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * The places already used, as tappable chips under the Place field.
 *
 * One line that scrolls sideways, not a wrapping block: a wrapping row would grow taller as places
 * accumulate and shove the Create button down the sheet, and the row's height would change as you
 * type. Typing narrows the list quickly enough that a sideways swipe is rarely needed.
 *
 * Deliberately no `key` on the items. The list reorders on every keystroke and has no state worth
 * preserving across those reorders, and an index key cannot collide — a duplicate key is a crash,
 * which is not a trade worth making for a row of five chips.
 */
@Composable
private fun PlaceSuggestions(suggestions: List<String>, onPick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count = suggestions.size) { index ->
            val place = suggestions[index]
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
                // The border marks it as a control rather than the static tag the same blue
                // means in the Foods list.
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onPick(place) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = place,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
