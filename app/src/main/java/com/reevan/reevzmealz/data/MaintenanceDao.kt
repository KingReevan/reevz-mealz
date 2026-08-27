package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** How many rows a purge would remove, per table. */
data class PurgeCounts(
    val boughtItems: Int,
    val plannedMeals: Int,
    val eatenMeals: Int,
) {
    val total: Int = boughtItems + plannedMeals + eatenMeals
    val isEmpty: Boolean = total == 0
}

/**
 * Enforces the 12-month retention window.
 *
 * This is the only code in the app that deletes records the user did not ask to delete
 * individually, so it is deliberately manual: [countOlderThan] is shown for confirmation first,
 * and [purgeOlderThan] only runs afterwards. Deletion is permanent and there is no export.
 *
 * `foods` is never touched — a food is a definition with no date. Neither is the superseded
 * `meals` table, which is preserved from milestone 1.
 */
@Dao
interface MaintenanceDao {

    @Transaction
    suspend fun countOlderThan(cutoff: Long): PurgeCounts = PurgeCounts(
        boughtItems = countOldBoughtItems(cutoff),
        plannedMeals = countOldPlannedMeals(cutoff),
        eatenMeals = countOldEatenMeals(cutoff),
    )

    /** Permanently removes every dated spending record before [cutoff]. Not reversible. */
    @Transaction
    suspend fun purgeOlderThan(cutoff: Long) {
        deleteOldBoughtItems(cutoff)
        deleteOldPlannedMeals(cutoff)
        deleteOldEatenMeals(cutoff)
        deleteOldEatenDays(cutoff)
    }

    @Query("SELECT COUNT(*) FROM bought_items WHERE boughtAt < :cutoff")
    suspend fun countOldBoughtItems(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM planned_meals WHERE dayStart < :cutoff")
    suspend fun countOldPlannedMeals(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM eaten_meals WHERE dayStart < :cutoff")
    suspend fun countOldEatenMeals(cutoff: Long): Int

    @Query("DELETE FROM bought_items WHERE boughtAt < :cutoff")
    suspend fun deleteOldBoughtItems(cutoff: Long)

    @Query("DELETE FROM planned_meals WHERE dayStart < :cutoff")
    suspend fun deleteOldPlannedMeals(cutoff: Long)

    @Query("DELETE FROM eaten_meals WHERE dayStart < :cutoff")
    suspend fun deleteOldEatenMeals(cutoff: Long)

    @Query("DELETE FROM eaten_days WHERE dayStart < :cutoff")
    suspend fun deleteOldEatenDays(cutoff: Long)
}
