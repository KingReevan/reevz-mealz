package com.reevan.reevzmealz.util

import com.reevan.reevzmealz.data.MealPlace

/** Label for a food's source in the Foods section. */
fun foodSourceLabel(source: MealPlace): String =
    if (source == MealPlace.HOME) "Homecooked" else "Outside"

/**
 * Secondary line for a food row. A price is only shown for food bought outside — homecooked
 * items deliberately carry no price.
 */
fun foodSubtitle(source: MealPlace, pricePaise: Int?): String =
    if (source == MealPlace.HOME) {
        foodSourceLabel(source)
    } else {
        foodSourceLabel(source) + " · " + formatPaise((pricePaise ?: 0).toLong())
    }
