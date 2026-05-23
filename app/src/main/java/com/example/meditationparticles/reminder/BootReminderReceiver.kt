package com.example.meditationparticles.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MeditationReminderScheduler.syncFromPreferences(context)
            AffirmationReminderScheduler.syncFromPreferences(context)
        }
    }
}
