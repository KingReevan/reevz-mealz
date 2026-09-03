package com.reevan.reevzmealz.util

/**
 * Suggestion logic for the Place field, kept pure so it can be unit-tested without a device.
 *
 * There is no table of shops: the places you have used are simply derived from the foods you
 * already have, which is the only record of them there is. That means a place cannot go stale —
 * delete the last food from a shop and the shop stops being suggested.
 */

/**
 * The distinct places foods have been bought from, most-used first.
 *
 * Homecooked foods contribute null and drop out, as do blank values. Matching is
 * case-insensitive, so "tea break" typed before the capitalisation rule existed does not show up
 * as a second entry beside "Tea Break" — the first spelling seen wins, and since [knownPlaces] is
 * fed a list ordered by food name, that is deterministic rather than whatever Room happened to
 * return first.
 *
 * Ordered by how many foods use the place, then alphabetically, because "I keep buying from the
 * same places" is the whole reason the suggestions exist — the usual suspects belong first.
 */
fun knownPlaces(places: List<String?>): List<String> {
    val counts = LinkedHashMap<String, Int>()
    val spellings = LinkedHashMap<String, String>()

    for (raw in places) {
        val place = raw?.trim().orEmpty()
        if (place.isEmpty()) continue
        val key = place.lowercase()
        counts[key] = (counts[key] ?: 0) + 1
        spellings.getOrPut(key) { place }
    }

    return counts.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
        )
        .mapNotNull { spellings[it.key] }
}

/**
 * Which of [known] to offer for what the user has [typed] so far.
 *
 * An empty box offers everything: tapping a place without typing at all is the fastest path, and
 * the most common one. Once there is text, places that *start* with it come before places that
 * merely contain it — typing "t" should reach "Tea Break" before "Taste Of Parika" only by
 * frequency, not put "Chai Point" ahead of both.
 *
 * A place identical to what is already typed is dropped: tapping it would do nothing, and it
 * would sit there looking like the field was not finished.
 */
fun suggestPlaces(known: List<String>, typed: String): List<String> {
    val query = typed.trim()
    if (query.isEmpty()) return known

    val alreadyTyped = query.lowercase()
    val startsWith = mutableListOf<String>()
    val contains = mutableListOf<String>()

    for (place in known) {
        if (place.lowercase() == alreadyTyped) continue
        when {
            place.startsWith(query, ignoreCase = true) -> startsWith += place
            place.contains(query, ignoreCase = true) -> contains += place
        }
    }

    return startsWith + contains
}
