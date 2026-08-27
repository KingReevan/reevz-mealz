package com.reevan.reevzmealz.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.AppSettings
import com.reevan.reevzmealz.data.AppSettingsDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.ThemeMode
import com.reevan.reevzmealz.notify.PlanReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide preferences: theme and the nightly reminder.
 *
 * Shared between MainActivity (which needs the theme before anything renders) and Settings —
 * `viewModel()` resolves to the activity's store, so both see one instance.
 */
class PreferencesViewModel(
    private val application: Application,
    private val dao: AppSettingsDao,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        dao.observe()
            .map { it ?: AppSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppSettings(),
            )

    init {
        // Arm the alarm to match whatever is stored, including the very first run where nothing
        // has been saved yet and reminders default to on.
        viewModelScope.launch {
            val stored = dao.get() ?: AppSettings()
            PlanReminderScheduler.apply(
                context = application,
                enabled = stored.remindersEnabled,
                hour = stored.reminderHour,
                minute = stored.reminderMinute,
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        update { it.copy(themeMode = mode) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        update { it.copy(remindersEnabled = enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        update {
            it.copy(
                reminderHour = hour.coerceIn(0, 23),
                reminderMinute = minute.coerceIn(0, 59),
            )
        }
    }

    /** Writes the change and re-arms the alarm so the two never drift apart. */
    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = dao.get() ?: AppSettings()
            val updated = transform(current)
            dao.upsert(updated)
            PlanReminderScheduler.apply(
                context = application,
                enabled = updated.remindersEnabled,
                hour = updated.reminderHour,
                minute = updated.reminderMinute,
            )
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                PreferencesViewModel(
                    application = application,
                    dao = MealDatabase.getInstance(application).appSettingsDao(),
                )
            }
        }
    }
}
