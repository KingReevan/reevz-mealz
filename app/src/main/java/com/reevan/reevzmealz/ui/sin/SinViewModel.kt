package com.reevan.reevzmealz.ui.sin

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.SinDao
import com.reevan.reevzmealz.data.SinSettings
import com.reevan.reevzmealz.util.addMonths
import com.reevan.reevzmealz.util.startOfDay
import com.reevan.reevzmealz.util.startOfMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SinUiState(
    val status: SinStatus = SinStatus(allowance = DEFAULT_SIN_ALLOWANCE, used = 0),
    val todayEnded: Boolean = false,
    val sinnedToday: Set<MealType> = emptySet(),
    val allowanceEditable: Boolean = true,
    val daysUntilEditable: Int = 0,
    val loaded: Boolean = false,
)

/**
 * Sin state for the whole app: the monthly count shown in the top bar, the End Day judgement, and
 * the allowance setting.
 *
 * Shared rather than per-screen — `viewModel()` resolves to the activity's store, so the shell,
 * Today and Settings all read the same instance and cannot disagree.
 */
class SinViewModel(private val sinDao: SinDao) : ViewModel() {

    val today: Long = startOfDay(System.currentTimeMillis())

    private val monthStart: Long = startOfMonth(today)
    private val monthEndExclusive: Long = addMonths(today, 1)

    val uiState: StateFlow<SinUiState> =
        combine(
            sinDao.observeSinsUsed(monthStart, monthEndExclusive),
            sinDao.observeIsDayEnded(today),
            sinDao.observeSinnedTypes(today),
            sinDao.observeSettings(),
        ) { used, ended, sinnedToday, settings ->
            val now = System.currentTimeMillis()
            val allowance = settings?.monthlyAllowance ?: DEFAULT_SIN_ALLOWANCE
            SinUiState(
                status = SinStatus(allowance = allowance, used = used),
                todayEnded = ended,
                sinnedToday = sinnedToday.toSet(),
                allowanceEditable = canEditAllowance(settings?.setAt, now),
                daysUntilEditable = daysUntilUnlock(settings?.setAt, now),
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SinUiState(),
        )

    /** Just the remaining count, for the top-bar badge. */
    val remaining: StateFlow<Int> =
        uiState
            .map { it.status.remaining }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_SIN_ALLOWANCE,
            )

    /**
     * Settles today. [sinnedTypes] are the slots that did not go to plan — one sin each.
     * A day already ended is left untouched.
     */
    fun endDay(sinnedTypes: Set<MealType>) {
        viewModelScope.launch {
            sinDao.endDay(
                dayStart = today,
                sinned = MealType.entries.filter { sinnedTypes.contains(it) },
                endedAt = System.currentTimeMillis(),
            )
        }
    }

    /**
     * Sets the monthly allowance and starts the 3-day lock. Silently ignored while locked, so a
     * stale screen cannot bypass it.
     */
    fun setAllowance(value: Int) {
        val state = uiState.value
        if (!state.allowanceEditable) return
        val clamped = value.coerceIn(0, MAX_ALLOWANCE)
        viewModelScope.launch {
            sinDao.upsertSettings(
                SinSettings(
                    monthlyAllowance = clamped,
                    setAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_ALLOWANCE = 999

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SinViewModel(MealDatabase.getInstance(application).sinDao())
            }
        }
    }
}
