package com.reevan.reevzmealz.ui.sin

/** Sins granted per calendar month before the user chooses their own number. */
const val DEFAULT_SIN_ALLOWANCE = 40

/** How long the allowance stays locked after being set, so it cannot be tuned for convenience. */
const val SIN_CONFIG_LOCK_DAYS = 3

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

/**
 * Where the month stands.
 *
 * [used] counts sins in the current calendar month only, so the month rolls over on its own and
 * unused sins are never carried forward.
 */
data class SinStatus(
    val allowance: Int,
    val used: Int,
) {
    /** Never negative: once the allowance is gone the month is simply failed. */
    val remaining: Int = (allowance - used).coerceAtLeast(0)

    /** The month is failed the moment nothing is left. */
    val failed: Boolean = allowance - used <= 0

    /** What [remaining] would become if [pending] more sins were confirmed. */
    fun remainingAfter(pending: Int): Int = (allowance - used - pending).coerceAtLeast(0)

    /** Whether confirming [pending] more sins would end the month in failure. */
    fun wouldFail(pending: Int): Boolean = allowance - used - pending <= 0
}

/**
 * Whether the allowance may be changed now.
 *
 * A [setAt] of null means the user has never chosen a number — the built-in default is not a
 * decision, so the first change is free. After that the 3-day lock applies.
 */
fun canEditAllowance(setAt: Long?, now: Long): Boolean =
    setAt == null || now >= unlockAt(setAt)

/** When a allowance set at [setAt] becomes editable again. */
fun unlockAt(setAt: Long): Long = setAt + SIN_CONFIG_LOCK_DAYS * MILLIS_PER_DAY

/** Whole days left before the allowance unlocks; 0 once it is editable. */
fun daysUntilUnlock(setAt: Long?, now: Long): Int {
    if (setAt == null) return 0
    val remainingMillis = unlockAt(setAt) - now
    if (remainingMillis <= 0L) return 0
    // Round up: any part of a day still counts as a day to wait.
    return ((remainingMillis + MILLIS_PER_DAY - 1L) / MILLIS_PER_DAY).toInt()
}
