package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One food assigned to one meal slot on one day — what was *planned*.
 *
 * A slot holds several foods, so this is a row per (day, slot, food) rather than a row per slot.
 *
 * [dayStart] is local midnight of the planned day, so equality is enough to group a day's rows —
 * no range scan needed.
 *
 * Deleting a food cascades: the food disappears from every plan that referenced it, rather than
 * leaving rows pointing at nothing.
 */
@Entity(
    tableName = "planned_meals",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dayStart", "type"]),
        Index(value = ["foodId"]),
        // The same food twice in one slot is a mis-tap, not a quantity of two.
        Index(value = ["dayStart", "type", "foodId"], unique = true),
    ],
)
data class PlannedMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Local midnight of the planned day. */
    val dayStart: Long,
    val type: MealType,
    val foodId: Long,
)

/**
 * A food sitting in a meal slot, joined with the food's own details — the name to show and the
 * price to add up. Used for both planned and eaten rows, so [entryId] is the id of whichever
 * row produced it.
 *
 * [pricePaise] is null for homecooked food — its cost is tracked through Bought Items instead.
 */
data class SlotFood(
    val entryId: Long,
    val type: MealType,
    val foodId: Long,
    val name: String,
    val source: MealPlace,
    val pricePaise: Int?,
)
