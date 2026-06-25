package com.example.meditationparticles.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meditationparticles.data.AppGraph

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val manager = AppGraph.autoBackup(applicationContext)
        val snapshot = AppGraph.autoBackupPreferences(applicationContext).load()
        if (!snapshot.autoBackupEnabled || snapshot.destinationTreeUri.isNullOrBlank()) {
            return Result.success()
        }
        val result = manager.runBackup(force = false)
        return if (result.success) Result.success() else Result.retry()
    }
}
