package com.reevan.reevzmealz.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.ui.common.AddFoodAction
import com.reevan.reevzmealz.ui.common.ClearSlotAction
import com.reevan.reevzmealz.ui.common.FoodPickerSheet
import com.reevan.reevzmealz.ui.common.MealSlotCard
import com.reevan.reevzmealz.ui.sin.DayAlreadyEndedDialog
import com.reevan.reevzmealz.ui.sin.EndDayDialog
import com.reevan.reevzmealz.ui.sin.SinViewModel
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatPaise

/**
 * The current day's meals: all four slots and the day's total pinned at the bottom.
 *
 * Read-only by default, showing the plan. Edit Mode switches to recording what was actually
 * eaten — seeded from the plan, then freely editable — without ever changing the plan itself.
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onGoToPlan: () -> Unit = {},
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
    sinViewModel: SinViewModel = viewModel(factory = SinViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val anyFoodsExist by viewModel.anyFoodsExist.collectAsState()
    val sinState by sinViewModel.uiState.collectAsState()
    val dayStart by viewModel.dayStart.collectAsState()
    val pickerForSlot by viewModel.pickerSlot.collectAsState()

    var editMode by rememberSaveable { mutableStateOf(false) }
    var showEndDay by remember { mutableStateOf(false) }

    // Left open across midnight, the screen would otherwise keep showing yesterday.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDay()
        sinViewModel.refreshDay()
        onPauseOrDispose { }
    }

    // Removing an entry needs an id from the eaten record; before the day is logged the rows on
    // screen are plan rows, whose ids belong to a different table.
    val canRemove = editMode && state.dayLogged

    Column(modifier = modifier.fillMaxSize()) {
        EditModeHeader(
            dayStart = dayStart,
            editMode = editMode,
            onEditModeChange = { enabled ->
                editMode = enabled
                if (enabled) viewModel.beginEditing()
            },
            dayEnded = sinState.todayEnded,
            onEndDay = { showEndDay = true },
        )
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            if (state.loaded && !state.hasAnything && !editMode && !state.dayLogged) {
                NothingPlanned(
                    onGoToPlan = onGoToPlan,
                    onEditToday = {
                        editMode = true
                        viewModel.beginEditing()
                    },
                )
            } else {
                LazyColumn(
                    // Same colour as the slots, so the space left below dinner reads as the end of
                    // one panel rather than a gap torn out of it.
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        count = state.slots.size,
                        key = { index -> state.slots[index].type.name },
                    ) { index ->
                        val slot = state.slots[index]
                        MealSlotCard(
                            slot = slot,
                            canRemove = canRemove,
                            onRemoveFood = viewModel::removeFood,
                            // Only worth a line when there was actually a plan to compare against;
                            // "Planned: nothing" said nothing and cost a line in every slot.
                            supportingLine = state.plannedFoodNames(slot.type)
                                .takeIf { state.dayLogged && it.isNotEmpty() }
                                ?.let { "Planned: " + it.joinToString(", ") },
                            actions = if (editMode) {
                                {
                                    AddFoodAction { viewModel.openPicker(slot.type) }
                                    if (!slot.isEmpty) {
                                        ClearSlotAction("Ate nothing") {
                                            viewModel.clearSlot(slot.type)
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }

                    if (editMode && state.dayLogged) {
                        item {
                            TextButton(
                                onClick = viewModel::resetToPlan,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text("Reset today back to the plan")
                            }
                        }
                    }
                }
            }
        }

        DayTotal(
            totalPaise = state.totalPaise,
            plannedTotalPaise = state.plannedTotalPaise,
            showPlanned = state.showPlannedComparison,
        )
    }

    if (showEndDay) {
        if (sinState.todayEnded) {
            DayAlreadyEndedDialog(
                status = sinState.status,
                sinnedToday = sinState.sinnedToday,
                onDismiss = { showEndDay = false },
            )
        } else {
            EndDayDialog(
                status = sinState.status,
                onDismiss = { showEndDay = false },
                onConfirm = { sinned ->
                    sinViewModel.endDay(sinned)
                    showEndDay = false
                },
            )
        }
    }

    val slotType = pickerForSlot
    if (slotType != null) {
        val addable by viewModel.addableFoods.collectAsState()
        FoodPickerSheet(
            slotType = slotType,
            foods = addable,
            anyFoodsExist = anyFoodsExist,
            onDismiss = viewModel::closePicker,
            onPick = { food ->
                viewModel.addFood(slotType, food)
                viewModel.closePicker()
            },
        )
    }
}

/**
 * The day, the Edit Mode switch and End day, all on one line.
 *
 * End day lives here rather than in the footer because a full-width button down there cost enough
 * height to push dinner off the screen. It stays within thumb reach of the top-right corner.
 */
@Composable
private fun EditModeHeader(
    dayStart: Long,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    dayEnded: Boolean,
    onEndDay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDayHeading(dayStart),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = if (editMode) "Editing" else "Edit mode",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
        Switch(checked = editMode, onCheckedChange = onEditModeChange)
        Button(
            onClick = onEndDay,
            modifier = Modifier
                .padding(start = 8.dp)
                .heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(text = if (dayEnded) "Ended" else "End day", maxLines = 1)
        }
    }
}

/** Pinned footer: the day's real spend, which is the number worth watching. */
@Composable
private fun DayTotal(
    totalPaise: Long,
    plannedTotalPaise: Long,
    showPlanned: Boolean,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (showPlanned) {
                    Text(
                        text = "Planned " + formatPaise(plannedTotalPaise),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatPaise(totalPaise),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun NothingPlanned(onGoToPlan: () -> Unit, onEditToday: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Nothing planned for today.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onGoToPlan,
                modifier = Modifier.height(48.dp),
            ) {
                Text("Plan today")
            }
            TextButton(
                onClick = onEditToday,
                modifier = Modifier.height(48.dp),
            ) {
                Text("Just record what I ate")
            }
        }
    }
}
