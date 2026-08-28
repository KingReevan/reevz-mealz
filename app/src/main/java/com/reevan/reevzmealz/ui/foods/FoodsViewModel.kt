package com.reevan.reevzmealz.ui.foods

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoodsUiState(
    val foods: List<Food> = emptyList(),
    val loaded: Boolean = false,
)

class FoodsViewModel(private val dao: FoodDao) : ViewModel() {

    val uiState: StateFlow<FoodsUiState> =
        dao.observeAll()
            .map { FoodsUiState(foods = it, loaded = true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = FoodsUiState(),
            )

    /**
     * Creates the food when [id] is null, otherwise updates it in place.
     * A homecooked food is stored with no price and no place, whatever was typed before the
     * toggle flipped. A blank place is stored as null rather than an empty string, so "no place
     * recorded" has exactly one representation.
     */
    fun save(id: Long?, name: String, source: MealPlace, pricePaise: Int?, place: String?) {
        val food = Food(
            id = id ?: 0L,
            name = name.trim(),
            source = source,
            pricePaise = if (source == MealPlace.HOME) null else pricePaise,
            place = if (source == MealPlace.HOME) null else place?.trim()?.ifBlank { null },
        )
        viewModelScope.launch {
            if (id == null) dao.insert(food) else dao.update(food)
        }
    }

    fun delete(food: Food) {
        viewModelScope.launch { dao.delete(food) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                FoodsViewModel(MealDatabase.getInstance(application).foodDao())
            }
        }
    }
}
