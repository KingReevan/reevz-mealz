package com.reevan.reevzmealz.ui.plan

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.FoodDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedMeal
import com.reevan.reevzmealz.data.PlannedMealDao
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import com.reevan.reevzmealz.util.addMonths
import com.reevan.reevzmealz.util.startOfDay
import com.reevan.reevzmealz.util.startOfMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val slots: List<PlanSlot> = emptyList(),
    val totalPaise: Long = 0L,
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanMealViewModel(
    private val plannedMealDao: PlannedMealDao,
    foodDao: FoodDao,
) : ViewModel() {

    private val _today = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val today: StateFlow<Long> = _today.asStateFlow()

    private val _selectedDay = MutableStateFlow(_today.value)
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    /** Which month or week the picker is showing; moves independently of the selected day. */
    private val _anchorDay = MutableStateFlow(_today.value)
    val anchorDay: StateFlow<Long> = _anchorDay.asStateFlow()

    val uiState: StateFlow<PlanUiState> =
        _selectedDay
            .flatMapLatest { day -> plannedMealDao.observeDay(day) }
            .map { planned ->
                val slots = buildSlots(planned)
                PlanUiState(
                    slots = slots,
                    totalPaise = totalCostPaise(slots),
                    loaded = true,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = PlanUiState(),
            )

    /**
     * Days with a plan, over a window generously spanning the visible month so both picker modes
     * are covered without re-querying as the week strip moves inside a month.
     */
    val plannedDays: StateFlow<Set<Long>> =
        _anchorDay
            .flatMapLatest { anchor ->
                plannedMealDao.observePlannedDays(
                    start = addMonths(startOfMonth(anchor), -1),
                    endExclusive = addMonths(startOfMonth(anchor), 2),
                )
            }
            .map { it.toSet() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet(),
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

    /** Which slot the food picker is open for, or null. */
    private val _pickerSlot = MutableStateFlow<MealType?>(null)
    val pickerSlot: StateFlow<MealType?> = _pickerSlot.asStateFlow()

    /**
     * Foods assignable to the open slot: everything not already in it.
     *
     * One flow built once, driven by [_pickerSlot] — not a function returning a fresh `stateIn`,
     * which would start a new sharing coroutine on every recomposition.
     */
    val assignableFoods: StateFlow<List<Food>> =
        combine(allFoods, uiState, _pickerSlot) { foods, state, slot ->
            if (slot == null) {
                emptyList()
            } else {
                val taken = state.slots.firstOrNull { it.type == slot }
                    ?.foods
                    ?.map { it.foodId }
                    ?.toSet()
                    .orEmpty()
                foods.filterNot { taken.contains(it.id) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    fun openPicker(type: MealType) {
        _pickerSlot.value = type
    }

    fun closePicker() {
        _pickerSlot.value = null
    }

    /** Moves the "today" marker if the app was left open across midnight. */
    fun refreshToday() {
        val today = startOfDay(System.currentTimeMillis())
        if (_today.value != today) {
            _today.value = today
        }
    }

    fun selectDay(dayStart: Long) {
        _selectedDay.value = startOfDay(dayStart)
    }

    fun moveAnchor(dayStart: Long) {
        _anchorDay.value = startOfDay(dayStart)
    }

    fun assignFood(type: MealType, food: Food) {
        val day = _selectedDay.value
        viewModelScope.launch {
            plannedMealDao.insert(
                PlannedMeal(dayStart = day, type = type, foodId = food.id),
            )
        }
    }

    fun removeSlotFood(entryId: Long) {
        viewModelScope.launch { plannedMealDao.deleteById(entryId) }
    }

    fun clearSlot(type: MealType) {
        val day = _selectedDay.value
        viewModelScope.launch { plannedMealDao.clearSlot(day, type) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val database = MealDatabase.getInstance(application)
                PlanMealViewModel(database.plannedMealDao(), database.foodDao())
            }
        }
    }
}
