package com.reevan.reevzmealz.ui.common

import com.reevan.reevzmealz.data.MealPlace
import com.reevan.reevzmealz.data.MealType
import com.reevan.reevzmealz.data.PlannedFood

/**
 * One meal slot of a day and the foods planned into it.
 *
 * [costPaise] counts only food bought outside. Homecooked food deliberately carries no price —
 * its real cost varies per shop and is tracked through Bought Items — so it contributes nothing
 * here rather than contributing a guess.
 */
data class PlanSlot(
    val type: MealType,
    val foods: List<PlannedFood>,
) {
    val costPaise: Long =
        foods.filter { it.source == MealPlace.OUT }
            .sumOf { (it.pricePaise ?: 0).toLong() }

    val isEmpty: Boolean = foods.isEmpty()

    /** True when the slot holds food but none of it was bought outside, so no price applies. */
    val isFullyHomecooked: Boolean =
        foods.isNotEmpty() && foods.none { it.source == MealPlace.OUT }
}

/**
 * Every meal slot for a day, in [MealType] declaration order, including the ones with nothing
 * planned. The UI always shows all four, so absent slots are empty rather than missing.
 */
fun buildSlots(planned: List<PlannedFood>): List<PlanSlot> {
    val byType = planned.groupBy { it.type }
    return MealType.entries.map { type ->
        PlanSlot(type = type, foods = byType[type].orEmpty())
    }
}

/** Total planned spend for a day: the sum of its slots, so again outside food only. */
fun totalCostPaise(slots: List<PlanSlot>): Long = slots.sumOf { it.costPaise }
