package com.example.meditationparticles.reminder

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
import com.example.meditationparticles.MainActivity
import com.example.meditationparticles.R
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.affirmations.AffirmationListKind
import com.example.meditationparticles.navigation.NavigationIntentExtras
import com.example.meditationparticles.navigation.SereneDestination
import kotlinx.coroutines.runBlocking

class AffirmationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val listKind = AffirmationListKind.fromStored(
            intent?.getStringExtra(AffirmationReminderScheduler.EXTRA_LIST_KIND),
        )

        val affirmationText = runBlocking {
            AppGraph.affirmations(context, listKind).randomAffirmation()?.text
                ?: when (listKind) {
                    AffirmationListKind.Affirmations -> "Take a moment for a gentle affirmation."
                    AffirmationListKind.KatiesLoveList -> "Take a moment with Katie's Love List."
                }
        }

        val channelId = when (listKind) {
            AffirmationListKind.Affirmations -> "affirmation_reminder"
            AffirmationListKind.KatiesLoveList -> "katie_love_list_reminder"
        }
        val channelName = when (listKind) {
            AffirmationListKind.Affirmations -> "Affirmation Reminders"
            AffirmationListKind.KatiesLoveList -> "Katie's Love List Reminders"
        }
        val notificationTitle = when (listKind) {
            AffirmationListKind.Affirmations -> "Daily Affirmation"
            AffirmationListKind.KatiesLoveList -> "Katie's Love List"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = channelName
                },
            )
        }

        val openTab = when (listKind) {
            AffirmationListKind.Affirmations -> SereneDestination.ToolkitTab.AFFIRMATIONS
            AffirmationListKind.KatiesLoveList -> SereneDestination.ToolkitTab.KATIES_LOVE_LIST
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NavigationIntentExtras.OPEN_TOOLKIT_TAB, openTab)
        }
        val pending = android.app.PendingIntent.getActivity(
            context,
            listKind.reminderRequestCode,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(affirmationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(affirmationText))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(listKind.notificationId, notification)
    }
}
