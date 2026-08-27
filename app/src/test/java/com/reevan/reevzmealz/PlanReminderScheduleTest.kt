package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.AppSettings
import com.reevan.reevzmealz.notify.PlanReminderScheduler
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanReminderScheduleTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun fieldsOf(millis: Long): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return Triple(
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    @Test
    fun the_default_reminder_is_seven_pm() {
        assertEquals(19, AppSettings.DEFAULT_REMINDER_HOUR)
        assertEquals(19, AppSettings().reminderHour)
        assertEquals(0, AppSettings().reminderMinute)
    }

    @Test
    fun reminders_are_on_by_default() {
        assertTrue(AppSettings().remindersEnabled)
    }

    @Test
    fun before_the_time_it_fires_the_same_day() {
        val now = at(2026, Calendar.AUGUST, 26, 14, 30)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 19, 0)
        assertEquals(Triple(26, 19, 0), fieldsOf(trigger))
    }

    @Test
    fun after_the_time_it_rolls_to_tomorrow() {
        val now = at(2026, Calendar.AUGUST, 26, 21, 15)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 19, 0)
        assertEquals(Triple(27, 19, 0), fieldsOf(trigger))
    }

    @Test
    fun exactly_at_the_time_it_rolls_to_tomorrow_rather_than_firing_twice() {
        val now = at(2026, Calendar.AUGUST, 26, 19, 0)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 19, 0)
        assertEquals(Triple(27, 19, 0), fieldsOf(trigger))
    }

    @Test
    fun a_minute_before_still_fires_today() {
        val now = at(2026, Calendar.AUGUST, 26, 18, 59)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 19, 0)
        assertEquals(Triple(26, 19, 0), fieldsOf(trigger))
    }

    @Test
    fun it_rolls_across_a_month_boundary() {
        val now = at(2026, Calendar.AUGUST, 31, 22, 0)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 19, 0)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = trigger
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH))
        assertEquals(19, calendar.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun a_custom_time_is_honoured() {
        val now = at(2026, Calendar.AUGUST, 26, 8, 0)
        val trigger = PlanReminderScheduler.nextTriggerAt(now, 21, 30)
        assertEquals(Triple(26, 21, 30), fieldsOf(trigger))
    }

    @Test
    fun the_trigger_is_always_in_the_future() {
        val now = at(2026, Calendar.AUGUST, 26, 19, 0)
        assertTrue(PlanReminderScheduler.nextTriggerAt(now, 19, 0) > now)
    }
}
