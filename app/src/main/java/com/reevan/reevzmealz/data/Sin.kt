package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One meal that did not go according to plan — one sin.
 *
 * Sins are stored as events rather than as a running counter. The monthly allowance is then
 * derived (`allowance - sins this calendar month`), which means a new month starts fresh on its
 * own and any unused sins are discarded automatically. There is no reset job that could fail to
 * run, and the history stays auditable.
 */
@Entity(
    tableName = "sin_events",
    indices = [
        Index(value = ["dayStart"]),
        // One sin per meal slot per day: a slot is either followed or not.
        Index(value = ["dayStart", "type"], unique = true),
    ],
)
data class SinEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Local midnight of the day the sin was committed. */
    val dayStart: Long,
    val type: MealType,
)

/**
 * Marks a day as settled by "End day".
 *
 * A day can only be ended once — the judgement is final, which is the point. This row is what
 * makes that check possible and keeps a second press from deducting sins twice.
 */
@Entity(tableName = "ended_days")
data class EndedDay(
    @PrimaryKey val dayStart: Long,
    val endedAt: Long,
)

/**
 * The configurable sin allowance. Single row, id 1.
 *
 * [setAt] is null until the user actually chooses a number: the built-in default is a starting
 * value, not a decision, so the first change is not subject to the 3-day lock.
 */
@Entity(tableName = "sin_settings")
data class SinSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val monthlyAllowance: Int,
    /** When the allowance was last chosen, or null if never. Drives the 3-day edit lock. */
    val setAt: Long?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
