package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}

enum class MealPlace(val label: String) {
    HOME("Home"),
    OUT("Out"),
}

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
