package com.reevan.reevzmealz.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzmealz.data.Meal
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatPaise
import com.reevan.reevzmealz.util.formatTimeOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Today · " + formatDayHeading(viewModel.dayStart)) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { sheetOpen = true }) {
                Text("Add meal")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            DailySummary(totalPaise = state.totalPaise, mealsOutCount = state.mealsOutCount)
            HorizontalDivider()

            if (state.loaded && state.meals.isEmpty()) {
                EmptyToday()
            } else {
                MealList(meals = state.meals)
            }
        }
    }

    if (sheetOpen) {
        AddMealSheet(
            onDismiss = { sheetOpen = false },
            onSave = { name, type, place, costPaise, notes ->
                viewModel.addMeal(name, type, place, costPaise, notes)
                sheetOpen = false
            },
        )
    }
}

@Composable
private fun DailySummary(totalPaise: Long, mealsOutCount: Int) {
    val outText = when (mealsOutCount) {
        0 -> "all home"
        1 -> "1 meal out"
        else -> mealsOutCount.toString() + " meals out"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatPaise(totalPaise),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "· " + outText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyToday() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nothing logged yet today.\nTap Add meal to start.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MealList(meals: List<Meal>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom padding leaves room for the FAB so the last row is never hidden behind it.
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
    ) {
        MealType.entries.forEach { type ->
            val ofType = meals.filter { it.type == type }
            if (ofType.isNotEmpty()) {
                item(key = "header-" + type.name) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
                items(items = ofType, key = { it.id }) { meal ->
                    MealRow(meal)
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: Meal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = meal.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = buildString {
                    append(meal.place.label)
                    append(" · ")
                    append(formatTimeOfDay(meal.eatenAt))
                    val note = meal.notes
                    if (note != null) {
                        append(" · ")
                        append(note)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (meal.place == MealPlace.OUT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = formatPaise(meal.costPaise.toLong()),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
