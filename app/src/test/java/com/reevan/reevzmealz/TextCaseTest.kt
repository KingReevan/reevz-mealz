package com.reevan.reevzmealz

import com.reevan.reevzmealz.util.capitalizeWords
import org.junit.Assert.assertEquals
import org.junit.Test

class TextCaseTest {

    @Test
    fun capitalizes_the_first_letter_of_each_word() {
        assertEquals("Green Tea", capitalizeWords("green tea"))
        assertEquals("Idli", capitalizeWords("idli"))
        assertEquals("Tea Break", capitalizeWords("tea break"))
    }

    @Test
    fun leaves_already_capitalized_text_alone() {
        assertEquals("Green Tea", capitalizeWords("Green Tea"))
    }

    /** Title-casing would wreck these, which is why only the first letter is touched. */
    @Test
    fun preserves_capitals_inside_a_word() {
        assertEquals("KFC", capitalizeWords("KFC"))
        assertEquals("McDonald's Burger", capitalizeWords("McDonald's Burger"))
        assertEquals("ChaiPoint", capitalizeWords("chaiPoint"))
    }

    @Test
    fun preserves_spacing_exactly() {
        assertEquals("  Green  Tea  ", capitalizeWords("  green  tea  "))
        assertEquals("Green\tTea", capitalizeWords("green\ttea"))
    }

    /** Parentheses are common in these names, where the shop goes in brackets after the food. */
    @Test
    fun capitalizes_through_leading_punctuation() {
        assertEquals("(Tea Break)", capitalizeWords("(tea break)"))
        assertEquals("Green Tea (Tea Break)", capitalizeWords("green tea (tea break)"))
        assertEquals("\"Special\" Dosa", capitalizeWords("\"special\" dosa"))
    }

    @Test
    fun a_leading_digit_holds_the_word_closed() {
        assertEquals("2 Minute Noodles", capitalizeWords("2 minute noodles"))
        assertEquals("2nd Floor Cafe", capitalizeWords("2nd floor cafe"))
    }

    @Test
    fun handles_empty_and_single_character_input() {
        assertEquals("", capitalizeWords(""))
        assertEquals("A", capitalizeWords("a"))
        assertEquals(" ", capitalizeWords(" "))
    }
}
