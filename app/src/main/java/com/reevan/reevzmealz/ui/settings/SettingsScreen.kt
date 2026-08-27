package com.reevan.reevzmealz.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.PurgeCounts
import com.reevan.reevzmealz.data.ThemeMode
import com.reevan.reevzmealz.ui.sin.SIN_CONFIG_LOCK_DAYS
import com.reevan.reevzmealz.ui.sin.SinViewModel
import com.reevan.reevzmealz.util.RETAINED_MONTHS
import com.reevan.reevzmealz.util.formatShortDate

/**
 * Everything configurable lives here: Theme, Notifications, Sins and Storage. New settings should
 * be added as another section rather than scattered into their own screens.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    sinViewModel: SinViewModel = viewModel(factory = SinViewModel.Factory),
    preferences: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val sinState by sinViewModel.uiState.collectAsState()
    val appSettings by preferences.settings.collectAsState()

    val context = LocalContext.current
    // Re-read on resume: the user may have changed it in system settings and come back.
    var notificationsPermitted by remember { mutableStateOf(hasNotificationPermission(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsPermitted = hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    // Android 13+ needs the user's consent before any notification can be shown.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsPermitted = granted
        preferences.setRemindersEnabled(granted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeading("Theme")
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = appSettings.themeMode == mode,
                    onClick = { preferences.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                    label = { Text(mode.label) },
                )
            }
        }
        Hint("System follows your phone's setting. Colours come from your wallpaper either way.")

        SectionDivider()

        SectionHeading("Notifications")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Nightly plan reminder", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Only if tomorrow has nothing planned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = appSettings.remindersEnabled,
                onCheckedChange = { wantOn ->
                    if (wantOn && !notificationsPermitted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        preferences.setRemindersEnabled(wantOn)
                    }
                },
            )
        }

        // Without this the switch could read "on" while Android silently drops every reminder.
        if (appSettings.remindersEnabled && !notificationsPermitted) {
            Text(
                text = "Reminders are on, but Android is not allowing notifications for this " +
                    "app, so nothing will appear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Allow notifications")
            }
        }
        ReminderTimeEditor(
            hour = appSettings.reminderHour,
            minute = appSettings.reminderMinute,
            enabled = appSettings.remindersEnabled,
            onSave = preferences::setReminderTime,
        )

        SectionDivider()

        SectionHeading("Sins")
        Hint(
            "One sin for each meal that does not go to plan. You get this many per month, and " +
                "whatever is left is discarded when the month turns over.",
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

        SectionDivider()

        SectionHeading("Storage")
        Hint(
            "Money Spent shows the last $RETAINED_MONTHS months. Older purchases, plans and " +
                "eaten records can be deleted to keep the app small.",
        )
        Hint("Anything before " + formatShortDate(viewModel.cutoff) + " counts as older.")
        OutlinedButton(
            onClick = viewModel::requestPurge,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Delete records older than $RETAINED_MONTHS months")
        }
        Hint("Your Foods list is never deleted.")
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

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}

/** Reminder time as two small number fields, avoiding a full time-picker dependency. */
@Composable
private fun ReminderTimeEditor(
    hour: Int,
    minute: Int,
    enabled: Boolean,
    onSave: (Int, Int) -> Unit,
) {
    var hourText by remember(hour) { mutableStateOf(hour.toString()) }
    var minuteText by remember(minute) { mutableStateOf(minute.toString().padStart(2, '0')) }

    val parsedHour = hourText.trim().toIntOrNull()
    val parsedMinute = minuteText.trim().toIntOrNull()
    val valid = parsedHour != null && parsedHour in 0..23 &&
        parsedMinute != null && parsedMinute in 0..59
    val changed = parsedHour != hour || parsedMinute != minute

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = hourText,
            onValueChange = { hourText = it },
            label = { Text("Hour") },
            singleLine = true,
            enabled = enabled,
            isError = !valid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = minuteText,
            onValueChange = { minuteText = it },
            label = { Text("Minute") },
            singleLine = true,
            enabled = enabled,
            isError = !valid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                if (parsedHour != null && parsedMinute != null) onSave(parsedHour, parsedMinute)
            },
            enabled = enabled && valid && changed,
            modifier = Modifier.height(48.dp),
        ) {
            Text("Set")
        }
    }
    Hint("24-hour, your phone's local time. Default is 19:00.")
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

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
