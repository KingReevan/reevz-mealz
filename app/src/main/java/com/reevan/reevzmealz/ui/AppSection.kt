package com.reevan.reevzmealz.ui

import androidx.annotation.DrawableRes
import com.reevan.reevzmealz.R

/**
 * The app's top-level sections.
 *
 * [tabLabel] is deliberately shorter than [title]; the full name shows in the top bar.
 *
 * [inBottomBar] splits the daily-use sections from the occasional ones. Six tabs left about 68dp
 * each on a phone, which was cramped; the four that get opened every day stay in the bar and the
 * two that do not — Money Spent and Settings — live in the pause menu instead. They are still
 * ordinary full sections, reached a different way.
 */
enum class AppSection(
    val title: String,
    val tabLabel: String,
    @param:DrawableRes val icon: Int,
    val inBottomBar: Boolean = true,
) {
    TODAY("Today", "Today", R.drawable.ic_today),
    PLAN_MEAL("Plan Meal", "Plan", R.drawable.ic_plan),
    BOUGHT_ITEMS("Bought Items", "Bought", R.drawable.ic_bought),
    FOODS("Foods", "Foods", R.drawable.ic_foods),
    MONEY_SPENT("Money Spent", "Money", R.drawable.ic_money, inBottomBar = false),
    SETTINGS("Settings", "Settings", R.drawable.ic_settings, inBottomBar = false);

    companion object {
        /** The bottom bar's four tabs, in order. */
        val tabs: List<AppSection> = entries.filter { it.inBottomBar }

        /** What the pause menu offers, in order. */
        val pauseMenuSections: List<AppSection> = entries.filter { !it.inBottomBar }
    }
}
