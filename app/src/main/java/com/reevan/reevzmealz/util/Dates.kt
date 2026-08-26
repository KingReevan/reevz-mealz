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

private fun yearOf(millis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    return calendar.get(Calendar.YEAR)
}
