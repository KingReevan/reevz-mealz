package com.reevan.reevzmealz.ui.plan

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.SlotFood
import com.reevan.reevzmealz.ui.common.FoodPickerSheet
import com.reevan.reevzmealz.ui.common.PlanSlot
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
    val selectedDay by viewModel.selectedDay.collectAsState()
    val anchorDay by viewModel.anchorDay.collectAsState()
    val plannedDays by viewModel.plannedDays.collectAsState()
    val anyFoodsExist by viewModel.anyFoodsExist.collectAsState()

    var mode by rememberSaveable { mutableStateOf(PickerMode.WEEK) }
    var pickerForSlot by remember { mutableStateOf<MealType?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        DayPicker(
            mode = mode,
            onModeChange = { mode = it },
            selectedDay = selectedDay,
            onSelectDay = viewModel::selectDay,
            anchorDay = anchorDay,
            onAnchorChange = viewModel::moveAnchor,
            plannedDays = plannedDays,
            today = viewModel.today,
        )
        HorizontalDivider()

        Text(
            text = "Planning " + formatDayHeading(selectedDay),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(state.slots.size) { index ->
                val slot = state.slots[index]
                SlotEditor(
                    slot = slot,
                    onAddFood = { pickerForSlot = slot.type },
                    onRemoveFood = viewModel::removeSlotFood,
                    onClearSlot = { viewModel.clearSlot(slot.type) },
                )
            }
        }

        DayTotal(totalPaise = state.totalPaise)
    }

    val slotType = pickerForSlot
    if (slotType != null) {
        val assignable by viewModel.assignableFoods(slotType).collectAsState()
        FoodPickerSheet(
            slotType = slotType,
            foods = assignable,
            anyFoodsExist = anyFoodsExist,
            onDismiss = { pickerForSlot = null },
            onPick = { food ->
                viewModel.assignFood(slotType, food)
                pickerForSlot = null
            },
        )
    }
}

@Composable
private fun SlotEditor(
    slot: PlanSlot,
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
            if (!slot.isEmpty && !slot.isFullyHomecooked) {
                Text(
                    text = formatPaise(slot.costPaise),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                SlotFoodRow(food = food, onRemove = { onRemoveFood(food.entryId) })
            }
        }

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
                    Text(text = "Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun SlotFoodRow(food: SlotFood, onRemove: () -> Unit) {
    val isOutside = food.source == MealPlace.OUT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
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
        TextButton(onClick = onRemove) {
            Text(text = "✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
