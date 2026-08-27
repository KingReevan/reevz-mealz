package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EatenMealDao {

    /** The foods actually eaten on one day, joined with their name, source and price. */
    @Query(
        "SELECT em.id AS entryId, em.type AS type, f.id AS foodId, f.name AS name, " +
            "f.source AS source, f.pricePaise AS pricePaise " +
            "FROM eaten_meals em INNER JOIN foods f ON f.id = em.foodId " +
            "WHERE em.dayStart = :dayStart " +
            "ORDER BY f.name COLLATE NOCASE ASC"
    )
    fun observeDay(dayStart: Long): Flow<List<SlotFood>>

    /** Whether this day has its own record, as opposed to falling back to the plan. */
    @Query("SELECT COUNT(*) > 0 FROM eaten_days WHERE dayStart = :dayStart")
    fun observeIsLogged(dayStart: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(eatenMeal: EatenMeal): Long

    @Query("DELETE FROM eaten_meals WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("DELETE FROM eaten_meals WHERE dayStart = :dayStart AND type = :type")
    suspend fun clearSlot(dayStart: Long, type: MealType)

    /**
     * Begins a real record for [dayStart] by copying that day's plan into it, so editing starts
     * from what was intended rather than from nothing. Idempotent: a day already logged is left
     * exactly as it is, so this can be called freely.
     */
    @Transaction
    suspend fun startLoggingDay(dayStart: Long) {
        if (isLogged(dayStart)) return
        insertDay(EatenDay(dayStart = dayStart))
        plannedRows(dayStart).forEach { planned ->
            insert(
                EatenMeal(
                    dayStart = dayStart,
                    type = planned.type,
                    foodId = planned.foodId,
                ),
            )
        }
    }

    /** Discards the day's own record so it falls back to showing the plan again. */
    @Transaction
    suspend fun resetDayToPlan(dayStart: Long) {
        deleteAllForDay(dayStart)
        deleteDay(dayStart)
    }

    @Query("SELECT COUNT(*) > 0 FROM eaten_days WHERE dayStart = :dayStart")
    suspend fun isLogged(dayStart: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(eatenDay: EatenDay)

    @Query("SELECT * FROM planned_meals WHERE dayStart = :dayStart")
    suspend fun plannedRows(dayStart: Long): List<PlannedMeal>

    @Query("DELETE FROM eaten_meals WHERE dayStart = :dayStart")
    suspend fun deleteAllForDay(dayStart: Long)

    @Query("DELETE FROM eaten_days WHERE dayStart = :dayStart")
    suspend fun deleteDay(dayStart: Long)
}
