package com.reevan.reevzmealz.ui.bought

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.util.formatShortDate
import com.reevan.reevzmealz.util.paiseToEditableRupees
import com.reevan.reevzmealz.util.parseRupeesToPaise

/**
 * Create or edit one purchase. Pass null for [item] to create.
 *
 * State is keyed on the item's id so reopening for a different row starts from that row's values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoughtItemEditorSheet(
    item: BoughtItem?,
    onDismiss: () -> Unit,
    onSave: (id: Long?, name: String, pricePaise: Int, boughtAt: Long?) -> Unit,
    onDelete: (BoughtItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val key = item?.id

    var name by rememberSaveable(key) { mutableStateOf(item?.name ?: "") }
    var priceText by rememberSaveable(key) {
        mutableStateOf(item?.pricePaise?.let { paiseToEditableRupees(it) } ?: "")
    }

    val parsedPrice = remember(priceText) { parseRupeesToPaise(priceText) }
    val priceInvalid = parsedPrice == null
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
                text = if (item == null) "Add bought item" else "Edit bought item",
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("What did you buy?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price") },
                prefix = { Text("₹") },
                singleLine = true,
                isError = priceInvalid,
                supportingText = if (priceInvalid) {
                    { Text("Enter an amount like 80 or 80.50") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = if (item == null) {
                    "Saved against today"
                } else {
                    "Bought " + formatShortDate(item.boughtAt)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { onSave(item?.id, name, parsedPrice ?: 0, item?.boughtAt) },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(if (item == null) "Save item" else "Save changes")
            }

            if (item != null) {
                TextButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = "Delete item",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
