package com.reevan.reevzmealz.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A half-open range [start, endExclusive) of epoch millis.
 *
 * java.util.Calendar is used rather than java.time because java.time needs API 26 and
 * this app supports minSdk 24. Revisit if minSdk rises or desugaring is enabled.
 */
data class DayRange(val start: Long, val endExclusive: Long)

/** The local calendar day containing [millis], from midnight to midnight. */
fun dayRangeOf(millis: Long): DayRange {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return DayRange(start, calendar.timeInMillis)
}

/** Local midnight of the day containing [millis]. */
fun startOfDay(millis: Long): Long = dayRangeOf(millis).start

fun formatDayHeading(millis: Long): String =
    SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(millis))

fun formatTimeOfDay(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

/**
 * Stable sortable key for the calendar month containing [millis], as `year * 100 + month`.
 * Two timestamps share a key exactly when they fall in the same month of the same year, so
 * grouping by it never merges the same month across different years.
 */
fun monthKeyOf(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
}

/**
 * Month heading for a history list: "August" within the current year, "August 2025" otherwise —
 * the year is only noise while it is the year you are in.
 *
 * [now] is a parameter rather than a call to the clock so the behaviour is testable.
 */
fun formatMonthLabel(millis: Long, now: Long = System.currentTimeMillis()): String {
    val pattern = if (yearOf(millis) == yearOf(now)) "MMMM" else "MMMM yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

/** Short date for a row inside a month section, e.g. "26 Aug". */
fun formatShortDate(millis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))

/** Always-qualified month heading for a date picker, e.g. "August 2026". */
fun formatMonthAndYear(millis: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))

/** Abbreviated weekday, e.g. "Tue". */
fun formatWeekdayShort(millis: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))

/** Day of the month as a bare number, e.g. "26". */
fun formatDayOfMonth(millis: Long): String =
    SimpleDateFormat("d", Locale.getDefault()).format(Date(millis))

/**
 * [days] later than [dayStart], renormalised to midnight so a DST transition cannot leave the
 * result an hour off its own day.
 */
fun addDays(dayStart: Long, days: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dayStart
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return startOfDay(calendar.timeInMillis)
}

/** Midnight of the first day of the month containing [millis]. */
fun startOfMonth(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return startOfDay(calendar.timeInMillis)
}

/** First day of the month [months] away from the month containing [millis]. */
fun addMonths(millis: Long, months: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startOfMonth(millis)
    calendar.add(Calendar.MONTH, months)
    return startOfDay(calendar.timeInMillis)
}

/** Exclusive end of the month containing [millis], i.e. the first day of the next one. */
fun endOfMonthExclusive(millis: Long): Long = addMonths(millis, 1)

/**
 * The seven day-starts of the week containing [millis], beginning at the locale's first day of
 * the week.
 */
fun weekDaysOf(millis: Long): List<Long> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startOfDay(millis)
    val firstDayOfWeek = calendar.firstDayOfWeek
    while (calendar.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    val weekStart = startOfDay(calendar.timeInMillis)
    return (0 until DAYS_IN_WEEK).map { addDays(weekStart, it) }
}

/** Start of the week containing [millis]. */
fun startOfWeek(millis: Long): Long = weekDaysOf(millis).first()

/** Exclusive end of the week containing [millis]. */
fun endOfWeekExclusive(millis: Long): Long = addDays(startOfWeek(millis), DAYS_IN_WEEK)

/**
 * The month containing [millis] as calendar cells: leading and trailing nulls pad the grid so
 * every row holds exactly seven entries and real days sit under the right weekday column.
 */
fun monthGridOf(millis: Long): List<Long?> {
    val monthStart = startOfMonth(millis)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = monthStart
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks =
        ((calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek) + DAYS_IN_WEEK) %
            DAYS_IN_WEEK

    val cells = ArrayList<Long?>(leadingBlanks + daysInMonth)
    repeat(leadingBlanks) { cells.add(null) }
    for (offset in 0 until daysInMonth) {
        cells.add(addDays(monthStart, offset))
    }
    while (cells.size % DAYS_IN_WEEK != 0) {
        cells.add(null)
    }
    return cells
}

/** Weekday column headings, ordered from the locale's first day of the week. */
fun weekdayHeadings(): List<String> =
    weekDaysOf(System.currentTimeMillis()).map { formatWeekdayShort(it).take(1) }

fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

private const val DAYS_IN_WEEK = 7

private fun yearOf(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.YEAR)
}
