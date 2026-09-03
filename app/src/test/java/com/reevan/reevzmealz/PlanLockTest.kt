package com.reevan.reevzmealz

import com.reevan.reevzmealz.ui.plan.PlanLock
import com.reevan.reevzmealz.ui.plan.isOpen
import com.reevan.reevzmealz.ui.plan.planLock
import com.reevan.reevzmealz.util.startOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanLockTest {

    private val today = startOfDay(System.currentTimeMillis())
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun tomorrow_and_beyond_are_editable() {
        assertEquals(PlanLock.OPEN, planLock(today + day, today))
        assertEquals(PlanLock.OPEN, planLock(today + 30 * day, today))
    }

    /** The whole point: the plan locks the moment its own day begins, not 24 hours later. */
    @Test
    fun today_is_locked_as_soon_as_it_starts() {
        assertEquals(PlanLock.DAY_UNDER_WAY, planLock(today, today))
    }

    @Test
    fun past_days_are_locked_as_history() {
        assertEquals(PlanLock.DAY_PASSED, planLock(today - day, today))
        assertEquals(PlanLock.DAY_PASSED, planLock(today - 365 * day, today))
    }

    @Test
    fun only_open_permits_writing() {
        assertTrue(planLock(today + day, today).isOpen)
        assertFalse(planLock(today, today).isOpen)
        assertFalse(planLock(today - day, today).isOpen)
    }

    /**
     * Crossing midnight flips tomorrow's plan shut without anything else changing: the same
     * dayStart that was OPEN against yesterday is DAY_UNDER_WAY against today.
     */
    @Test
    fun a_plan_locks_when_the_clock_rolls_into_its_day() {
        val planned = today
        assertEquals(PlanLock.OPEN, planLock(planned, today - day))
        assertEquals(PlanLock.DAY_UNDER_WAY, planLock(planned, today))
    }
}
