package com.reevan.reevzmealz.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reevan.reevzmealz.MainActivity
import com.reevan.reevzmealz.R
import com.reevan.reevzmealz.data.AppSettings
import com.reevan.reevzmealz.data.MealDatabase
import com.reevan.reevzmealz.util.addDays
import com.reevan.reevzmealz.util.startOfDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires at the configured time each evening. Posts a reminder only when tomorrow has nothing
 * planned at all, then re-arms itself for the next day.
 */
class PlanReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The DB read cannot happen on the main thread, so hold the broadcast open.
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = MealDatabase.getInstance(appContext)
                val settings = database.appSettingsDao().get() ?: AppSettings()

                if (settings.remindersEnabled) {
                    val tomorrow = addDays(startOfDay(System.currentTimeMillis()), 1)
                    val plannedCount = database.plannedMealDao().countForDay(tomorrow)
                    // Nothing planned at all is the trigger; a partly planned day stays quiet.
                    if (plannedCount == 0) {
                        notifyPlanTomorrow(appContext)
                    }
                    // Re-arm for the next evening.
                    PlanReminderScheduler.schedule(
                        context = appContext,
                        hour = settings.reminderHour,
                        minute = settings.reminderMinute,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notifyPlanTomorrow(context: Context) {
        // Checked inline rather than via a helper so lint can follow the dataflow. The version
        // gate matters: POST_NOTIFICATIONS only exists as a runtime permission from API 33.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val openApp = android.app.PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plan)
            .setContentTitle("What's for tomorrow?")
            .setContentText("Nothing planned yet. Pick tomorrow's meals before bed.")
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (securityException: SecurityException) {
            // Permission can be revoked between the check above and this call; a missed nudge is
            // not worth crashing over.
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meal planning reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "The nightly nudge to plan tomorrow's meals."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "plan_reminder"
        private const val NOTIFICATION_ID = 4201
    }
}
