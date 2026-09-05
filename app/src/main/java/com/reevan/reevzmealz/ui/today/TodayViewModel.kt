package com.reevan.reevzmealz.ui.today

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.EatenMealDao
import com.reevan.reevzmealz.data.Food
import com.reevan.reevzmealz.data.FoodDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedMealDao
import com.reevan.reevzmealz.data.foodOf
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.effectiveSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import com.reevan.reevzmealz.util.startOfDay
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
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val plannedMealDao: PlannedMealDao,
    private val eatenMealDao: EatenMealDao,
    private val foodDao: FoodDao,
) : ViewModel() {

    /**
     * The day being shown. Held in state rather than captured once, so [refreshDay] can move it
     * when the app is left open across midnight.
     */
    private val _dayStart = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val dayStart: StateFlow<Long> = _dayStart.asStateFlow()

    /** Which slot the food picker is open for, or null. Held here so the flow below is built once. */
    private val _pickerSlot = MutableStateFlow<MealType?>(null)
    val pickerSlot: StateFlow<MealType?> = _pickerSlot.asStateFlow()

    val uiState: StateFlow<TodayUiState> =
        _dayStart
            .flatMapLatest { day ->
                combine(
                    plannedMealDao.observeDay(day),
                    eatenMealDao.observeDay(day),
                    eatenMealDao.observeIsLogged(day),
                ) { planned, eaten, logged ->
                    val plannedSlots = buildSlots(planned)
                    val slots = effectiveSlots(
                        planned = planned,
                        eaten = eaten,
                        dayLogged = logged,
                    )
                    TodayUiState(
                        slots = slots,
                        plannedSlots = plannedSlots,
                        totalPaise = totalCostPaise(slots),
                        plannedTotalPaise = totalCostPaise(plannedSlots),
                        dayLogged = logged,
                        loaded = true,
                    )
                }
            }
            .stateIn(
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

    /**
     * Foods addable to the open slot: everything not already in it.
     *
     * One flow built once, driven by [_pickerSlot] — not a function returning a fresh `stateIn`,
     * which would start a new sharing coroutine on every recomposition.
     */
    /**
     * Every food, including ones the slot already holds.
     *
     * They used to be filtered out, which is precisely what made a second helping impossible:
     * the food you wanted twice was the one food missing from the list. Picking it again now
     * raises its quantity, so nothing needs hiding.
     */
    val addableFoods: StateFlow<List<Food>> = allFoods

    /** Moves to the real current day if it has changed. Called when the screen resumes. */
    fun refreshDay() {
        val today = startOfDay(System.currentTimeMillis())
        if (_dayStart.value != today) {
            _dayStart.value = today
        }
    }

    fun openPicker(type: MealType) {
        _pickerSlot.value = type
    }

    fun closePicker() {
        _pickerSlot.value = null
    }

    /**
     * Called when Edit Mode is switched on. Seeds today's record from the plan the first time, so
     * editing starts from what was intended rather than from an empty day. Idempotent.
     */
    fun beginEditing() {
        val day = _dayStart.value
        viewModelScope.launch { eatenMealDao.startLoggingDay(day) }
    }

    /**
     * Creates a food that did not exist yet and puts it straight into the slot.
     *
     * The picker can reach a restaurant or a dish that has no row yet, and the useful thing to do
     * then is record it for good rather than send the user to the Foods section and back. Both
     * writes share one coroutine so the row cannot be created without being added.
     */
    fun createAndAddFood(
        type: MealType,
        name: String,
        source: MealPlace,
        pricePaise: Int?,
        place: String?,
    ) {
        val day = _dayStart.value
        viewModelScope.launch {
            val id = foodDao.insert(
                foodOf(name = name, source = source, pricePaise = pricePaise, place = place),
            )
            eatenMealDao.startLoggingDay(day)
            eatenMealDao.addOne(dayStart = day, type = type, foodId = id)
        }
    }

    /**
     * Records one helping of [food]. Adding the same food again raises its quantity rather than
     * doing nothing, which is what a second kebab is.
     */
    fun addFood(type: MealType, food: Food) {
        val day = _dayStart.value
        viewModelScope.launch {
            // Safe to repeat: seeding only happens once per day.
            eatenMealDao.startLoggingDay(day)
            eatenMealDao.addOne(dayStart = day, type = type, foodId = food.id)
        }
    }

    /**
     * Removes one eaten entry.
     *
     * [entryId] must come from the eaten record, so the caller may only offer this once the day is
     * logged — before that the rows on screen are plan rows and their ids belong to another table.
     */
    fun removeFood(entryId: Long) {
        viewModelScope.launch { eatenMealDao.deleteById(entryId) }
    }

    /** Throws away today's edits so the screen shows the plan again. */
    fun resetToPlan() {
        val day = _dayStart.value
        viewModelScope.launch { eatenMealDao.resetDayToPlan(day) }
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
