package com.example.meditationparticles.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.meditationparticles.MainActivity
import com.example.meditationparticles.R
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.navigation.NavigationIntentExtras
import com.example.meditationparticles.permissions.SchedulingPermissions
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.runBlocking

class FutureSelfMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val messageId = intent?.getLongExtra(EXTRA_MESSAGE_ID, -1L) ?: -1L
        if (messageId <= 0L) return
        deliverMessage(context, messageId)
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "future_self_message_id"
        private const val CHANNEL_ID = "future_self_message"

        fun deliverMessage(context: Context, messageId: Long): Boolean {
            if (!canPostNotifications(context)) return false

            val message = runBlocking {
                AppGraph.futureSelfMessages(context).getById(messageId)
            } ?: return false
            if (message.delivered) return true

            ensureNotificationChannel(context)

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

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Message from your past self")
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

            return try {
                NotificationManagerCompat.from(context).notify(messageId.toInt(), notification)
                runBlocking {
                    AppGraph.futureSelfMessages(context).markDelivered(messageId)
                }
                true
            } catch (_: SecurityException) {
                false
            }
        }

        fun canPostNotifications(context: Context): Boolean =
            SchedulingPermissions.canPostNotifications(context)

        private fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Future Self Messages",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Scheduled messages to your future self"
                },
            )
        }
    }
}
