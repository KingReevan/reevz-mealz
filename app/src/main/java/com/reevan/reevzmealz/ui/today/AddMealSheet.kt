package com.reevan.reevzmealz.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.util.parseRupeesToPaise

/** Short labels so all four meal types fit a phone width. */
private val MealType.shortLabel: String
    get() = when (this) {
        MealType.BREAKFAST -> "Brkfst"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACK -> "Snack"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: MealType,
        place: MealPlace,
        costPaise: Int,
        notes: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }
    var place by rememberSaveable { mutableStateOf(MealPlace.HOME) }
    var cost by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val costPaise = remember(cost) { parseRupeesToPaise(cost) }
    val costInvalid = costPaise == null
    val canSave = name.isNotBlank() && costPaise != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Add meal", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                MealType.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = type == entry,
                        onClick = { type = entry },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MealType.entries.size,
                        ),
                        label = { Text(entry.shortLabel) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("What did you eat?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                MealPlace.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = place == entry,
                        onClick = { place = entry },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MealPlace.entries.size,
                        ),
                        label = { Text(entry.label) },
                    )
                }
            }

            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Cost") },
                prefix = { Text("₹") },
                singleLine = true,
                isError = costInvalid,
                supportingText = if (costInvalid) {
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

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onSave(name, type, place, costPaise ?: 0, notes) },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Save meal")
            }
        }
    }
}
