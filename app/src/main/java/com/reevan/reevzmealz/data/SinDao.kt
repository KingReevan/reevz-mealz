package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SinDao {

    /** Sins committed within [start, endExclusive) — used for the current calendar month. */
    @Query(
        "SELECT COUNT(*) FROM sin_events WHERE dayStart >= :start AND dayStart < :endExclusive"
    )
    fun observeSinsUsed(start: Long, endExclusive: Long): Flow<Int>

    /** Which meal slots were marked as sins on a day, so an ended day can be shown back. */
    @Query("SELECT type FROM sin_events WHERE dayStart = :dayStart")
    fun observeSinnedTypes(dayStart: Long): Flow<List<MealType>>

    @Query("SELECT COUNT(*) > 0 FROM ended_days WHERE dayStart = :dayStart")
    fun observeIsDayEnded(dayStart: Long): Flow<Boolean>

    @Query("SELECT * FROM sin_settings WHERE id = 1")
    fun observeSettings(): Flow<SinSettings?>

    /**
     * Settles a day: records it as ended and writes one sin per slot in [sinned].
     *
     * Does nothing if the day is already ended, so a repeat press cannot double-deduct.
     */
    @Transaction
    suspend fun endDay(dayStart: Long, sinned: List<MealType>, endedAt: Long) {
        if (isDayEnded(dayStart)) return
        insertEndedDay(EndedDay(dayStart = dayStart, endedAt = endedAt))
        sinned.forEach { type ->
            insertSin(SinEvent(dayStart = dayStart, type = type))
        }
    }

    @Query("SELECT COUNT(*) > 0 FROM ended_days WHERE dayStart = :dayStart")
    suspend fun isDayEnded(dayStart: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEndedDay(endedDay: EndedDay)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSin(sinEvent: SinEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SinSettings)
}
