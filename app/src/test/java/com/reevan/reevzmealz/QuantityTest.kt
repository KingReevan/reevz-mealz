package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.BoughtItem
import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.SlotFood
import com.reevan.reevzmealz.data.SpendEntry
import com.reevan.reevzmealz.ui.common.PlanSlot
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import com.reevan.reevzmealz.ui.money.groupByDay
import com.reevan.reevzmealz.ui.money.spendLines
import com.reevan.reevzmealz.util.startOfDay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Quantity multiplies money. Getting this wrong would understate every total in the app while
 * looking perfectly plausible, so each place that adds prices up is pinned down here.
 */
class QuantityTest {

    private val today = startOfDay(System.currentTimeMillis())

    private fun out(name: String, paise: Int?, quantity: Int = 1) = SlotFood(
        entryId = 0L,
        type = MealType.LUNCH,
        foodId = 0L,
        name = name,
        source = MealPlace.OUT,
        pricePaise = paise,
        quantity = quantity,
    )

    private fun home(name: String, quantity: Int = 1) = SlotFood(
        entryId = 0L,
        type = MealType.LUNCH,
        foodId = 0L,
        name = name,
        source = MealPlace.HOME,
        pricePaise = null,
        quantity = quantity,
    )

    // ---- the slot cost rule -------------------------------------------------

    @Test
    fun a_single_helping_costs_the_price() {
        val slot = PlanSlot(type = MealType.LUNCH, foods = listOf(out("Kebab", 3000)))
        assertEquals(3000L, slot.costPaise)
    }

    @Test
    fun two_kebabs_cost_twice_one_kebab() {
        val slot = PlanSlot(type = MealType.LUNCH, foods = listOf(out("Kebab", 3000, quantity = 2)))
        assertEquals(6000L, slot.costPaise)
    }

    @Test
    fun quantities_add_up_across_a_slot() {
        val slot = PlanSlot(
            type = MealType.LUNCH,
            foods = listOf(out("Kebab", 3000, quantity = 2), out("Tea", 2000, quantity = 3)),
        )
        assertEquals(12_000L, slot.costPaise)
    }

    /** Homecooked food has no price, so any quantity of it still costs nothing. */
    @Test
    fun a_quantity_of_homecooked_food_still_costs_nothing() {
        val slot = PlanSlot(
            type = MealType.LUNCH,
            foods = listOf(home("Rice", quantity = 4), out("Kebab", 3000, quantity = 2)),
        )
        assertEquals(6000L, slot.costPaise)
    }

    @Test
    fun the_day_total_follows_the_quantities() {
        val slots = buildSlots(
            listOf(
                out("Kebab", 3000, quantity = 2).copy(type = MealType.LUNCH),
                out("Tea", 2000).copy(type = MealType.SNACK),
            ),
        )
        assertEquals(8000L, totalCostPaise(slots))
    }

    /** An existing row read back before the migration ran would carry no quantity at all. */
    @Test
    fun quantity_defaults_to_one() {
        assertEquals(1, out("Kebab", 3000).quantity)
        assertEquals(1, SpendEntry(dayStart = today, name = "Kebab", pricePaise = 3000, place = null).quantity)
    }

    // ---- Money Spent --------------------------------------------------------

    @Test
    fun a_spend_line_carries_the_whole_lines_cost() {
        val lines = spendLines(
            outsideFoods = listOf(
                SpendEntry(dayStart = today, name = "Kebab", pricePaise = 3000, place = "Thilak", quantity = 2),
            ),
            boughtItems = emptyList(),
        )
        assertEquals(6000L, lines[0].pricePaise)
        assertEquals(2, lines[0].quantity)
    }

    @Test
    fun a_day_section_totals_the_multiplied_lines() {
        val lines = spendLines(
            outsideFoods = listOf(
                SpendEntry(dayStart = today, name = "Kebab", pricePaise = 3000, place = null, quantity = 2),
                SpendEntry(dayStart = today, name = "Tea", pricePaise = 2000, place = null),
            ),
            boughtItems = listOf(BoughtItem(id = 0L, name = "Rice", pricePaise = 1000, boughtAt = today)),
        )
        assertEquals(9000L, groupByDay(lines).single().totalPaise)
    }

    /** Groceries have no quantity of their own, so they must not be scaled by anything. */
    @Test
    fun a_bought_item_is_never_multiplied() {
        val lines = spendLines(
            outsideFoods = emptyList(),
            boughtItems = listOf(BoughtItem(id = 0L, name = "Rice", pricePaise = 1000, boughtAt = today)),
        )
        assertEquals(1000L, lines[0].pricePaise)
        assertEquals(1, lines[0].quantity)
    }
}
