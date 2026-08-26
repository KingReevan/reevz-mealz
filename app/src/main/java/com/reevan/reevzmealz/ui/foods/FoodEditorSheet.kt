package com.reevan.reevzmealz.ui.foods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.util.paiseToEditableRupees
import com.reevan.reevzmealz.util.parseRupeesToPaise

/**
 * Create or edit a single food. Pass null for [food] to create.
 *
 * State is keyed on the food's id so reopening the sheet for a different row starts from that
 * row's values rather than the previous one's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorSheet(
    food: Food?,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, source: MealPlace, pricePaise: Int?) -> Unit,
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

            // Price is only meaningful for food bought outside.
            AnimatedVisibility(visible = isOutside) {
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
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    onSave(
                        food?.id,
                        name,
                        if (isOutside) MealPlace.OUT else MealPlace.HOME,
                        if (isOutside) parsedPrice else null,
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
