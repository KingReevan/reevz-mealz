package com.reevan.reevzmealz

import com.reevan.reevzmealz.ui.sin.DEFAULT_SIN_ALLOWANCE
import com.reevan.reevzmealz.ui.sin.SIN_CONFIG_LOCK_DAYS
import com.reevan.reevzmealz.ui.sin.SinStatus
import com.reevan.reevzmealz.ui.sin.canEditAllowance
import com.reevan.reevzmealz.ui.sin.daysUntilUnlock
import com.reevan.reevzmealz.ui.sin.unlockAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SinStatusTest {

    private val day = 24L * 60L * 60L * 1000L
    private val now = 1_800_000_000_000L

    @Test
    fun the_default_allowance_is_forty() {
        assertEquals(40, DEFAULT_SIN_ALLOWANCE)
    }

    @Test
    fun a_fresh_month_has_the_whole_allowance() {
        val status = SinStatus(allowance = 40, used = 0)
        assertEquals(40, status.remaining)
        assertFalse(status.failed)
    }

    @Test
    fun each_sin_reduces_what_is_left() {
        assertEquals(38, SinStatus(allowance = 40, used = 2).remaining)
    }

    @Test
    fun the_month_is_failed_once_nothing_is_left() {
        val status = SinStatus(allowance = 40, used = 40)
        assertEquals(0, status.remaining)
        assertTrue(status.failed)
    }

    @Test
    fun remaining_never_goes_negative_but_still_counts_as_failed() {
        val status = SinStatus(allowance = 40, used = 45)
        assertEquals(0, status.remaining)
        assertTrue(status.failed)
    }

    @Test
    fun pending_sins_are_projected_before_confirming() {
        val status = SinStatus(allowance = 40, used = 36)
        assertEquals(4, status.remaining)
        assertEquals(2, status.remainingAfter(2))
        assertFalse(status.wouldFail(2))
        assertTrue(status.wouldFail(4))
    }

    @Test
    fun confirming_the_last_sins_would_fail_the_month() {
        val status = SinStatus(allowance = 2, used = 0)
        assertTrue(status.wouldFail(2))
        assertEquals(0, status.remainingAfter(2))
    }

    @Test
    fun an_allowance_never_set_is_editable_immediately() {
        assertTrue(canEditAllowance(setAt = null, now = now))
        assertEquals(0, daysUntilUnlock(setAt = null, now = now))
    }

    @Test
    fun a_freshly_set_allowance_is_locked() {
        assertFalse(canEditAllowance(setAt = now, now = now))
        assertEquals(SIN_CONFIG_LOCK_DAYS, daysUntilUnlock(setAt = now, now = now))
    }

    @Test
    fun the_lock_holds_for_three_days() {
        assertFalse(canEditAllowance(setAt = now, now = now + day))
        assertFalse(canEditAllowance(setAt = now, now = now + 2 * day))
        // Just shy of three days is still locked.
        assertFalse(canEditAllowance(setAt = now, now = now + 3 * day - 1))
    }

    @Test
    fun the_lock_lifts_after_exactly_three_days() {
        assertTrue(canEditAllowance(setAt = now, now = now + 3 * day))
        assertEquals(0, daysUntilUnlock(setAt = now, now = now + 3 * day))
    }

    @Test
    fun days_until_unlock_rounds_up_so_a_partial_day_still_waits() {
        // Two and a half days in: one day still to wait, not zero.
        assertEquals(1, daysUntilUnlock(setAt = now, now = now + 2 * day + day / 2))
    }

    @Test
    fun unlock_time_is_three_days_after_setting() {
        assertEquals(now + 3 * day, unlockAt(now))
    }
}
