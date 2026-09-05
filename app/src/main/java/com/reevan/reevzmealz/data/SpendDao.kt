package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One outside-food cost inside a period, for the Money Spent breakdown.
 *
 * [dayStart] is the day it belongs to, not a precise time: meals are recorded per day, so there is
 * no clock time to show.
 */
data class SpendEntry(
    val dayStart: Long,
    val name: String,
    /** The **unit** price. Multiply by [quantity] for what the line actually cost. */
    val pricePaise: Int,
    val place: String?,
    val quantity: Int = 1,
)

/** Read-only spending detail, for the Money Spent section. */
@Dao
interface SpendDao {

    /**
     * Every purchase inside [start, endExclusive), newest first.
     *
     * The period's total is summed from this list rather than by a second `SUM()` query, so the
     * headline figure and the itemised breakdown below it cannot disagree.
     */
    @Query(
        "SELECT * FROM bought_items " +
            "WHERE boughtAt >= :start AND boughtAt < :endExclusive " +
            "ORDER BY boughtAt DESC"
    )
    fun observeBoughtItemsIn(start: Long, endExclusive: Long): Flow<List<BoughtItem>>

    /**
     * Every outside-food cost inside [start, endExclusive), newest day first.
     *
     * Mirrors the rule Today uses: a day with its own eaten record counts that record, a day
     * without one falls back to its plan — which is why the two halves of the union exclude each
     * other on `eaten_days`. Homecooked food is excluded by [outside] rather than by a null price,
     * so a stray price cannot leak into the total.
     *
     * Keep this in step with `effectiveSlots` in `ui/common/PlanSlots.kt`: the same rule has to
     * exist twice because this half of it must run as SQL.
     */
    @Query(
        "SELECT em.dayStart AS dayStart, f.name AS name, " +
            "COALESCE(f.pricePaise, 0) AS pricePaise, f.place AS place, " +
            "em.quantity AS quantity " +
            "FROM eaten_meals em INNER JOIN foods f ON f.id = em.foodId " +
            "WHERE em.dayStart >= :start AND em.dayStart < :endExclusive " +
            "AND f.source = :outside " +
            "UNION ALL " +
            "SELECT pm.dayStart AS dayStart, f.name AS name, " +
            "COALESCE(f.pricePaise, 0) AS pricePaise, f.place AS place, " +
            "pm.quantity AS quantity " +
            "FROM planned_meals pm INNER JOIN foods f ON f.id = pm.foodId " +
            "WHERE pm.dayStart >= :start AND pm.dayStart < :endExclusive " +
            "AND f.source = :outside " +
            "AND pm.dayStart NOT IN (SELECT dayStart FROM eaten_days) " +
            "ORDER BY dayStart DESC, name COLLATE NOCASE ASC"
    )
    fun observeOutsideFoodsIn(
        start: Long,
        endExclusive: Long,
        outside: MealPlace,
    ): Flow<List<SpendEntry>>
}
