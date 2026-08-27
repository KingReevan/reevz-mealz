package com.reevan.reevzmealz.ui.sin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.ui.theme.goodColor

/**
 * End-of-day judgement: for each meal slot, did the day go to plan?
 *
 * Every toggle starts positive — the assumption is that you followed the plan, and a sin is
 * something you actively admit to. Each slot flipped negative costs one sin on confirm.
 */
@Composable
fun EndDayDialog(
    status: SinStatus,
    onDismiss: () -> Unit,
    onConfirm: (sinned: Set<MealType>) -> Unit,
) {
    // true = followed the plan.
    var followed by remember {
        mutableStateOf(MealType.entries.associateWith { true })
    }
    val sinned = followed.filterValues { !it }.keys
    val pending = sinned.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                if (status.failed) {
                    Text(
                        text = "You have failed for the month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "End day",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Did each meal go to plan?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                MealType.entries.forEach { type ->
                    MealJudgementRow(
                        type = type,
                        followedPlan = followed[type] == true,
                        onChange = { value ->
                            followed = followed.toMutableMap().apply { put(type, value) }
                        },
                    )
                }

                SinsLeftLine(status = status, pending = pending)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sinned) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MealJudgementRow(
    type: MealType,
    followedPlan: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val accent = if (followedPlan) goodColor() else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.label,
                style = MaterialTheme.typography.bodyLarge,
                color = accent,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (followedPlan) "Good Job!" else "This is bad!",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
        }
        Switch(
            checked = followedPlan,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun SinsLeftLine(status: SinStatus, pending: Int) {
    val after = status.remainingAfter(pending)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "Sins left this month: " + after,
            style = MaterialTheme.typography.titleMedium,
            color = if (status.wouldFail(pending)) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (pending > 0) {
            Text(
                text = pending.toString() + " to be deducted on confirm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shown instead when the day has already been settled. */
@Composable
fun DayAlreadyEndedDialog(
    status: SinStatus,
    sinnedToday: Set<MealType>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                if (status.failed) {
                    Text(
                        text = "You have failed for the month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(text = "Day already ended", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MealType.entries.forEach { type ->
                    val sinned = sinnedToday.contains(type)
                    val accent = if (sinned) {
                        MaterialTheme.colorScheme.error
                    } else {
                        goodColor()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = accent,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (sinned) "This is bad!" else "Good Job!",
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                        )
                    }
                }
                Text(
                    text = "Sins left this month: " + status.remaining,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}
