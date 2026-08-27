package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Declaration order is the order slots appear everywhere in the UI: breakfast, lunch, snack,
 * dinner. Room stores enums by name, so reordering this is safe for existing rows.
 */
enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    SNACK("Snack"),
    DINNER("Dinner"),
}

enum class MealPlace(val label: String) {
    HOME("Home"),
    OUT("Out"),
}

/**
 * A meal logged ad hoc, from the original meal-logging flow.
 *
 * Superseded by [PlannedMeal]: Today now shows the day's plan rather than typed-in entries, and
 * nothing in the UI reads this table any more. It stays declared on the database so the table and
 * any rows already in it are preserved — dropping it would destroy user data.
 */
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: MealType,
    val place: MealPlace,
    /** Cost in paise. Stored as an integer so money is never subject to float rounding. */
    val costPaise: Int,
    /** When the meal was eaten, as epoch millis. */
    val eatenAt: Long,
    val notes: String? = null,
)
