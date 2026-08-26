package com.reevan.reevzmealz.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.Meal
import com.reevan.reevzmealz.data.MealDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.util.dayRangeOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val meals: List<Meal> = emptyList(),
    val totalPaise: Long = 0L,
    val mealsOutCount: Int = 0,
    val loaded: Boolean = false,
)

class TodayViewModel(private val dao: MealDao) : ViewModel() {

    private val today = dayRangeOf(System.currentTimeMillis())

    /** Midnight of the day being shown, for the screen heading. */
    val dayStart: Long = today.start

    val uiState: StateFlow<TodayUiState> =
        dao.observeInRange(today.start, today.endExclusive)
            .map { meals ->
                TodayUiState(
                    meals = meals,
                    totalPaise = meals.sumOf { it.costPaise.toLong() },
                    mealsOutCount = meals.count { it.place == MealPlace.OUT },
                    loaded = true,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = TodayUiState(),
            )

    fun addMeal(
        name: String,
        type: MealType,
        place: MealPlace,
        costPaise: Int,
        notes: String?,
    ) {
        viewModelScope.launch {
            dao.insert(
                Meal(
                    name = name.trim(),
                    type = type,
                    place = place,
                    costPaise = costPaise,
                    eatenAt = System.currentTimeMillis(),
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                TodayViewModel(MealDatabase.getInstance(application).mealDao())
            }
        }
    }
}
