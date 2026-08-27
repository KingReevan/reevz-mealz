package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.SlotFood
import com.reevan.reevzmealz.ui.common.effectiveSlots
import com.reevan.reevzmealz.ui.common.totalCostPaise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plan-versus-actual rule: an unedited day falls back to its plan, an edited day is
 * authoritative even when a slot is empty.
 */
class EffectiveSlotsTest {

    private fun food(
        id: Long,
        type: MealType,
        name: String,
        source: MealPlace,
        pricePaise: Int?,
    ) = SlotFood(
        entryId = id,
        type = type,
        foodId = id,
        name = name,
        source = source,
        pricePaise = pricePaise,
    )

    private val plannedLunch = listOf(
        food(1, MealType.LUNCH, "Biryani", MealPlace.OUT, 38_000),
    )

    @Test
    fun an_unedited_day_shows_the_plan() {
        val slots = effectiveSlots(planned = plannedLunch, eaten = emptyList(), dayLogged = false)

        assertEquals(
            listOf("Biryani"),
            slots.first { it.type == MealType.LUNCH }.foods.map { it.name },
        )
        assertEquals(38_000L, totalCostPaise(slots))
    }

    @Test
    fun an_edited_day_shows_what_was_eaten_not_the_plan() {
        val eaten = listOf(food(9, MealType.LUNCH, "Dosa", MealPlace.OUT, 12_000))

        val slots = effectiveSlots(planned = plannedLunch, eaten = eaten, dayLogged = true)

        assertEquals(
            listOf("Dosa"),
            slots.first { it.type == MealType.LUNCH }.foods.map { it.name },
        )
        assertEquals(12_000L, totalCostPaise(slots))
    }

    @Test
    fun a_skipped_meal_costs_nothing_even_though_it_was_planned() {
        // The whole point of the logged-day marker: empty-and-logged is not the same as unedited.
        val slots = effectiveSlots(planned = plannedLunch, eaten = emptyList(), dayLogged = true)

        assertTrue(slots.first { it.type == MealType.LUNCH }.isEmpty)
        assertEquals(0L, totalCostPaise(slots))
    }

    @Test
    fun eating_more_than_planned_raises_the_day_total() {
        val eaten = listOf(
            food(1, MealType.LUNCH, "Biryani", MealPlace.OUT, 38_000),
            food(2, MealType.SNACK, "Samosa", MealPlace.OUT, 2_000),
        )

        val slots = effectiveSlots(planned = plannedLunch, eaten = eaten, dayLogged = true)

        assertEquals(40_000L, totalCostPaise(slots))
    }

    @Test
    fun eating_something_cheaper_lowers_the_day_total() {
        val eaten = listOf(food(2, MealType.LUNCH, "Idli", MealPlace.OUT, 5_000))

        val slots = effectiveSlots(planned = plannedLunch, eaten = eaten, dayLogged = true)

        assertEquals(5_000L, totalCostPaise(slots))
    }

    @Test
    fun a_day_with_no_plan_can_still_record_what_was_eaten() {
        val eaten = listOf(food(3, MealType.DINNER, "Pizza", MealPlace.OUT, 45_000))

        val slots = effectiveSlots(planned = emptyList(), eaten = eaten, dayLogged = true)

        assertEquals(
            listOf("Pizza"),
            slots.first { it.type == MealType.DINNER }.foods.map { it.name },
        )
        assertEquals(45_000L, totalCostPaise(slots))
    }

    @Test
    fun eaten_homecooked_food_still_contributes_nothing() {
        val eaten = listOf(food(4, MealType.DINNER, "Khichdi", MealPlace.HOME, null))

        val slots = effectiveSlots(planned = emptyList(), eaten = eaten, dayLogged = true)

        assertEquals(0L, totalCostPaise(slots))
        assertTrue(slots.first { it.type == MealType.DINNER }.isFullyHomecooked)
    }

    @Test
    fun all_four_slots_render_whichever_source_is_authoritative() {
        val logged = effectiveSlots(emptyList(), emptyList(), dayLogged = true)
        val unlogged = effectiveSlots(emptyList(), emptyList(), dayLogged = false)

        assertEquals(4, logged.size)
        assertEquals(4, unlogged.size)
        assertEquals(logged.map { it.type }, unlogged.map { it.type })
    }
}
