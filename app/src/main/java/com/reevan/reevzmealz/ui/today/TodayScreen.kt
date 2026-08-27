package com.reevan.reevzmealz.ui.today

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.SlotFood
import com.reevan.reevzmealz.ui.common.FoodPickerSheet
import com.reevan.reevzmealz.ui.common.PlanSlot
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

    var editMode by rememberSaveable { mutableStateOf(false) }
    var pickerForSlot by remember { mutableStateOf<MealType?>(null) }
    var showEndDay by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        EditModeHeader(
            dayStart = viewModel.dayStart,
            editMode = editMode,
            onEditModeChange = { enabled ->
                editMode = enabled
                if (enabled) viewModel.beginEditing()
            },
        )
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            if (state.loaded && !state.hasAnything && !editMode) {
                NothingPlanned(
                    onGoToPlan = onGoToPlan,
                    onEditToday = {
                        editMode = true
                        viewModel.beginEditing()
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                ) {
                    items(state.slots.size) { index ->
                        val slot = state.slots[index]
                        SlotBlock(
                            slot = slot,
                            plannedNames = if (state.dayLogged) {
                                state.plannedFoodNames(slot.type)
                            } else {
                                emptyList()
                            },
                            showPlannedLine = state.dayLogged,
                            editMode = editMode,
                            onAddFood = { pickerForSlot = slot.type },
                            onRemoveFood = viewModel::removeFood,
                            onClearSlot = { viewModel.clearSlot(slot.type) },
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
            dayEnded = sinState.todayEnded,
            onEndDay = { showEndDay = true },
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
        val addable by viewModel.addableFoods(slotType).collectAsState()
        FoodPickerSheet(
            slotType = slotType,
            foods = addable,
            anyFoodsExist = anyFoodsExist,
            onDismiss = { pickerForSlot = null },
            onPick = { food ->
                viewModel.addFood(slotType, food)
                pickerForSlot = null
            },
        )
    }
}

@Composable
private fun EditModeHeader(
    dayStart: Long,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDayHeading(dayStart),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (editMode) "Editing what you ate" else "Edit mode",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Switch(checked = editMode, onCheckedChange = onEditModeChange)
    }
}

@Composable
private fun SlotBlock(
    slot: PlanSlot,
    plannedNames: List<String>,
    showPlannedLine: Boolean,
    editMode: Boolean,
    onAddFood: () -> Unit,
    onRemoveFood: (Long) -> Unit,
    onClearSlot: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = slot.type.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            // Only a slot containing outside food has a price worth showing.
            if (!slot.isEmpty && !slot.isFullyHomecooked) {
                Text(
                    text = formatPaise(slot.costPaise),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Once the day has its own record, show what was originally planned for reference.
        if (showPlannedLine) {
            Text(
                text = "Planned: " + plannedNames.ifEmpty { listOf("nothing") }.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }

        if (slot.isEmpty) {
            Text(
                text = "No food",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            slot.foods.forEach { food ->
                SlotFoodRow(
                    food = food,
                    editMode = editMode,
                    onRemove = { onRemoveFood(food.entryId) },
                )
            }
        }

        if (editMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onAddFood,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("+ Add food")
                }
                if (!slot.isEmpty) {
                    TextButton(
                        onClick = onClearSlot,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = "Ate nothing", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotFoodRow(food: SlotFood, editMode: Boolean, onRemove: () -> Unit) {
    val isOutside = food.source == MealPlace.OUT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = if (editMode) 4.dp else 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = food.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (isOutside) formatPaise((food.pricePaise ?: 0).toLong()) else "Homecooked",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOutside) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (editMode) {
            TextButton(onClick = onRemove) {
                Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Pinned footer: the day's real spend, which is the number worth watching. */
@Composable
private fun DayTotal(
    totalPaise: Long,
    plannedTotalPaise: Long,
    showPlanned: Boolean,
    dayEnded: Boolean,
    onEndDay: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
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

            // Kept in the footer so it stays in the thumb zone.
            Button(
                onClick = onEndDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(if (dayEnded) "Day ended" else "End day")
            }
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
