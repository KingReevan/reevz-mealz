package com.reevan.reevzmealz.util

/**
 * Capitalises the first letter of every word: "green tea (tea break)" -> "Green Tea (Tea Break)".
 *
 * The rest of each word is left exactly as typed, rather than lower-cased. Title-casing properly
 * would turn "KFC" into "Kfc" and "McDonald" into "Mcdonald"; only the first letter was ever the
 * tedious part, so only the first letter is touched.
 *
 * Three rules decide where a word starts, and the punctuation ones matter for real food names:
 * - whitespace starts a new word;
 * - leading punctuation does **not** consume the word, so "(tea break)" capitalises the T. An
 *   opening bracket is common in these names, where the shop is written in parentheses;
 * - punctuation *inside* a word does not start a new one, so "McDonald's" keeps its lowercase s
 *   after the apostrophe, and a leading digit holds the word closed so "2nd" is not "2Nd".
 *
 * Uses [Char.uppercaseChar], which is locale-independent, so the result cannot change with the
 * device's locale (the Turkish dotless i being the classic way that goes wrong).
 *
 * Spacing is preserved character for character — the caller trims if it wants to.
 */
fun capitalizeWords(text: String): String {
    if (text.isEmpty()) return text

    val result = StringBuilder(text.length)
    var atWordStart = true
    for (character in text) {
        when {
            character.isWhitespace() -> {
                atWordStart = true
                result.append(character)
            }

            atWordStart && character.isLetter() -> {
                result.append(character.uppercaseChar())
                atWordStart = false
            }

            character.isLetterOrDigit() -> {
                result.append(character)
                atWordStart = false
            }

            // Punctuation neither opens nor closes a word: it just goes through.
            else -> result.append(character)
        }
    }
    return result.toString()
}
