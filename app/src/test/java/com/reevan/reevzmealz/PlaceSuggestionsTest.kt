package com.reevan.reevzmealz

import com.reevan.reevzmealz.util.knownPlaces
import com.reevan.reevzmealz.util.suggestPlaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSuggestionsTest {

    // ---- knownPlaces ------------------------------------------------------

    /** Homecooked food has a null place, and that is most of the list on a good week. */
    @Test
    fun drops_nulls_and_blanks() {
        val places = listOf("Tea Break", null, "", "   ", "Thilak")
        assertEquals(listOf("Tea Break", "Thilak"), knownPlaces(places))
    }

    @Test
    fun orders_by_how_many_foods_use_the_place() {
        val places = listOf("Thilak", "Tea Break", "Tea Break", "Tea Break", "Thilak", "S Cube")
        assertEquals(listOf("Tea Break", "Thilak", "S Cube"), knownPlaces(places))
    }

    /** Equal counts fall back to alphabetical, so the row does not reshuffle run to run. */
    @Test
    fun breaks_ties_alphabetically() {
        assertEquals(
            listOf("Limeology", "S Cube", "Thilak"),
            knownPlaces(listOf("Thilak", "S Cube", "Limeology")),
        )
    }

    /**
     * Data typed before the capitalisation rule existed can differ only in case. One shop is one
     * suggestion, and the first spelling seen is the one shown.
     */
    @Test
    fun folds_places_that_differ_only_in_case() {
        assertEquals(
            listOf("Tea Break"),
            knownPlaces(listOf("Tea Break", "tea break", "TEA BREAK")),
        )
    }

    @Test
    fun trims_surrounding_whitespace_before_comparing() {
        assertEquals(listOf("Thilak"), knownPlaces(listOf("Thilak", "  Thilak  ")))
    }

    @Test
    fun no_places_at_all_gives_an_empty_list() {
        assertEquals(emptyList<String>(), knownPlaces(listOf(null, null, "")))
        assertEquals(emptyList<String>(), knownPlaces(emptyList()))
    }

    // ---- suggestPlaces ----------------------------------------------------

    private val known = listOf("Tea Break", "Thilak", "Taste Of Parika", "Limeology", "S Cube")

    /** An empty box is the common case: everything, so one tap does the whole job. */
    @Test
    fun an_empty_query_offers_everything_in_order() {
        assertEquals(known, suggestPlaces(known, ""))
        assertEquals(known, suggestPlaces(known, "   "))
    }

    @Test
    fun matches_as_a_prefix_case_insensitively() {
        assertEquals(
            listOf("Tea Break", "Thilak", "Taste Of Parika"),
            suggestPlaces(known, "t"),
        )
        assertEquals(listOf("Thilak"), suggestPlaces(known, "TH"))
    }

    /** Prefix matches first: "cube" should not outrank a place that starts with the query. */
    @Test
    fun prefix_matches_come_before_mid_word_matches() {
        val places = listOf("Cube Cafe", "S Cube")
        assertEquals(listOf("Cube Cafe", "S Cube"), suggestPlaces(places, "cube"))
        assertEquals(listOf("Cube Cafe", "S Cube"), suggestPlaces(places.reversed(), "cube"))
    }

    @Test
    fun matches_inside_the_name_too() {
        assertEquals(listOf("Taste Of Parika"), suggestPlaces(known, "parika"))
    }

    /** Tapping it would change nothing, so it is not offered. */
    @Test
    fun drops_a_place_identical_to_what_is_typed() {
        assertTrue(suggestPlaces(listOf("Thilak"), "Thilak").isEmpty())
        assertTrue(suggestPlaces(listOf("Thilak"), "thilak").isEmpty())
        assertTrue(suggestPlaces(listOf("Thilak"), "  Thilak ").isEmpty())
    }

    /** ...but a longer place that merely starts with it still is. */
    @Test
    fun keeps_longer_places_that_start_with_what_is_typed() {
        val places = listOf("Tea", "Tea Break")
        assertEquals(listOf("Tea Break"), suggestPlaces(places, "Tea"))
    }

    @Test
    fun no_match_gives_an_empty_list() {
        assertTrue(suggestPlaces(known, "zzz").isEmpty())
        assertTrue(suggestPlaces(emptyList(), "tea").isEmpty())
    }

    /** Frequency order from knownPlaces survives filtering. */
    @Test
    fun preserves_the_frequency_order_it_was_given() {
        val ordered = knownPlaces(
            listOf("Thilak", "Tea Break", "Tea Break", "Taste Of Parika"),
        )
        assertEquals(listOf("Tea Break", "Taste Of Parika", "Thilak"), ordered)
        assertEquals(listOf("Tea Break", "Taste Of Parika", "Thilak"), suggestPlaces(ordered, "t"))
    }
}
