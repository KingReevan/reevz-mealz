package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An atomic food item: the building block used to plan a day's meals.
 *
 * [source] reuses [MealPlace] rather than declaring a parallel HOME/OUTSIDE enum, because it is
 * the same distinction a meal carries — a food bought outside becomes a meal eaten out. The
 * Foods UI labels it "Homecooked" / "Outside".
 *
 * [pricePaise] is null for homecooked food: a price is only meaningful when the item is bought.
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val source: MealPlace,
    /** Price in paise, or null when [source] is [MealPlace.HOME]. */
    val pricePaise: Int? = null,
)
