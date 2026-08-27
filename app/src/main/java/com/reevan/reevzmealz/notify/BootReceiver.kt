package com.reevan.reevzmealz.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reevan.reevzmealz.data.AppSettings
import com.reevan.reevzmealz.data.MealDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Alarms do not survive a reboot, so the reminder is re-armed here from the stored settings.
 * Without this the nightly nudge silently stops after the phone restarts.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = MealDatabase.getInstance(appContext).appSettingsDao().get()
                    ?: AppSettings()
                PlanReminderScheduler.apply(
                    context = appContext,
                    enabled = settings.remindersEnabled,
                    hour = settings.reminderHour,
                    minute = settings.reminderMinute,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
