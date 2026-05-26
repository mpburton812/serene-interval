package com.example.meditationparticles.data.onenote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.meditationparticles.data.AppGraph

class OneNoteSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = AppGraph.oneNoteSync(applicationContext)
        if (!repository.canAutoSync()) return Result.success()
        val runResult = repository.processQueue(force = false)
        return if (runResult.failedCount > 0 && runResult.syncedCount == 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
