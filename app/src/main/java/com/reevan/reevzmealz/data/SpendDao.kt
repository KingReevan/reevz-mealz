package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Read-only spending totals, for the Money Spent section. */
@Dao
interface SpendDao {

    /** What was spent in Bought Items within [start, endExclusive). */
    @Query(
        "SELECT COALESCE(SUM(pricePaise), 0) FROM bought_items " +
            "WHERE boughtAt >= :start AND boughtAt < :endExclusive"
    )
    fun observeBoughtItemsTotal(start: Long, endExclusive: Long): Flow<Long>

    /**
     * What was spent on food bought outside within [start, endExclusive).
     *
     * Mirrors the rule Today uses: a day with its own eaten record counts that record, a day
     * without one falls back to its plan. Homecooked food is excluded by [outside] rather than by
     * a null price, so a stray price cannot leak into the total.
     */
    @Query(
        "SELECT COALESCE((" +
            "SELECT SUM(f.pricePaise) FROM eaten_meals em " +
            "INNER JOIN foods f ON f.id = em.foodId " +
            "WHERE em.dayStart >= :start AND em.dayStart < :endExclusive " +
            "AND f.source = :outside" +
            "), 0) + COALESCE((" +
            "SELECT SUM(f.pricePaise) FROM planned_meals pm " +
            "INNER JOIN foods f ON f.id = pm.foodId " +
            "WHERE pm.dayStart >= :start AND pm.dayStart < :endExclusive " +
            "AND f.source = :outside " +
            "AND pm.dayStart NOT IN (SELECT dayStart FROM eaten_days)" +
            "), 0)"
    )
    fun observeOutsideFoodTotal(
        start: Long,
        endExclusive: Long,
        outside: MealPlace,
    ): Flow<Long>
}
