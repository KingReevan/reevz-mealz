package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedMealDao {

    /**
     * The foods planned for one day, joined with their name, source and price.
     * Ordered by name so a slot's contents stay stable as foods are added and removed.
     */
    @Query(
        "SELECT pm.id AS plannedMealId, pm.type AS type, f.id AS foodId, f.name AS name, " +
            "f.source AS source, f.pricePaise AS pricePaise " +
            "FROM planned_meals pm INNER JOIN foods f ON f.id = pm.foodId " +
            "WHERE pm.dayStart = :dayStart " +
            "ORDER BY f.name COLLATE NOCASE ASC"
    )
    fun observeDay(dayStart: Long): Flow<List<PlannedFood>>

    /** Days in [start, endExclusive) that have anything planned, for marking the day picker. */
    @Query(
        "SELECT DISTINCT dayStart FROM planned_meals " +
            "WHERE dayStart >= :start AND dayStart < :endExclusive"
    )
    fun observePlannedDays(start: Long, endExclusive: Long): Flow<List<Long>>

    /** Assigning a food already in the slot is a no-op rather than a constraint crash. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(plannedMeal: PlannedMeal): Long

    @Query("DELETE FROM planned_meals WHERE id = :plannedMealId")
    suspend fun deleteById(plannedMealId: Long)

    /** Empties one slot on one day. */
    @Query("DELETE FROM planned_meals WHERE dayStart = :dayStart AND type = :type")
    suspend fun clearSlot(dayStart: Long, type: MealType)
}
