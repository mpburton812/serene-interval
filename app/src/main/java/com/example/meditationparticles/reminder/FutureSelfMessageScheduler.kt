package com.example.meditationparticles.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.meditationparticles.data.AppGraph
import kotlinx.coroutines.runBlocking

object FutureSelfMessageScheduler {
    private const val REQUEST_CODE_BASE = 52000

    fun schedule(context: Context, messageId: Long, triggerAtMillis: Long): Boolean {
        if (triggerAtMillis <= System.currentTimeMillis()) return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context, messageId)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        return try {
            when {
                canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
                canScheduleExact -> {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
                else -> {
                    @Suppress("DEPRECATION")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
            true
        } catch (_: SecurityException) {
            scheduleInexactFallback(alarmManager, triggerAtMillis, pendingIntent)
        }
    }

    private fun scheduleInexactFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, messageId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context, messageId))
    }

    fun rescheduleAll(context: Context) {
        runBlocking {
            val pending = AppGraph.futureSelfMessages(context)
                .getPendingAfter(System.currentTimeMillis())
            pending.forEach { message ->
                schedule(context, message.id, message.scheduledAtMillis)
            }
        }
    }

    private fun alarmPendingIntent(context: Context, messageId: Long): PendingIntent {
        val intent = Intent(context, FutureSelfMessageReceiver::class.java).apply {
            putExtra(FutureSelfMessageReceiver.EXTRA_MESSAGE_ID, messageId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(messageId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodeFor(messageId: Long): Int =
        REQUEST_CODE_BASE + (messageId % 10_000).toInt()
}
