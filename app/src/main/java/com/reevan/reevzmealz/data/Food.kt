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
 * [pricePaise] and [place] are null for homecooked food: neither a price nor a shop is meaningful
 * for something cooked at home.
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val source: MealPlace,
    /** Price in paise, or null when [source] is [MealPlace.HOME]. */
    val pricePaise: Int? = null,
    /**
     * Where the food was bought, or null when [source] is [MealPlace.HOME].
     *
     * Free text rather than a table of shops: it is a note to jog the memory, not something that
     * needs to reconcile across foods. Optional even for outside food, so a forgotten shop name
     * cannot block saving.
     */
    val place: String? = null,
)
