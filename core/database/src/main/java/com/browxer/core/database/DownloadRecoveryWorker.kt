package com.browxer.core.database

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs at app start: resets interrupted DOWNLOADING rows back to QUEUED so
 * StartupRecovery (:app) can hand them to the foreground DownloadService.
 * Kept free of any downloader dependency to avoid a
 * core:database <-> engine:downloader module dependency cycle.
 */
@HiltWorker
class DownloadRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: DownloadDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        dao.getActiveDownloads().forEach { entity ->
            dao.updateStatus(entity.id, DownloadEntity.QUEUED)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "DownloadRecovery",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DownloadRecoveryWorker>().build()
            )
        }
    }
}
