package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.data.SpendEntry
import com.reevan.reevzmealz.ui.money.SpendStream
import com.reevan.reevzmealz.ui.money.groupByDay
import com.reevan.reevzmealz.ui.money.spendLines
import com.reevan.reevzmealz.util.startOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendGroupingTest {

    private val day = 24L * 60 * 60 * 1000
    private val today = startOfDay(System.currentTimeMillis())
    private val yesterday = today - day

    private fun food(dayStart: Long, name: String, paise: Int, place: String? = "S Cube") =
        SpendEntry(dayStart = dayStart, name = name, pricePaise = paise, place = place)

    private fun bought(at: Long, name: String, paise: Int) =
        BoughtItem(id = 0L, name = name, pricePaise = paise, boughtAt = at)

    @Test
    fun groups_everything_from_one_day_together() {
        val lines = spendLines(
            outsideFoods = listOf(
                food(today, "Coffee", 90),
                food(today, "Dosa", 35),
                food(yesterday, "Tea", 20),
            ),
            boughtItems = emptyList(),
        )
        val sections = groupByDay(lines)

        assertEquals(2, sections.size)
        assertEquals(listOf("Coffee", "Dosa"), sections[0].lines.map { it.name })
        assertEquals(listOf("Tea"), sections[1].lines.map { it.name })
    }

    @Test
    fun newest_day_comes_first() {
        val lines = spendLines(
            outsideFoods = listOf(food(yesterday, "Old", 10), food(today, "New", 10)),
            boughtItems = emptyList(),
        )
        assertEquals(listOf(today, yesterday), groupByDay(lines).map { it.dayStart })
    }

    /** The day's figure has to come from the very lines under it, or the two could disagree. */
    @Test
    fun a_day_total_is_the_sum_of_its_own_lines() {
        val lines = spendLines(
            outsideFoods = listOf(food(today, "A", 90), food(today, "B", 35)),
            boughtItems = listOf(bought(today + 5_000, "Rice", 120)),
        )
        val sections = groupByDay(lines)
        assertEquals(1, sections.size)
        assertEquals(245L, sections[0].totalPaise)
    }

    /**
     * `boughtAt` is a real timestamp, not a midnight. Without normalising it, a purchase would
     * form its own section instead of joining that day's food.
     */
    @Test
    fun a_purchase_joins_the_day_it_was_made_on() {
        val middayPurchase = today + 13 * 60 * 60 * 1000
        val lines = spendLines(
            outsideFoods = listOf(food(today, "Dosa", 35)),
            boughtItems = listOf(bought(middayPurchase, "Rice", 120)),
        )
        val sections = groupByDay(lines)

        assertEquals(1, sections.size)
        assertEquals(today, sections[0].dayStart)
        assertEquals(listOf("Dosa", "Rice"), sections[0].lines.map { it.name })
    }

    /** Red above blue within a day, whichever order the two queries returned. */
    @Test
    fun outside_food_is_listed_before_bought_items_within_a_day() {
        val lines = spendLines(
            outsideFoods = listOf(food(today, "Dosa", 35)),
            boughtItems = listOf(bought(today, "Rice", 120)),
        )
        val streams = groupByDay(lines)[0].lines.map { it.stream }
        assertEquals(
            listOf(SpendStream.OUTSIDE_FOOD, SpendStream.BOUGHT_ITEM),
            streams,
        )
    }

    @Test
    fun a_purchase_on_a_day_with_no_food_still_gets_its_own_section() {
        val lines = spendLines(
            outsideFoods = listOf(food(today, "Dosa", 35)),
            boughtItems = listOf(bought(yesterday + 1_000, "Rice", 120)),
        )
        val sections = groupByDay(lines)

        assertEquals(2, sections.size)
        assertEquals(today, sections[0].dayStart)
        assertEquals(yesterday, sections[1].dayStart)
        assertEquals(120L, sections[1].totalPaise)
    }

    @Test
    fun nothing_spent_gives_no_sections() {
        assertTrue(groupByDay(spendLines(emptyList(), emptyList())).isEmpty())
    }

    @Test
    fun a_food_with_no_place_carries_no_detail() {
        val lines = spendLines(listOf(food(today, "Snack", 10, place = null)), emptyList())
        assertEquals(null, lines[0].detail)
    }
}
