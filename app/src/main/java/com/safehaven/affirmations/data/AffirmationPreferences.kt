package com.safehaven.affirmations.data

import android.content.Context
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind

private const val KEY_REMINDER_ENABLED = "reminder_enabled"
private const val KEY_REMINDER_HOUR = "reminder_hour"
private const val KEY_REMINDER_MINUTE = "reminder_minute"

class AffirmationPreferences(
    context: Context,
    private val listKind: AffirmationListKind = AffirmationListKind.Affirmations,
) {
    private val prefs = context.getSharedPreferences(listKind.preferencesName, Context.MODE_PRIVATE)

    fun load(): AffirmationPrefsSnapshot = AffirmationPrefsSnapshot(
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 9),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
    )

    fun save(snapshot: AffirmationPrefsSnapshot) {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, snapshot.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, snapshot.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, snapshot.reminderMinute)
            .apply()
    }

    data class AffirmationPrefsSnapshot(
        val reminderEnabled: Boolean,
        val reminderHour: Int,
        val reminderMinute: Int,
    )
}
