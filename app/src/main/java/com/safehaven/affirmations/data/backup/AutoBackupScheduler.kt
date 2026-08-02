package com.safehaven.affirmations.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AutoBackupScheduler(
    private val context: Context,
) {
    fun apply(snapshot: AutoBackupSnapshot) {
        val workManager = WorkManager.getInstance(context)
        if (!snapshot.autoBackupEnabled || snapshot.destinationTreeUri.isNullOrBlank()) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val intervalDays = snapshot.frequency.intervalDays.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    companion object {
        const val WORK_NAME = "auto_backup"
    }
}
