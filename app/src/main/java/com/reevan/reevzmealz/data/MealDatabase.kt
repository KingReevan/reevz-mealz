package com.reevan.reevzmealz.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema history:
 * - v1: `meals`
 * - v2: added `foods`
 * - v3: added `bought_items`
 * - v4: added `planned_meals`
 * - v5: added `eaten_meals` and `eaten_days`
 *
 * Version bumps must add a migration here. Adding a table is purely additive, so Room generates
 * the migration from the exported schemas in `app/schemas/`. Never use
 * fallbackToDestructiveMigration — logged meals and spending are not recoverable.
 */
@Database(
    entities = [
        Meal::class,
        Food::class,
        BoughtItem::class,
        PlannedMeal::class,
        EatenMeal::class,
        EatenDay::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class MealDatabase : RoomDatabase() {

    /**
     * Backs the superseded ad-hoc meal log. Nothing in the UI uses it; it stays so the `meals`
     * table and its rows are preserved.
     */
    abstract fun mealDao(): MealDao

    abstract fun foodDao(): FoodDao

    abstract fun boughtItemDao(): BoughtItemDao

    abstract fun plannedMealDao(): PlannedMealDao

    abstract fun eatenMealDao(): EatenMealDao

    companion object {
        private const val NAME = "reevz-mealz.db"

        @Volatile
        private var instance: MealDatabase? = null

        fun getInstance(context: Context): MealDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MealDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
