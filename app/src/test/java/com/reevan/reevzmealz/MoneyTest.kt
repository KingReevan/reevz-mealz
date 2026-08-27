package com.reevan.reevzmealz

import com.reevan.reevzmealz.util.formatPaise
import com.reevan.reevzmealz.util.paiseToEditableRupees
import com.reevan.reevzmealz.util.parseRupeesToPaise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun formats_whole_rupees_without_decimals() {
        assertEquals("₹0", formatPaise(0))
        assertEquals("₹420", formatPaise(42_000))
    }

    @Test
    fun formats_partial_rupees_with_two_decimals() {
        assertEquals("₹420.50", formatPaise(42_050))
        assertEquals("₹0.05", formatPaise(5))
    }

    @Test
    fun parses_plain_and_decimal_rupees() {
        assertEquals(4_200, parseRupeesToPaise("42"))
        assertEquals(4_250, parseRupeesToPaise("42.5"))
        assertEquals(4_250, parseRupeesToPaise("42.50"))
        assertEquals(4_205, parseRupeesToPaise("42.05"))
    }

    @Test
    fun blank_cost_counts_as_zero() {
        assertEquals(0, parseRupeesToPaise(""))
        assertEquals(0, parseRupeesToPaise("   "))
    }

    @Test
    fun rejects_nonsense_amounts() {
        assertNull(parseRupeesToPaise("abc"))
        assertNull(parseRupeesToPaise("12.345"))
        assertNull(parseRupeesToPaise("-5"))
        assertNull(parseRupeesToPaise("1,000"))
    }

    @Test
    fun rupees_are_grouped_indian_style() {
        assertEquals("₹999", formatPaise(99_900))
        assertEquals("₹1,000", formatPaise(100_000))
        assertEquals("₹58,200", formatPaise(5_820_000))
        assertEquals("₹1,00,000", formatPaise(10_000_000))
        assertEquals("₹12,34,567", formatPaise(123_456_700))
    }

    @Test
    fun grouping_survives_paise_and_sign() {
        assertEquals("₹1,00,000.50", formatPaise(10_000_050))
        assertEquals("-₹1,00,000", formatPaise(-10_000_000))
    }

    @Test
    fun the_editable_field_form_is_never_grouped() {
        // It has to parse back, and parseRupeesToPaise rejects commas.
        assertEquals("100000", paiseToEditableRupees(10_000_000))
        assertNull(parseRupeesToPaise("1,00,000"))
    }

    @Test
    fun parse_then_format_round_trips() {
        val paise = parseRupeesToPaise("199.99")
        assertEquals("₹199.99", formatPaise(paise!!.toLong()))
    }
}
