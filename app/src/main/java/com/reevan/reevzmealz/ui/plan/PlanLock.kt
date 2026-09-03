package com.reevan.reevzmealz.ui.plan

/**
 * Whether a day's plan can still be changed, and if not, why.
 *
 * A plan is an *intention*, so it can only be set while the day is still ahead. The moment a day
 * begins it stops being a plan and becomes a record of what is happening, which Today owns through
 * its Edit Mode. Without this, the same day could be rewritten from two screens with different
 * meanings — and worse, yesterday's plan could be quietly edited after the fact to match what was
 * actually eaten, which would make the plan-versus-actual comparison meaningless.
 */
enum class PlanLock {
    /** The day is still in the future; the plan is fully editable. */
    OPEN,

    /** The day is today. It has begun, so what happens now belongs to Today's Edit Mode. */
    DAY_UNDER_WAY,

    /** The day is over. Its plan is history and stays as it was. */
    DAY_PASSED,
}

/**
 * [dayStart] and [today] must both be local midnights (see `startOfDay`); this compares days, not
 * instants, which is what makes the cutoff exactly 12 am rather than 24 hours from now.
 */
fun planLock(dayStart: Long, today: Long): PlanLock = when {
    dayStart > today -> PlanLock.OPEN
    dayStart == today -> PlanLock.DAY_UNDER_WAY
    else -> PlanLock.DAY_PASSED
}

/** True only for [PlanLock.OPEN] — the one state in which the plan may be written to. */
val PlanLock.isOpen: Boolean
    get() = this == PlanLock.OPEN
