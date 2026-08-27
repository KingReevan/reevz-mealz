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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.PlannedFood
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatPaise

/**
 * The day's plan: all four meal slots with what is planned in each, and the day's total pinned
 * at the bottom.
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onGoToPlan: () -> Unit = {},
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = formatDayHeading(viewModel.dayStart),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            if (state.loaded && !state.hasAnything) {
                NothingPlanned(onGoToPlan = onGoToPlan)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.slots.size) { index ->
                        SlotBlock(slot = state.slots[index])
                    }
                }
            }
        }

        DayTotal(totalPaise = state.totalPaise)
    }
}

@Composable
private fun SlotBlock(slot: PlanSlot) {
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

        if (slot.isEmpty) {
            Text(
                text = "No food",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            slot.foods.forEach { food ->
                PlannedFoodRow(food)
            }
        }
    }
}

@Composable
private fun PlannedFoodRow(food: PlannedFood) {
    val isOutside = food.source == MealPlace.OUT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
    }
}

/** Pinned footer: the day's planned spend, which is the number worth watching. */
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
                text = "Total",
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

@Composable
private fun NothingPlanned(onGoToPlan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
        }
    }
}
