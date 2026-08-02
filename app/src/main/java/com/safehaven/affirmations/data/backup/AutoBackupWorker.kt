package com.safehaven.affirmations.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safehaven.affirmations.data.AppGraph

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val snapshot = AppGraph.autoBackupPreferences(applicationContext).load()
        if (!snapshot.autoBackupEnabled || snapshot.destinationTreeUri.isNullOrBlank()) {
            return Result.success()
        }
        val result = AppGraph.autoBackup(applicationContext).runBackup(force = false)
        return if (result.success) Result.success() else Result.retry()
    }
}
