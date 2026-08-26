package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    /** Meals eaten within the half-open range [start, endExclusive), oldest first. */
    @Query(
        "SELECT * FROM meals WHERE eatenAt >= :start AND eatenAt < :endExclusive " +
            "ORDER BY eatenAt ASC"
    )
    fun observeInRange(start: Long, endExclusive: Long): Flow<List<Meal>>

    @Insert
    suspend fun insert(meal: Meal): Long
}
