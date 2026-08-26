package com.reevan.reevzmealz.util

import kotlin.math.abs

/** Formats paise for display: 42000 -> "₹420", 42050 -> "₹420.50". */
fun formatPaise(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val magnitude = abs(paise)
    val rupees = magnitude / 100
    val remainder = magnitude % 100
    return if (remainder == 0L) {
        "$sign₹$rupees"
    } else {
        "$sign₹$rupees.${remainder.toString().padStart(2, '0')}"
    }
}

/**
 * Renders paise as plain rupees suitable for prefilling a text field: 12000 -> "120",
 * 12050 -> "120.50". No currency symbol, because the field shows its own prefix.
 */
fun paiseToEditableRupees(paise: Int): String {
    val rupees = paise / 100
    val remainder = paise % 100
    return if (remainder == 0) {
        rupees.toString()
    } else {
        "$rupees.${remainder.toString().padStart(2, '0')}"
    }
}

/**
 * Parses rupees as typed by hand ("42", "42.5", "42.50") into paise.
 * Blank input counts as zero, since a home-cooked meal may have no cost worth entering.
 * Returns null when the text is not a valid amount, so the caller can block the save.
 */
fun parseRupeesToPaise(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    if (!AMOUNT.matches(trimmed)) return null
    val parts = trimmed.split('.')
    val rupees = parts[0].ifEmpty { "0" }.toIntOrNull() ?: return null
    val paise = parts.getOrNull(1)?.ifEmpty { "0" }?.padEnd(2, '0')?.toIntOrNull() ?: 0
    return rupees * 100 + paise
}

private val AMOUNT = Regex("""^\d{0,7}(\.\d{0,2})?$""")
