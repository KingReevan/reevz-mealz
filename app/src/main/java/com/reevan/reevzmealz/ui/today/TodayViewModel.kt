package com.reevan.reevzmealz.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.EatenMeal
import com.reevan.reevzmealz.data.EatenMealDao
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.FoodDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedMealDao
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.effectiveSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import com.reevan.reevzmealz.util.startOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    /** What the day actually amounts to: the eaten record if there is one, else the plan. */
    val slots: List<PlanSlot> = emptyList(),
    /** The original plan, kept for comparison once the day has been edited. */
    val plannedSlots: List<PlanSlot> = emptyList(),
    val totalPaise: Long = 0L,
    val plannedTotalPaise: Long = 0L,
    /** True once this day has its own record and no longer just mirrors the plan. */
    val dayLogged: Boolean = false,
    val loaded: Boolean = false,
) {
    val hasAnything: Boolean = slots.any { !it.isEmpty }

    /** Worth showing the plan's figure only once it disagrees with reality. */
    val showPlannedComparison: Boolean = dayLogged && totalPaise != plannedTotalPaise

    fun plannedFoodNames(type: MealType): List<String> =
        plannedSlots.firstOrNull { it.type == type }?.foods?.map { it.name }.orEmpty()
}

/**
 * Today's screen. Shows the current day's plan until the day is edited, then shows what was
 * actually eaten. Editing never touches the plan — Plan Meal keeps the original.
 */
class TodayViewModel(
    plannedMealDao: PlannedMealDao,
    private val eatenMealDao: EatenMealDao,
    foodDao: FoodDao,
) : ViewModel() {

    val dayStart: Long = startOfDay(System.currentTimeMillis())

    val uiState: StateFlow<TodayUiState> =
        combine(
            plannedMealDao.observeDay(dayStart),
            eatenMealDao.observeDay(dayStart),
            eatenMealDao.observeIsLogged(dayStart),
        ) { planned, eaten, logged ->
            val plannedSlots = buildSlots(planned)
            val slots = effectiveSlots(planned = planned, eaten = eaten, dayLogged = logged)
            TodayUiState(
                slots = slots,
                plannedSlots = plannedSlots,
                totalPaise = totalCostPaise(slots),
                plannedTotalPaise = totalCostPaise(plannedSlots),
                dayLogged = logged,
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TodayUiState(),
        )

    private val allFoods: StateFlow<List<Food>> =
        foodDao.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

    val anyFoodsExist: StateFlow<Boolean> =
        allFoods
            .map { it.isNotEmpty() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = false,
            )

    /** Foods addable to [type] today: everything not already in that slot. */
    fun addableFoods(type: MealType): StateFlow<List<Food>> =
        combine(allFoods, uiState) { foods, state ->
            val taken = state.slots.firstOrNull { it.type == type }
                ?.foods
                ?.map { it.foodId }
                ?.toSet()
                .orEmpty()
            foods.filterNot { taken.contains(it.id) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    /**
     * Called when Edit Mode is switched on. Seeds today's record from the plan the first time, so
     * editing starts from what was intended rather than from an empty day. Idempotent.
     */
    fun beginEditing() {
        viewModelScope.launch { eatenMealDao.startLoggingDay(dayStart) }
    }

    fun addFood(type: MealType, food: Food) {
        viewModelScope.launch {
            // Safe to repeat: seeding only happens once per day.
            eatenMealDao.startLoggingDay(dayStart)
            eatenMealDao.insert(
                EatenMeal(dayStart = dayStart, type = type, foodId = food.id),
            )
        }
    }

    fun removeFood(entryId: Long) {
        viewModelScope.launch {
            eatenMealDao.startLoggingDay(dayStart)
            eatenMealDao.deleteById(entryId)
        }
    }

    /** Records that nothing was eaten in this slot, which is not the same as never editing it. */
    fun clearSlot(type: MealType) {
        viewModelScope.launch {
            eatenMealDao.startLoggingDay(dayStart)
            eatenMealDao.clearSlot(dayStart, type)
        }
    }

    /** Throws away today's edits so the screen shows the plan again. */
    fun resetToPlan() {
        viewModelScope.launch { eatenMealDao.resetDayToPlan(dayStart) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val database = MealDatabase.getInstance(application)
                TodayViewModel(
                    plannedMealDao = database.plannedMealDao(),
                    eatenMealDao = database.eatenMealDao(),
                    foodDao = database.foodDao(),
                )
            }
        }
    }
}
