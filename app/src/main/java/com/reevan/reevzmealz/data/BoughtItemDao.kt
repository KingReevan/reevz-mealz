package com.reevan.reevzmealz.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoughtItemDao {

    /** All purchases, newest first, so the month-wise history reads top-down like a statement. */
    @Query("SELECT * FROM bought_items ORDER BY boughtAt DESC, id DESC")
    fun observeAll(): Flow<List<BoughtItem>>

    @Insert
    suspend fun insert(item: BoughtItem): Long

    @Update
    suspend fun update(item: BoughtItem)

    @Delete
    suspend fun delete(item: BoughtItem)
}
