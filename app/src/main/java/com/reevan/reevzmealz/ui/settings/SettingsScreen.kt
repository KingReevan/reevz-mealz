package com.reevan.reevzmealz.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.PurgeCounts
import com.reevan.reevzmealz.ui.sin.SIN_CONFIG_LOCK_DAYS
import com.reevan.reevzmealz.ui.sin.SinViewModel
import com.reevan.reevzmealz.util.RETAINED_MONTHS
import com.reevan.reevzmealz.util.formatShortDate

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    sinViewModel: SinViewModel = viewModel(factory = SinViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val sinState by sinViewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Sins",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "One sin for each meal that does not go to plan. You get this many per month, " +
                "and whatever is left is discarded when the month turns over.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AllowanceEditor(
            allowance = sinState.status.allowance,
            editable = sinState.allowanceEditable,
            daysUntilEditable = sinState.daysUntilEditable,
            onSave = sinViewModel::setAllowance,
        )

        Text(
            text = sinState.status.remaining.toString() + " of " +
                sinState.status.allowance + " left this month",
            style = MaterialTheme.typography.bodyMedium,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Storage",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Money Spent shows the last $RETAINED_MONTHS months. Older purchases, plans " +
                "and eaten records can be deleted to keep the app small.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Anything before " + formatShortDate(viewModel.cutoff) + " counts as older.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = viewModel::requestPurge,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Delete records older than $RETAINED_MONTHS months")
        }
        Text(
            text = "Your Foods list is never deleted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val pending = state.pendingPurge
    if (pending != null) {
        PurgeConfirmDialog(
            counts = pending,
            onConfirm = viewModel::confirmPurge,
            onCancel = viewModel::cancelPurge,
        )
    }

    val result = state.lastPurgeResult
    if (result != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("Deleted") },
            text = { Text(describe(result) + " removed.") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("OK") }
            },
        )
    }
}

/**
 * The sin allowance. Locked for 3 days after being set, so it cannot be raised the moment it
 * starts to pinch — which is the whole point of the number.
 */
@Composable
private fun AllowanceEditor(
    allowance: Int,
    editable: Boolean,
    daysUntilEditable: Int,
    onSave: (Int) -> Unit,
) {
    var text by remember(allowance, editable) { mutableStateOf(allowance.toString()) }
    val parsed = text.trim().toIntOrNull()
    val valid = parsed != null && parsed >= 0
    val changed = parsed != allowance

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Sins per month") },
            singleLine = true,
            enabled = editable,
            isError = !valid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { parsed?.let(onSave) },
            enabled = editable && valid && changed,
            modifier = Modifier.height(48.dp),
        ) {
            Text("Set")
        }
    }

    Text(
        text = if (editable) {
            "Once set, this cannot be changed for $SIN_CONFIG_LOCK_DAYS days."
        } else {
            "Locked. You can change this again in " + daysUntilEditable +
                if (daysUntilEditable == 1) " day." else " days."
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (editable) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun PurgeConfirmDialog(
    counts: PurgeCounts,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    if (counts.isEmpty) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Nothing to delete") },
            text = { Text("There are no records older than $RETAINED_MONTHS months.") },
            confirmButton = {
                TextButton(onClick = onCancel) { Text("OK") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete old records?") },
        text = {
            Text(
                "This will permanently delete " + describe(counts) +
                    ". It cannot be undone and there is no backup.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

private fun describe(counts: PurgeCounts): String {
    val parts = buildList {
        if (counts.boughtItems > 0) add(plural(counts.boughtItems, "bought item", "bought items"))
        if (counts.plannedMeals > 0) {
            add(plural(counts.plannedMeals, "planned meal entry", "planned meal entries"))
        }
        if (counts.eatenMeals > 0) {
            add(plural(counts.eatenMeals, "eaten meal entry", "eaten meal entries"))
        }
    }
    return if (parts.isEmpty()) "nothing" else parts.joinToString(", ")
}

private fun plural(count: Int, singular: String, plural: String): String =
    count.toString() + " " + if (count == 1) singular else plural
