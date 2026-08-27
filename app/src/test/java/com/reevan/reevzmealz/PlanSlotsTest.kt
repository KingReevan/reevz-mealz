package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedFood
import com.reevan.reevzmealz.ui.common.buildSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSlotsTest {

    private fun planned(
        id: Long,
        type: MealType,
        name: String,
        source: MealPlace,
        pricePaise: Int?,
    ) = PlannedFood(
        plannedMealId = id,
        type = type,
        foodId = id,
        name = name,
        source = source,
        pricePaise = pricePaise,
    )

    @Test
    fun all_four_slots_are_present_even_when_nothing_is_planned() {
        val slots = buildSlots(emptyList())

        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER),
            slots.map { it.type },
        )
        assertTrue(slots.all { it.isEmpty })
    }

    @Test
    fun slots_appear_in_breakfast_lunch_snack_dinner_order() {
        assertEquals(
            listOf("Breakfast", "Lunch", "Snack", "Dinner"),
            MealType.entries.map { it.label },
        )
    }

    @Test
    fun homecooked_food_contributes_nothing_to_the_slot_cost() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.BREAKFAST, "Poha", MealPlace.HOME, null),
                planned(2, MealType.BREAKFAST, "Tea", MealPlace.HOME, null),
            ),
        )
        val breakfast = slots.first { it.type == MealType.BREAKFAST }

        assertEquals(0L, breakfast.costPaise)
        assertTrue(breakfast.isFullyHomecooked)
        assertFalse(breakfast.isEmpty)
    }

    @Test
    fun a_stray_price_on_homecooked_food_is_still_ignored() {
        // Foods nulls the price on save, but the cost rule must not depend on that.
        val slots = buildSlots(
            listOf(planned(1, MealType.LUNCH, "Dal", MealPlace.HOME, 9_999)),
        )

        assertEquals(0L, slots.first { it.type == MealType.LUNCH }.costPaise)
    }

    @Test
    fun outside_food_counts_towards_the_slot_cost() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.LUNCH, "Biryani", MealPlace.OUT, 38_000),
                planned(2, MealType.LUNCH, "Coke", MealPlace.OUT, 4_000),
            ),
        )
        val lunch = slots.first { it.type == MealType.LUNCH }

        assertEquals(42_000L, lunch.costPaise)
        assertFalse(lunch.isFullyHomecooked)
    }

    @Test
    fun a_mixed_slot_counts_only_the_outside_food() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.BREAKFAST, "Poha", MealPlace.HOME, null),
                planned(2, MealType.BREAKFAST, "Croissant", MealPlace.OUT, 4_000),
            ),
        )
        val breakfast = slots.first { it.type == MealType.BREAKFAST }

        assertEquals(4_000L, breakfast.costPaise)
        assertFalse(breakfast.isFullyHomecooked)
    }

    @Test
    fun day_total_sums_every_slot_and_ignores_homecooked() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.BREAKFAST, "Poha", MealPlace.HOME, null),
                planned(2, MealType.BREAKFAST, "Croissant", MealPlace.OUT, 4_000),
                planned(3, MealType.LUNCH, "Biryani", MealPlace.OUT, 38_000),
                planned(4, MealType.DINNER, "Khichdi", MealPlace.HOME, null),
            ),
        )

        assertEquals(42_000L, totalCostPaise(slots))
    }

    @Test
    fun day_total_is_zero_when_everything_planned_is_homecooked() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.BREAKFAST, "Poha", MealPlace.HOME, null),
                planned(2, MealType.DINNER, "Khichdi", MealPlace.HOME, null),
            ),
        )

        assertEquals(0L, totalCostPaise(slots))
    }

    @Test
    fun foods_land_in_the_slot_they_were_planned_into() {
        val slots = buildSlots(
            listOf(
                planned(1, MealType.SNACK, "Samosa", MealPlace.OUT, 2_000),
                planned(2, MealType.DINNER, "Roti", MealPlace.HOME, null),
            ),
        )

        assertEquals(listOf("Samosa"), slots.first { it.type == MealType.SNACK }.foods.map { it.name })
        assertEquals(listOf("Roti"), slots.first { it.type == MealType.DINNER }.foods.map { it.name })
        assertTrue(slots.first { it.type == MealType.LUNCH }.isEmpty)
    }
}
