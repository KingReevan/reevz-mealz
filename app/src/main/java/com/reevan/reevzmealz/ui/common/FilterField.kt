package com.reevan.reevzmealz.ui.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

/**
 * A single-line filter box: type to narrow a list, tap the ✕ to clear it.
 *
 * Shared by the food picker (name and place) and the Foods list (name), so the three boxes cannot
 * drift apart in behaviour. It carries no state of its own — the caller owns the text, because the
 * caller is what does the filtering.
 *
 * Deliberately not auto-focused: on the picker the keyboard would cover the list, and most uses of
 * both screens involve no filtering at all. The clear button appears only when there is something
 * to clear, so an empty box keeps its full width for typing — which matters on the picker, where
 * two boxes share one row.
 */
@Composable
fun FilterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = if (value.isNotEmpty()) {
            {
                TextButton(onClick = { onValueChange("") }) {
                    Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDone() }),
        modifier = modifier,
    )
}
