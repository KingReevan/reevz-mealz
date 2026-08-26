package com.reevan.reevzmealz.ui.bought

import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.util.monthKeyOf

/** One month's purchases, with what they came to in total. */
data class MonthSection(
    val monthKey: Int,
    /** Any timestamp inside the month, for formatting the heading. */
    val anyMillisInMonth: Long,
    val totalPaise: Long,
    val items: List<BoughtItem>,
)

/**
 * Groups purchases into month sections, newest month first.
 *
 * [items] is expected newest-first (the DAO orders it that way); item order within each month is
 * preserved rather than re-sorted.
 */
fun groupByMonth(items: List<BoughtItem>): List<MonthSection> =
    items
        .groupBy { monthKeyOf(it.boughtAt) }
        .map { (key, monthItems) ->
            MonthSection(
                monthKey = key,
                anyMillisInMonth = monthItems.first().boughtAt,
                totalPaise = monthItems.sumOf { it.pricePaise.toLong() },
                items = monthItems,
            )
        }
        .sortedByDescending { it.monthKey }
