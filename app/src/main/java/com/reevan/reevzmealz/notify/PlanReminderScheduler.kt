package com.reevan.reevzmealz.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules the nightly "plan tomorrow" reminder.
 *
 * Uses AlarmManager rather than WorkManager: this is a time-of-day delivery, which WorkManager is
 * explicitly not designed for. A single one-shot alarm is armed and then re-armed each time it
 * fires, and re-armed again after a reboot, which avoids setRepeating's drift.
 *
 * setAndAllowWhileIdle gets close-enough timing through Doze without needing the Android 12+
 * exact-alarm permission — a nightly nudge does not have to land on the second.
 */
object PlanReminderScheduler {

    private const val REQUEST_CODE = 4201

    /** Arms or cancels the reminder to match the current settings. */
    fun apply(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        if (enabled) {
            schedule(context, hour, minute)
        } else {
            cancel(context)
        }
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerAt(System.currentTimeMillis(), hour, minute)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PlanReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * The next moment at [hour]:[minute] in the device's own timezone, strictly after [now].
     *
     * Device-local rather than a hardcoded Asia/Kolkata: the phone is in India, so local time is
     * IST, and this way the reminder still makes sense at 7pm wherever the phone actually is.
     */
    fun nextTriggerAt(now: Long, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
