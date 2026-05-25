package com.example.meditationparticles.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.example.meditationparticles.navigation.NavigationIntentExtras
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.runBlocking

class FutureSelfMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val messageId = intent?.getLongExtra(EXTRA_MESSAGE_ID, -1L) ?: -1L
        if (messageId <= 0L) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val message = runBlocking {
            AppGraph.futureSelfMessages(context).getById(messageId)
        } ?: return

        runBlocking {
            AppGraph.futureSelfMessages(context).markDelivered(messageId)
        }

        val channelId = "future_self_message"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Future Self Messages",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Scheduled messages to your future self"
                },
            )
        }

        val preview = when {
            message.content.isNotBlank() -> message.content
            message.audioPath != null -> "You left yourself an audio message."
            else -> "A message from your past self is ready."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NavigationIntentExtras.OPEN_TOOLKIT_TAB, SereneDestination.ToolkitTab.TOOLKIT)
            putExtra(
                NavigationIntentExtras.OPEN_TOOLKIT_TOOL_ID,
                ToolkitToolId.FutureSelfMessage.name,
            )
            putExtra(NavigationIntentExtras.FUTURE_SELF_MESSAGE_ID, messageId)
        }
        val pending = PendingIntent.getActivity(
            context,
            messageId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Message from your past self")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(messageId.toInt(), notification)
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "future_self_message_id"
    }
}
