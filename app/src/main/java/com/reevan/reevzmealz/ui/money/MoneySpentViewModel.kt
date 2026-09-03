package com.reevan.reevzmealz.ui.money

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.SpendDao
import com.reevan.reevzmealz.data.SpendEntry
import com.reevan.reevzmealz.util.startOfDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The period Money Spent opens on. Day, because "what did today cost" is the everyday question. */
private val DEFAULT_PERIOD = SpendPeriod.DAY

data class MoneySpentUiState(
    val period: SpendPeriod = DEFAULT_PERIOD,
    val anchor: Long = 0L,
    val label: String = "",
    val boughtItems: List<BoughtItem> = emptyList(),
    val outsideFoods: List<SpendEntry> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isCurrent: Boolean = true,
    val loaded: Boolean = false,
) {
    /** Totals are summed from the item lists, so the headline can never disagree with them. */
    val boughtItemsPaise: Long = boughtItems.sumOf { it.pricePaise.toLong() }
    val outsideFoodPaise: Long = outsideFoods.sumOf { it.pricePaise.toLong() }
    val totalPaise: Long = boughtItemsPaise + outsideFoodPaise

    /** Bought Items' share of the total, for the split bar. A zero total has nothing to split. */
    val boughtItemsShare: Float =
        if (totalPaise == 0L) 0f else boughtItemsPaise.toFloat() / totalPaise.toFloat()

    val isEmpty: Boolean = boughtItems.isEmpty() && outsideFoods.isEmpty()

    /**
     * The breakdown as one list grouped by date, newest first, both streams merged.
     *
     * Derived here from the same two lists the totals come from, so a section's figure and the
     * headline cannot drift apart.
     */
    val days: List<DaySection> = groupByDay(spendLines(outsideFoods, boughtItems))
}

/**
 * Money Spent: what a chosen day, week, month or year cost, split into groceries bought and food
 * eaten outside, with the individual items behind it.
 *
 * Opens on **today**, and switching granularity jumps to the current week/month/year rather than
 * keeping wherever you had browsed to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoneySpentViewModel(private val spendDao: SpendDao) : ViewModel() {

    /**
     * Today, as state rather than captured once: it bounds forward navigation and caps the
     * counted range, both of which would be wrong after midnight with the app left open.
     */
    private val _now = MutableStateFlow(startOfDay(System.currentTimeMillis()))

    private val _period = MutableStateFlow(DEFAULT_PERIOD)
    private val _anchor = MutableStateFlow(normalise(DEFAULT_PERIOD, _now.value))

    val uiState: StateFlow<MoneySpentUiState> =
        combine(_period, _anchor, _now) { period, anchor, now -> Triple(period, anchor, now) }
            .flatMapLatest { (period, anchor, now) ->
                // Never count days that have not happened yet.
                val counted = countedRange(period, anchor, now)
                val details = if (counted == null) {
                    flowOf(emptyList<BoughtItem>() to emptyList<SpendEntry>())
                } else {
                    combine(
                        spendDao.observeBoughtItemsIn(counted.start, counted.endExclusive),
                        spendDao.observeOutsideFoodsIn(
                            start = counted.start,
                            endExclusive = counted.endExclusive,
                            outside = MealPlace.OUT,
                        ),
                    ) { bought, outside -> bought to outside }
                }
                details.map { (bought, outside) ->
                    MoneySpentUiState(
                        period = period,
                        anchor = anchor,
                        label = labelOf(period, anchor),
                        boughtItems = bought,
                        outsideFoods = outside,
                        canGoBack = canGoBack(period, anchor, now),
                        canGoForward = canGoForward(period, anchor, now),
                        isCurrent = anchor == normalise(period, now),
                        loaded = true,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MoneySpentUiState(
                    anchor = normalise(DEFAULT_PERIOD, _now.value),
                    label = labelOf(DEFAULT_PERIOD, _now.value),
                ),
            )

    /**
     * Switching granularity always jumps to the **current** day, week, month or year.
     *
     * It used to keep the anchor and merely snap it to the new period's start, so browsing back to
     * March and then tapping Day showed 1 March. Asking for "Week" means this week.
     */
    fun selectPeriod(period: SpendPeriod) {
        _period.value = period
        _anchor.value = normalise(period, _now.value)
    }

    /** Jumps back to the current period without changing granularity. */
    fun goToCurrent() {
        _anchor.value = normalise(_period.value, _now.value)
    }

    /**
     * Moves "today" if the app was left open across midnight.
     *
     * If the view was sitting on the current period, it follows the clock into the new one —
     * otherwise "Day" would still be showing yesterday after midnight.
     */
    fun refreshNow() {
        val today = startOfDay(System.currentTimeMillis())
        if (_now.value == today) return
        val wasOnCurrent = _anchor.value == normalise(_period.value, _now.value)
        _now.value = today
        if (wasOnCurrent) {
            _anchor.value = normalise(_period.value, today)
        }
    }

    fun goBack() {
        val period = _period.value
        if (canGoBack(period, _anchor.value, _now.value)) {
            _anchor.value = normalise(period, shift(period, _anchor.value, -1))
        }
    }

    fun goForward() {
        val period = _period.value
        if (canGoForward(period, _anchor.value, _now.value)) {
            _anchor.value = normalise(period, shift(period, _anchor.value, 1))
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                MoneySpentViewModel(MealDatabase.getInstance(application).spendDao())
            }
        }
    }
}
