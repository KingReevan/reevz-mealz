package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.ui.bought.groupByMonth
import com.reevan.reevzmealz.util.formatMonthLabel
import com.reevan.reevzmealz.util.monthKeyOf
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthGroupingTest {

    private fun millisAt(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 12, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun item(id: Long, name: String, paise: Int, millis: Long) =
        BoughtItem(id = id, name = name, pricePaise = paise, boughtAt = millis)

    @Test
    fun same_month_shares_a_key_and_different_months_do_not() {
        assertEquals(
            monthKeyOf(millisAt(2026, Calendar.AUGUST, 1)),
            monthKeyOf(millisAt(2026, Calendar.AUGUST, 28)),
        )
        assert(
            monthKeyOf(millisAt(2026, Calendar.AUGUST, 1)) !=
                monthKeyOf(millisAt(2026, Calendar.JULY, 1)),
        )
    }

    @Test
    fun same_month_in_different_years_never_merges() {
        assert(
            monthKeyOf(millisAt(2026, Calendar.JANUARY, 5)) !=
                monthKeyOf(millisAt(2025, Calendar.JANUARY, 5)),
        )
    }

    @Test
    fun groups_into_months_newest_first() {
        val items = listOf(
            item(1, "Bread", 8_000, millisAt(2026, Calendar.AUGUST, 26)),
            item(2, "Milk", 3_000, millisAt(2026, Calendar.AUGUST, 2)),
            item(3, "Rice", 25_000, millisAt(2026, Calendar.JULY, 15)),
        )

        val months = groupByMonth(items)

        assertEquals(2, months.size)
        assertEquals(listOf("Bread", "Milk"), months[0].items.map { it.name })
        assertEquals(listOf("Rice"), months[1].items.map { it.name })
    }

    @Test
    fun month_total_is_the_sum_of_its_items() {
        val items = listOf(
            item(1, "Bread", 8_000, millisAt(2026, Calendar.AUGUST, 26)),
            item(2, "Milk", 3_050, millisAt(2026, Calendar.AUGUST, 2)),
            item(3, "Rice", 25_000, millisAt(2026, Calendar.JULY, 15)),
        )

        val months = groupByMonth(items)

        assertEquals(11_050L, months[0].totalPaise)
        assertEquals(25_000L, months[1].totalPaise)
    }

    @Test
    fun empty_history_produces_no_sections() {
        assertEquals(emptyList<Any>(), groupByMonth(emptyList()))
    }

    @Test
    fun month_label_omits_the_year_only_for_the_current_year() {
        val now = millisAt(2026, Calendar.AUGUST, 26)
        assertEquals("August", formatMonthLabel(millisAt(2026, Calendar.AUGUST, 2), now))
        assertEquals("January", formatMonthLabel(millisAt(2026, Calendar.JANUARY, 2), now))
        assertEquals("December", formatMonthLabel(millisAt(2025, Calendar.DECEMBER, 2), now).take(8))
    }

    @Test
    fun month_label_includes_the_year_for_other_years() {
        val now = millisAt(2026, Calendar.AUGUST, 26)
        assertEquals("December 2025", formatMonthLabel(millisAt(2025, Calendar.DECEMBER, 2), now))
    }
}
