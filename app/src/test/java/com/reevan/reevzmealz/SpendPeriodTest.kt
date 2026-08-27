package com.reevan.reevzmealz

import com.reevan.reevzmealz.ui.money.SpendPeriod
import com.reevan.reevzmealz.ui.money.canGoBack
import com.reevan.reevzmealz.ui.money.canGoForward
import com.reevan.reevzmealz.ui.money.countedRange
import com.reevan.reevzmealz.ui.money.normalise
import com.reevan.reevzmealz.ui.money.rangeOf
import com.reevan.reevzmealz.ui.money.shift
import com.reevan.reevzmealz.util.addDays
import com.reevan.reevzmealz.util.retentionCutoff
import com.reevan.reevzmealz.util.startOfDay
import com.reevan.reevzmealz.util.startOfMonth
import com.reevan.reevzmealz.util.startOfYear
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendPeriodTest {

    private fun at(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private val today = at(2026, Calendar.AUGUST, 26)

    @Test
    fun a_day_period_covers_exactly_one_day() {
        val range = rangeOf(SpendPeriod.DAY, today)
        assertEquals(startOfDay(today), range.start)
        assertEquals(addDays(startOfDay(today), 1), range.endExclusive)
    }

    @Test
    fun a_week_period_covers_seven_days() {
        val range = rangeOf(SpendPeriod.WEEK, today)
        assertEquals(addDays(range.start, 7), range.endExclusive)
    }

    @Test
    fun a_month_period_starts_on_the_first() {
        val range = rangeOf(SpendPeriod.MONTH, today)
        assertEquals(startOfMonth(today), range.start)
        assertEquals(startOfMonth(at(2026, Calendar.SEPTEMBER, 1)), range.endExclusive)
    }

    @Test
    fun a_year_period_starts_in_january() {
        val range = rangeOf(SpendPeriod.YEAR, today)
        assertEquals(startOfYear(today), range.start)
        assertEquals(startOfYear(at(2027, Calendar.MARCH, 3)), range.endExclusive)
    }

    @Test
    fun shifting_a_month_back_lands_on_the_previous_month() {
        assertEquals(
            startOfMonth(at(2026, Calendar.JULY, 1)),
            normalise(SpendPeriod.MONTH, shift(SpendPeriod.MONTH, today, -1)),
        )
    }

    @Test
    fun shifting_a_month_across_a_year_boundary_works() {
        val january = at(2026, Calendar.JANUARY, 15)
        assertEquals(
            startOfMonth(at(2025, Calendar.DECEMBER, 1)),
            normalise(SpendPeriod.MONTH, shift(SpendPeriod.MONTH, january, -1)),
        )
    }

    @Test
    fun future_periods_are_not_reachable() {
        assertFalse(canGoForward(SpendPeriod.MONTH, startOfMonth(today), today))
        assertFalse(canGoForward(SpendPeriod.DAY, startOfDay(today), today))
    }

    @Test
    fun the_previous_month_is_reachable_but_thirteen_months_back_is_not() {
        var anchor = startOfMonth(today)
        assertTrue(canGoBack(SpendPeriod.MONTH, anchor, today))

        // Walk back as far as the window allows.
        var steps = 0
        while (canGoBack(SpendPeriod.MONTH, anchor, today) && steps < 50) {
            anchor = normalise(SpendPeriod.MONTH, shift(SpendPeriod.MONTH, anchor, -1))
            steps++
        }

        // 12 months retained, counting the current one, so 11 steps back.
        assertEquals(11, steps)
        assertEquals(retentionCutoff(today), anchor)
    }

    @Test
    fun the_current_month_is_counted_only_up_to_today() {
        val counted = countedRange(SpendPeriod.MONTH, startOfMonth(today), today)
        assertEquals(startOfMonth(today), counted!!.start)
        // Ends after today, not at month end.
        assertEquals(addDays(startOfDay(today), 1), counted.endExclusive)
    }

    @Test
    fun a_fully_past_month_is_counted_in_full() {
        val july = startOfMonth(at(2026, Calendar.JULY, 10))
        val counted = countedRange(SpendPeriod.MONTH, july, today)
        assertEquals(rangeOf(SpendPeriod.MONTH, july).endExclusive, counted!!.endExclusive)
    }

    @Test
    fun a_period_entirely_in_the_future_counts_nothing() {
        val nextMonth = startOfMonth(at(2026, Calendar.SEPTEMBER, 5))
        assertNull(countedRange(SpendPeriod.MONTH, nextMonth, today))
    }

    @Test
    fun switching_granularity_snaps_the_anchor_to_that_periods_start() {
        val midMonth = at(2026, Calendar.AUGUST, 17)
        assertEquals(startOfMonth(midMonth), normalise(SpendPeriod.MONTH, midMonth))
        assertEquals(startOfYear(midMonth), normalise(SpendPeriod.YEAR, midMonth))
        assertEquals(startOfDay(midMonth), normalise(SpendPeriod.DAY, midMonth))
    }

    @Test
    fun retention_cutoff_keeps_twelve_months_including_the_current_one() {
        assertEquals(startOfMonth(at(2025, Calendar.SEPTEMBER, 1)), retentionCutoff(today))
    }

    @Test
    fun period_toggle_order_is_day_week_month_year() {
        assertEquals(
            listOf("Day", "Week", "Month", "Year"),
            SpendPeriod.entries.map { it.label },
        )
    }
}
