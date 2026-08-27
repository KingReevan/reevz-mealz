package com.reevan.reevzmealz.ui.money

import com.reevan.reevzmealz.util.addDays
import com.reevan.reevzmealz.util.addMonths
import com.reevan.reevzmealz.util.addYears
import com.reevan.reevzmealz.util.formatDayHeading
import com.reevan.reevzmealz.util.formatMonthAndYear
import com.reevan.reevzmealz.util.formatShortDate
import com.reevan.reevzmealz.util.formatYear
import com.reevan.reevzmealz.util.retentionCutoff
import com.reevan.reevzmealz.util.startOfDay
import com.reevan.reevzmealz.util.startOfMonth
import com.reevan.reevzmealz.util.startOfWeek
import com.reevan.reevzmealz.util.startOfYear

/** How spending is bucketed. Declaration order is the order of the toggle. */
enum class SpendPeriod(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

/** A half-open window [start, endExclusive) of epoch millis. */
data class PeriodRange(val start: Long, val endExclusive: Long)

/** The window [period] covers around [anchor]. */
fun rangeOf(period: SpendPeriod, anchor: Long): PeriodRange = when (period) {
    SpendPeriod.DAY -> PeriodRange(startOfDay(anchor), addDays(startOfDay(anchor), 1))
    SpendPeriod.WEEK -> PeriodRange(startOfWeek(anchor), addDays(startOfWeek(anchor), 7))
    SpendPeriod.MONTH -> PeriodRange(startOfMonth(anchor), addMonths(anchor, 1))
    SpendPeriod.YEAR -> PeriodRange(startOfYear(anchor), addYears(anchor, 1))
}

/** [steps] periods away from the one containing [anchor]. */
fun shift(period: SpendPeriod, anchor: Long, steps: Int): Long = when (period) {
    SpendPeriod.DAY -> addDays(startOfDay(anchor), steps)
    SpendPeriod.WEEK -> addDays(startOfWeek(anchor), steps * 7)
    SpendPeriod.MONTH -> addMonths(anchor, steps)
    SpendPeriod.YEAR -> addYears(anchor, steps)
}

/**
 * Anchor snapped to the start of its own period, so switching granularity never leaves the
 * anchor mid-period.
 */
fun normalise(period: SpendPeriod, anchor: Long): Long = rangeOf(period, anchor).start

fun labelOf(period: SpendPeriod, anchor: Long): String = when (period) {
    SpendPeriod.DAY -> formatDayHeading(startOfDay(anchor))
    SpendPeriod.WEEK -> {
        val range = rangeOf(period, anchor)
        formatShortDate(range.start) + " – " + formatShortDate(addDays(range.endExclusive, -1))
    }

    SpendPeriod.MONTH -> formatMonthAndYear(startOfMonth(anchor))
    SpendPeriod.YEAR -> formatYear(startOfYear(anchor))
}

/**
 * Whether the period before [anchor] is still inside the retention window.
 *
 * A period counts as reachable while any part of it falls on or after the cutoff, so a year view
 * can still reach the previous calendar year that the 12-month window partly covers.
 */
fun canGoBack(period: SpendPeriod, anchor: Long, now: Long): Boolean {
    val previous = rangeOf(period, shift(period, anchor, -1))
    return previous.endExclusive > retentionCutoff(now)
}

/** Whether the period after [anchor] has begun; there is no spending to show in the future. */
fun canGoForward(period: SpendPeriod, anchor: Long, now: Long): Boolean =
    rangeOf(period, shift(period, anchor, 1)).start <= startOfDay(now)

/**
 * The window actually counted: never past today, because money has not been spent on days that
 * have not happened. Returns null when the whole period is still in the future.
 */
fun countedRange(period: SpendPeriod, anchor: Long, now: Long): PeriodRange? {
    val range = rangeOf(period, anchor)
    val todayEnd = addDays(startOfDay(now), 1)
    if (range.start >= todayEnd) return null
    return PeriodRange(range.start, minOf(range.endExclusive, todayEnd))
}
