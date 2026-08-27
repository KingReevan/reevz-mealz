package com.reevan.reevzmealz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/**
 * App-wide preferences. Single row, id 1.
 *
 * Kept separate from [SinSettings] because that one carries its own 3-day edit lock; mixing them
 * would put unrelated fields behind that lock. New preferences belong here.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = true,
    /** Local time of the nightly plan reminder. Defaults to 19:00. */
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_REMINDER_HOUR = 19
    }
}
