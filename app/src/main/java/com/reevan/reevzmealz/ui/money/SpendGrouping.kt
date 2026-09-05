package com.reevan.reevzmealz.ui.money

import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.data.SpendEntry
import com.reevan.reevzmealz.util.startOfDay

/** Which stream a line came from. Decides its accent, and nothing else. */
enum class SpendStream {
    OUTSIDE_FOOD,
    BOUGHT_ITEM,
}

/**
 * One line of the breakdown, independent of which table it came from.
 *
 * The two streams are shown as one list grouped by date, so they need one shared shape — the
 * alternative is two parallel lists that have to be interleaved at render time.
 */
data class SpendLine(
    val dayStart: Long,
    val name: String,
    /** The place for outside food, "Bought item" for groceries; null if there is nothing to say. */
    val detail: String?,
    /** What the line came to: unit price times quantity. */
    val pricePaise: Long,
    val stream: SpendStream,
    /** Shown as an "x2" tag; 1 means nothing is shown. */
    val quantity: Int = 1,
)

/** Everything spent on one day, with what the day came to. */
data class DaySection(
    val dayStart: Long,
    val totalPaise: Long,
    val lines: List<SpendLine>,
)

/**
 * Merges the two streams into one list of lines.
 *
 * `SpendEntry.dayStart` is already a local midnight, but `BoughtItem.boughtAt` is a real timestamp,
 * so it has to be normalised here or a purchase would form its own group per *instant* rather than
 * joining that day's.
 *
 * Outside food is emitted before bought items so that within a day the food (red) stays together
 * above the groceries (blue), and each stream keeps the order its query gave it.
 */
fun spendLines(
    outsideFoods: List<SpendEntry>,
    boughtItems: List<BoughtItem>,
): List<SpendLine> {
    val foodLines = outsideFoods.map { entry ->
        SpendLine(
            dayStart = entry.dayStart,
            name = entry.name,
            detail = entry.place,
            // The line's own cost, not the unit price — two kebabs is two kebabs' worth.
            pricePaise = entry.pricePaise.toLong() * entry.quantity,
            stream = SpendStream.OUTSIDE_FOOD,
            quantity = entry.quantity,
        )
    }
    val itemLines = boughtItems.map { item ->
        SpendLine(
            dayStart = startOfDay(item.boughtAt),
            name = item.name,
            detail = "Bought item",
            pricePaise = item.pricePaise.toLong(),
            stream = SpendStream.BOUGHT_ITEM,
        )
    }
    return foodLines + itemLines
}

/**
 * Groups lines into one section per day, newest day first.
 *
 * `groupBy` keeps the order lines arrived in within each day, so the stream ordering set up by
 * [spendLines] survives. The per-day total is summed from the very lines shown underneath it —
 * the same reason there is no `SUM()` query: a separately computed figure could drift from its
 * own list.
 */
fun groupByDay(lines: List<SpendLine>): List<DaySection> =
    lines
        .groupBy { it.dayStart }
        .map { (dayStart, dayLines) ->
            DaySection(
                dayStart = dayStart,
                totalPaise = dayLines.sumOf { it.pricePaise },
                lines = dayLines,
            )
        }
        .sortedByDescending { it.dayStart }
