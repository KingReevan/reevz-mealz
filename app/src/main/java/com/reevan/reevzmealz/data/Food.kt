package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reevan.reevzmealz.util.capitalizeWords

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

/**
 * Builds a [Food] from raw form input, applying the rules every entry point must obey.
 *
 * There are three ways into the foods table now — the Foods editor, and the picker's "new food"
 * step on Today and on Plan Meal — and each of them has to capitalise the same way and null out
 * the same fields. That is three chances to drift, so the rules live here instead:
 *
 * - **Name and place are capitalised on the way in.** The keyboard's `Words` capitalisation is
 *   only a hint to the IME; pasted text, swipe input and `adb shell input text` all slip past it,
 *   so this pass is what actually guarantees the stored value.
 * - **A homecooked food has no price and no place**, and null rather than zero or "" — so a price
 *   typed before the toggle flipped cannot leak through, and "nothing recorded" has exactly one
 *   representation.
 * - A blank place on outside food is stored as null for the same reason.
 */
fun foodOf(
    id: Long = 0L,
    name: String,
    source: MealPlace,
    pricePaise: Int?,
    place: String?,
): Food = Food(
    id = id,
    name = capitalizeWords(name.trim()),
    source = source,
    pricePaise = if (source == MealPlace.HOME) null else pricePaise,
    place = if (source == MealPlace.HOME) {
        null
    } else {
        place?.trim()?.ifBlank { null }?.let(::capitalizeWords)
    },
)
