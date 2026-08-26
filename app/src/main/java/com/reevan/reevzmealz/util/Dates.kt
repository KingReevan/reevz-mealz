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
