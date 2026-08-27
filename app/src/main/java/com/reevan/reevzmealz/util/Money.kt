package com.reevan.reevzmealz.util

import kotlin.math.abs

/**
 * Formats paise for display: 42000 -> "₹420", 42050 -> "₹420.50", 5820000 -> "₹58,200".
 *
 * Rupees are grouped Indian-style (last three digits, then twos), so a year's spend reads as
 * ₹1,20,000 rather than ₹120000.
 */
fun formatPaise(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val magnitude = abs(paise)
    val rupees = magnitude / 100
    val remainder = magnitude % 100
    val grouped = groupIndian(rupees)
    return if (remainder == 0L) {
        "$sign₹$grouped"
    } else {
        "$sign₹$grouped.${remainder.toString().padStart(2, '0')}"
    }
}

/**
 * Renders paise as plain rupees suitable for prefilling a text field: 12000 -> "120",
 * 12050 -> "120.50". No currency symbol and no grouping, because the text has to parse back.
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

/**
 * Indian digit grouping: three digits closest to the decimal point, then groups of two.
 *
 * Hand-rolled rather than delegating to `NumberFormat` for an Indian locale, so the output does
 * not vary with the platform's CLDR data and can be asserted exactly in tests. Java's
 * DecimalFormat cannot express this pattern at all — it supports only one grouping size.
 */
private fun groupIndian(rupees: Long): String {
    val digits = rupees.toString()
    if (digits.length <= 3) return digits

    val head = digits.substring(0, digits.length - 3)
    val lastThree = digits.substring(digits.length - 3)

    val grouped = StringBuilder()
    var index = head.length
    while (index > 0) {
        val start = maxOf(0, index - 2)
        if (grouped.isNotEmpty()) grouped.insert(0, ',')
        grouped.insert(0, head.substring(start, index))
        index = start
    }
    return "$grouped,$lastThree"
}

private val AMOUNT = Regex("""^\d{0,7}(\.\d{0,2})?$""")
