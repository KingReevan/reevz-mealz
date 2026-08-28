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
 * - v6: added `sin_events`, `ended_days` and `sin_settings`
 * - v7: added `app_settings`
 * - v8: added `foods.place`
 *
 * Version bumps must add a migration here. Adding a table, or a nullable column, is purely
 * additive, so Room generates the migration from the exported schemas in `app/schemas/`. Never use
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
        SinEvent::class,
        EndedDay::class,
        SinSettings::class,
        AppSettings::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
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

    abstract fun sinDao(): SinDao

    abstract fun appSettingsDao(): AppSettingsDao

    /** Read-only totals for Money Spent. No entities of its own, so no schema impact. */
    abstract fun spendDao(): SpendDao

    /** Enforces the 12-month retention window. Deletes; see the DAO's warning. */
    abstract fun maintenanceDao(): MaintenanceDao

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
