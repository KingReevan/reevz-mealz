package com.reevan.reevzmealz.ui.bought

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.data.BoughtItemDao
import com.reevan.reevzmealz.data.MealDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BoughtItemsUiState(
    val months: List<MonthSection> = emptyList(),
    val loaded: Boolean = false,
)

class BoughtItemsViewModel(private val dao: BoughtItemDao) : ViewModel() {

    val uiState: StateFlow<BoughtItemsUiState> =
        dao.observeAll()
            .map { items -> BoughtItemsUiState(months = groupByMonth(items), loaded = true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = BoughtItemsUiState(),
            )

    /**
     * Creates the purchase when [id] is null, otherwise updates it in place.
     *
     * A new purchase is stamped with the current time. Editing keeps the original [boughtAt] so
     * fixing a typo never silently moves an item into a different month.
     */
    fun save(id: Long?, name: String, pricePaise: Int, boughtAt: Long?) {
        val item = BoughtItem(
            id = id ?: 0L,
            name = name.trim(),
            pricePaise = pricePaise,
            boughtAt = boughtAt ?: System.currentTimeMillis(),
        )
        viewModelScope.launch {
            if (id == null) dao.insert(item) else dao.update(item)
        }
    }

    fun delete(item: BoughtItem) {
        viewModelScope.launch { dao.delete(item) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                BoughtItemsViewModel(MealDatabase.getInstance(application).boughtItemDao())
            }
        }
    }
}
