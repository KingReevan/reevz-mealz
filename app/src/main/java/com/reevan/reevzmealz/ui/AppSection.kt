package com.reevan.reevzmealz.ui

import androidx.annotation.DrawableRes
import com.reevan.reevzmealz.R

/**
 * The app's top-level sections, in the order they appear in the bottom bar.
 *
 * [tabLabel] is deliberately shorter than [title] so all six fit a phone width; the full
 * name shows in the top bar.
 */
enum class AppSection(
    val title: String,
    val tabLabel: String,
    @param:DrawableRes val icon: Int,
) {
    TODAY("Today", "Today", R.drawable.ic_today),
    PLAN_MEAL("Plan Meal", "Plan", R.drawable.ic_plan),
    BOUGHT_ITEMS("Bought Items", "Bought", R.drawable.ic_bought),
    FOODS("Foods", "Foods", R.drawable.ic_foods),
    MONEY_SPENT("Money Spent", "Money", R.drawable.ic_money),
    SETTINGS("Settings", "Settings", R.drawable.ic_settings),
}
