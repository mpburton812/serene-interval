package com.example.meditationparticles.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.domain.affirmations.AffirmationListKind
import java.util.Calendar

object AffirmationReminderScheduler {
    const val EXTRA_LIST_KIND = "affirmation_list_kind"

    fun schedule(context: Context, listKind: AffirmationListKind, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime(hour, minute),
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent(context, listKind),
        )
    }

    fun cancel(context: Context, listKind: AffirmationListKind) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, listKind))
    }

    fun syncFromPreferences(context: Context, listKind: AffirmationListKind = AffirmationListKind.Affirmations) {
        val prefs = AffirmationPreferences(context, listKind).load()
        if (prefs.reminderEnabled) {
            schedule(context, listKind, prefs.reminderHour, prefs.reminderMinute)
        } else {
            cancel(context, listKind)
        }
    }

    fun syncAllFromPreferences(context: Context) {
        AffirmationListKind.entries.forEach { syncFromPreferences(context, it) }
    }

    private fun reminderPendingIntent(context: Context, listKind: AffirmationListKind): PendingIntent {
        val intent = Intent(context, AffirmationReminderReceiver::class.java).apply {
            putExtra(EXTRA_LIST_KIND, listKind.name)
        }
        return PendingIntent.getBroadcast(
            context,
            listKind.reminderRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
