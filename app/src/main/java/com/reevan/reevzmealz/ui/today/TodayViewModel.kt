package com.reevan.reevzmealz.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.PlannedMealDao
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import com.reevan.reevzmealz.util.startOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val slots: List<PlanSlot> = emptyList(),
    val totalPaise: Long = 0L,
    val loaded: Boolean = false,
) {
    val hasAnything: Boolean = slots.any { !it.isEmpty }
}

/** Today's screen shows the plan for the current day, made in the Plan Meal section. */
class TodayViewModel(dao: PlannedMealDao) : ViewModel() {

    val dayStart: Long = startOfDay(System.currentTimeMillis())

    val uiState: StateFlow<TodayUiState> =
        dao.observeDay(dayStart)
            .map { planned ->
                val slots = buildSlots(planned)
                TodayUiState(
                    slots = slots,
                    totalPaise = totalCostPaise(slots),
                    loaded = true,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = TodayUiState(),
            )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                TodayViewModel(MealDatabase.getInstance(application).plannedMealDao())
            }
        }
    }
}
