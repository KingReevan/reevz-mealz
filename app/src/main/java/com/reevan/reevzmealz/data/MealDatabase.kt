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
 *
 * Version bumps must add a migration here. Adding a table is purely additive, so Room generates
 * the migration from the exported schemas in `app/schemas/`. Never use
 * fallbackToDestructiveMigration — logged meals and spending are not recoverable.
 */
@Database(
    entities = [Meal::class, Food::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MealDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

    abstract fun foodDao(): FoodDao

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
