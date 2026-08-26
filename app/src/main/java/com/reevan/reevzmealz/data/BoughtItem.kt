package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Something bought for cooking — an ingredient or grocery, with what it cost and when.
 *
 * Deliberately not linked to [Food]. A bought item is a purchase event ("wholewheat bread,
 * ₹80, today"); a Food is a reusable building block for planning. Tying them together would
 * force every purchase to first exist as a Food, which is more friction than the section is
 * worth.
 */
@Entity(tableName = "bought_items")
data class BoughtItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Price in paise. Integer so money is never subject to float rounding. */
    val pricePaise: Int,
    /** When it was bought, as epoch millis. */
    val boughtAt: Long,
)
