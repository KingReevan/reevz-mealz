package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedMealDao {

    /**
     * The foods planned for one day, joined with their name, source and price.
     * Ordered by name so a slot's contents stay stable as foods are added and removed.
     */
    @Query(
        "SELECT pm.id AS entryId, pm.type AS type, f.id AS foodId, f.name AS name, " +
            "f.source AS source, f.pricePaise AS pricePaise, pm.quantity AS quantity " +
            "FROM planned_meals pm INNER JOIN foods f ON f.id = pm.foodId " +
            "WHERE pm.dayStart = :dayStart " +
            "ORDER BY f.name COLLATE NOCASE ASC"
    )
    fun observeDay(dayStart: Long): Flow<List<SlotFood>>

    /** Days in [start, endExclusive) that have anything planned, for marking the day picker. */
    @Query(
        "SELECT DISTINCT dayStart FROM planned_meals " +
            "WHERE dayStart >= :start AND dayStart < :endExclusive"
    )
    fun observePlannedDays(start: Long, endExclusive: Long): Flow<List<Long>>

    /** How many foods are planned for one day. Used by the nightly reminder check. */
    @Query("SELECT COUNT(*) FROM planned_meals WHERE dayStart = :dayStart")
    suspend fun countForDay(dayStart: Long): Int

    /** Inserting a food already in the slot is ignored; [addOne] raises its quantity instead. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(plannedMeal: PlannedMeal): Long

    /**
     * Plans one more of a food: raises the quantity if the slot already holds it, otherwise adds
     * it with a quantity of 1. See `EatenMealDao.addOne` — the same shape, on the plan side.
     */
    @Transaction
    suspend fun addOne(dayStart: Long, type: MealType, foodId: Long) {
        val raised = raiseQuantity(dayStart, type, foodId)
        if (raised == 0) {
            insert(PlannedMeal(dayStart = dayStart, type = type, foodId = foodId))
        }
    }

    /** Returns the number of rows changed, so the caller knows whether to insert instead. */
    @Query(
        "UPDATE planned_meals SET quantity = quantity + 1 " +
            "WHERE dayStart = :dayStart AND type = :type AND foodId = :foodId"
    )
    suspend fun raiseQuantity(dayStart: Long, type: MealType, foodId: Long): Int

    @Query("DELETE FROM planned_meals WHERE id = :plannedMealId")
    suspend fun deleteById(plannedMealId: Long)

    /** Empties one slot on one day. */
    @Query("DELETE FROM planned_meals WHERE dayStart = :dayStart AND type = :type")
    suspend fun clearSlot(dayStart: Long, type: MealType)
}
