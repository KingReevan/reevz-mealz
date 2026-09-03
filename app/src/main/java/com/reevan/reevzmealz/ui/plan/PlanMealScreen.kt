package com.reevan.reevzmealz.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.ui.common.FoodPickerSheet
import com.reevan.reevzmealz.ui.common.AddFoodAction
import com.reevan.reevzmealz.ui.common.MealSlotCard
import com.reevan.reevzmealz.ui.common.SlotLayout
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatPaise

/**
 * Pick a day, then fill its four meal slots with foods from the Foods section.
 */
@Composable
fun PlanMealScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanMealViewModel = viewModel(factory = PlanMealViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val lock by viewModel.lock.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val anchorDay by viewModel.anchorDay.collectAsState()
    val plannedDays by viewModel.plannedDays.collectAsState()
    val today by viewModel.today.collectAsState()
    val pickerForSlot by viewModel.pickerSlot.collectAsState()

    var mode by rememberSaveable { mutableStateOf(PickerMode.WEEK) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DayPicker(
            mode = mode,
            onModeChange = { mode = it },
            selectedDay = selectedDay,
            onSelectDay = viewModel::selectDay,
            anchorDay = anchorDay,
            onAnchorChange = viewModel::moveAnchor,
            plannedDays = plannedDays,
            today = today,
        )
        HorizontalDivider()

        PlanHeading(day = selectedDay, lock = lock)

        LazyColumn(
            // Matches Today: the slots and the space below them are one continuous panel.
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                    // Plan Meal always shows plan rows, so removal is safe whenever it is
                    // allowed at all — and it is only allowed while the day is still ahead.
                    canRemove = lock.isOpen,
                    onRemoveFood = viewModel::removeSlotFood,
                    // The day picker owns roughly a third of this screen, so the slots use the
                    // shorter side-by-side layout to keep dinner nearer the fold.
                    layout = SlotLayout.SIDE_BY_SIDE,
                    emptyText = "No food",
                    // Just the + block, as on Today. Clear was a second route to emptying a
                    // slot when every row already carries its own remove. A day that has begun
                    // gets no actions at all: it is a record now, and Today owns records.
                    actions = if (lock.isOpen) {
                        { AddFoodAction { viewModel.openPicker(slot.type) } }
                    } else {
                        null
                    },
                )
            }
        }

        DayTotal(totalPaise = state.totalPaise)
    }

    val slotType = pickerForSlot
    if (slotType != null) {
        val assignable by viewModel.assignableFoods.collectAsState()
        FoodPickerSheet(
            slotType = slotType,
            foods = assignable,
            onDismiss = viewModel::closePicker,
            onPick = { food ->
                viewModel.assignFood(slotType, food)
                viewModel.closePicker()
            },
            onCreateAndPick = { name, source, pricePaise, place ->
                viewModel.createAndAssignFood(slotType, name, source, pricePaise, place)
                viewModel.closePicker()
            },
        )
    }
}

/**
 * The selected day, plus why its plan is closed when it is.
 *
 * The reason line only appears when locked, so the common case still costs one line. It can afford
 * the extra line anyway: a locked day shows no `+` blocks, which saves far more height than this
 * spends.
 */
@Composable
private fun PlanHeading(day: Long, lock: PlanLock) {
    val heading = when (lock) {
        PlanLock.OPEN -> "Planning " + formatDayHeading(day)
        PlanLock.DAY_UNDER_WAY, PlanLock.DAY_PASSED -> formatDayHeading(day) + " · locked"
    }
    // Names the way out rather than just refusing: the day is still editable, just from Today.
    val reason = when (lock) {
        PlanLock.OPEN -> null
        PlanLock.DAY_UNDER_WAY -> "The day has begun. Use Edit mode in Today."
        PlanLock.DAY_PASSED -> "This day has passed."
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                // Yellow is the HUD's "take note" colour; 12.3:1 dark, 4.8:1 light.
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun DayTotal(totalPaise: Long) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Day total",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPaise(totalPaise),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
