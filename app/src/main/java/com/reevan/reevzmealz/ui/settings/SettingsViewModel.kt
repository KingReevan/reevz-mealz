package com.reevan.reevzmealz.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.MaintenanceDao
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.data.PurgeCounts
import com.reevan.reevzmealz.util.retentionCutoff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    /** Non-null while the confirmation dialog is up, holding what would be deleted. */
    val pendingPurge: PurgeCounts? = null,
    /** Set after a purge runs, so the outcome is reported rather than silent. */
    val lastPurgeResult: PurgeCounts? = null,
)

/**
 * Settings. Currently only the retention control.
 *
 * The purge is a two-step action on purpose: counts are read and shown first, and the delete only
 * happens on explicit confirmation. It is permanent and the app has no export.
 */
class SettingsViewModel(private val maintenanceDao: MaintenanceDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val cutoff: Long = retentionCutoff(System.currentTimeMillis())

    /** Reads what a purge would remove and raises the confirmation. Deletes nothing. */
    fun requestPurge() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                pendingPurge = maintenanceDao.countOlderThan(cutoff),
                lastPurgeResult = null,
            )
        }
    }

    fun cancelPurge() {
        _uiState.value = _uiState.value.copy(pendingPurge = null)
    }

    /** Runs the delete. Only reachable from the confirmation dialog. */
    fun confirmPurge() {
        val counts = _uiState.value.pendingPurge ?: return
        viewModelScope.launch {
            maintenanceDao.purgeOlderThan(cutoff)
            _uiState.value = SettingsUiState(pendingPurge = null, lastPurgeResult = counts)
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(lastPurgeResult = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsViewModel(MealDatabase.getInstance(application).maintenanceDao())
            }
        }
    }
}
