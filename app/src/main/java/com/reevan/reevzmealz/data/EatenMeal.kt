package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One food actually eaten in one meal slot on one day.
 *
 * Deliberately a separate table from [PlannedMeal] rather than an edit of it: the plan is what
 * was intended and stays untouched, so what actually happened can be compared against it.
 */
@Entity(
    tableName = "eaten_meals",
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
        Index(value = ["dayStart", "type", "foodId"], unique = true),
    ],
)
data class EatenMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Local midnight of the day it was eaten. */
    val dayStart: Long,
    val type: MealType,
    val foodId: Long,
)

/**
 * Marks a day as having a real record of its own, distinct from its plan.
 *
 * Without this a day with no [EatenMeal] rows would be ambiguous: it could mean "not edited yet,
 * so assume the plan" or "edited, and I ate nothing". The presence of this row means the eaten
 * rows are authoritative even when there are none — which is how skipping a planned meal is
 * recorded, and it keeps a skipped meal from inflating the day's cost.
 */
@Entity(tableName = "eaten_days")
data class EatenDay(
    @PrimaryKey val dayStart: Long,
)
