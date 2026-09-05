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
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedMealDao
import com.reevan.reevzmealz.data.foodOf
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
    private val foodDao: FoodDao,
) : ViewModel() {

    private val _today = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val today: StateFlow<Long> = _today.asStateFlow()

    private val _selectedDay = MutableStateFlow(_today.value)
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    /** Which month or week the picker is showing; moves independently of the selected day. */
    private val _anchorDay = MutableStateFlow(_today.value)
    val anchorDay: StateFlow<Long> = _anchorDay.asStateFlow()

    /**
     * Whether the selected day's plan may still be changed.
     *
     * Drives the screen, but is not what protects the table — see [selectedDayIsOpen].
     */
    val lock: StateFlow<PlanLock> =
        combine(_selectedDay, _today) { day, today -> planLock(day, today) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = planLock(_selectedDay.value, _today.value),
            )

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

    /** Which slot the food picker is open for, or null. */
    private val _pickerSlot = MutableStateFlow<MealType?>(null)
    val pickerSlot: StateFlow<MealType?> = _pickerSlot.asStateFlow()

    /**
     * Foods assignable to the open slot: everything not already in it.
     *
     * One flow built once, driven by [_pickerSlot] — not a function returning a fresh `stateIn`,
     * which would start a new sharing coroutine on every recomposition.
     */
    /**
     * Every food, including ones the slot already holds — picking one again raises its quantity.
     * See `TodayViewModel.addableFoods` for why nothing is filtered out any more.
     */
    val assignableFoods: StateFlow<List<Food>> = allFoods

    /**
     * Re-checks the lock against the clock, not against cached state.
     *
     * Every write goes through this rather than trusting the UI to have hidden its buttons. A
     * screen left open across midnight would otherwise still believe tomorrow is tomorrow, and
     * [refreshToday] here means that same check also drags the UI into the new day.
     */
    private fun selectedDayIsOpen(): Boolean {
        refreshToday()
        return planLock(_selectedDay.value, _today.value).isOpen
    }

    fun openPicker(type: MealType) {
        if (!selectedDayIsOpen()) return
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
            // The day just became today, so it can no longer be planned. Drop a picker left open
            // over midnight rather than leaving a sheet whose taps silently do nothing.
            if (!planLock(_selectedDay.value, today).isOpen) {
                _pickerSlot.value = null
            }
        }
    }

    fun selectDay(dayStart: Long) {
        _selectedDay.value = startOfDay(dayStart)
    }

    fun moveAnchor(dayStart: Long) {
        _anchorDay.value = startOfDay(dayStart)
    }

    /** Plans one helping; assigning the same food again raises its quantity. */
    fun assignFood(type: MealType, food: Food) {
        if (!selectedDayIsOpen()) return
        val day = _selectedDay.value
        viewModelScope.launch {
            plannedMealDao.addOne(dayStart = day, type = type, foodId = food.id)
        }
    }

    /**
     * Creates a food that did not exist yet and assigns it to the slot.
     *
     * Behind [selectedDayIsOpen] like every other write here: a locked day must not gain plan
     * rows, and it must not quietly create foods either.
     */
    fun createAndAssignFood(
        type: MealType,
        name: String,
        source: MealPlace,
        pricePaise: Int?,
        place: String?,
    ) {
        if (!selectedDayIsOpen()) return
        val day = _selectedDay.value
        viewModelScope.launch {
            val id = foodDao.insert(
                foodOf(name = name, source = source, pricePaise = pricePaise, place = place),
            )
            plannedMealDao.addOne(dayStart = day, type = type, foodId = id)
        }
    }

    fun removeSlotFood(entryId: Long) {
        if (!selectedDayIsOpen()) return
        viewModelScope.launch { plannedMealDao.deleteById(entryId) }
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
