package com.reevan.reevzmealz

import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.util.foodSubtitle
import com.reevan.reevzmealz.util.paiseToEditableRupees
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodFormatTest {

    @Test
    fun homecooked_food_shows_no_price() {
        assertEquals("Homecooked", foodSubtitle(MealPlace.HOME, null))
    }

    @Test
    fun homecooked_food_ignores_a_stray_price() {
        // Should never happen, but the subtitle must not leak a price for homecooked food.
        assertEquals("Homecooked", foodSubtitle(MealPlace.HOME, 12_000))
    }

    @Test
    fun outside_food_shows_its_price() {
        assertEquals("Outside · ₹120", foodSubtitle(MealPlace.OUT, 12_000))
        assertEquals("Outside · ₹120.50", foodSubtitle(MealPlace.OUT, 12_050))
    }

    @Test
    fun outside_food_with_no_price_reads_as_zero() {
        assertEquals("Outside · ₹0", foodSubtitle(MealPlace.OUT, null))
    }

    @Test
    fun paise_render_as_editable_rupees_without_symbol() {
        assertEquals("120", paiseToEditableRupees(12_000))
        assertEquals("120.50", paiseToEditableRupees(12_050))
        assertEquals("0.05", paiseToEditableRupees(5))
        assertEquals("0", paiseToEditableRupees(0))
    }
}
