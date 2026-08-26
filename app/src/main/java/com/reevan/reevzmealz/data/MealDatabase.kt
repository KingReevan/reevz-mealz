package com.reevan.reevzmealz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Meal::class], version = 1, exportSchema = true)
abstract class MealDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

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
