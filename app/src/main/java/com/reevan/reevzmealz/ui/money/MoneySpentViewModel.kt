package com.reevan.reevzmealz.ui.money

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.SpendDao
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

data class MoneySpentUiState(
    val period: SpendPeriod = SpendPeriod.MONTH,
    val anchor: Long = 0L,
    val label: String = "",
    val boughtItemsPaise: Long = 0L,
    val outsideFoodPaise: Long = 0L,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val loaded: Boolean = false,
) {
    val totalPaise: Long = boughtItemsPaise + outsideFoodPaise

    /** Bought Items' share of the total, for the split bar. A zero total has nothing to split. */
    val boughtItemsShare: Float =
        if (totalPaise == 0L) 0f else boughtItemsPaise.toFloat() / totalPaise.toFloat()
}

/**
 * Money Spent: what a chosen day, week, month or year cost, split into groceries bought and food
 * eaten outside. Defaults to the current month.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoneySpentViewModel(private val spendDao: SpendDao) : ViewModel() {

    private val now: Long = startOfDay(System.currentTimeMillis())

    private val _period = MutableStateFlow(SpendPeriod.MONTH)
    private val _anchor = MutableStateFlow(normalise(SpendPeriod.MONTH, now))

    val uiState: StateFlow<MoneySpentUiState> =
        combine(_period, _anchor) { period, anchor -> period to anchor }
            .flatMapLatest { (period, anchor) ->
                // Never count days that have not happened yet.
                val counted = countedRange(period, anchor, now)
                val totals = if (counted == null) {
                    flowOf(0L to 0L)
                } else {
                    combine(
                        spendDao.observeBoughtItemsTotal(counted.start, counted.endExclusive),
                        spendDao.observeOutsideFoodTotal(
                            start = counted.start,
                            endExclusive = counted.endExclusive,
                            outside = MealPlace.OUT,
                        ),
                    ) { bought, outside -> bought to outside }
                }
                totals.map { (bought, outside) ->
                    MoneySpentUiState(
                        period = period,
                        anchor = anchor,
                        label = labelOf(period, anchor),
                        boughtItemsPaise = bought,
                        outsideFoodPaise = outside,
                        canGoBack = canGoBack(period, anchor, now),
                        canGoForward = canGoForward(period, anchor, now),
                        loaded = true,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = MoneySpentUiState(
                    anchor = normalise(SpendPeriod.MONTH, now),
                    label = labelOf(SpendPeriod.MONTH, now),
                ),
            )

    /** Switching granularity keeps you in the same moment, snapped to that period's start. */
    fun selectPeriod(period: SpendPeriod) {
        val current = _anchor.value
        _period.value = period
        _anchor.value = normalise(period, current)
    }

    fun goBack() {
        val period = _period.value
        if (canGoBack(period, _anchor.value, now)) {
            _anchor.value = normalise(period, shift(period, _anchor.value, -1))
        }
    }

    fun goForward() {
        val period = _period.value
        if (canGoForward(period, _anchor.value, now)) {
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
