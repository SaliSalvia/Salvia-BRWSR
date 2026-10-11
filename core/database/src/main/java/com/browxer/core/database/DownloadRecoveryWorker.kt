package com.browxer.core.database

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.browxer.core.domain.DownloadTask
import com.browxer.engine.downloader.DownloadService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Runs at app start: re-enqueues interrupted downloads (QUEUED/DOWNLOADING)
 * after a process kill. Registered under unique name "DownloadRecovery".
 */
@HiltWorker
class DownloadRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: DownloadDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val active = dao.getActiveDownloads()
        active.forEach { e ->
            dao.updateStatus(e.id, DownloadEntity.QUEUED)
            DownloadService.enqueue(applicationContext, e.toTask())
        }
        return Result.success()
    }

    private fun DownloadEntity.toTask() = DownloadTask(
        id = id, url = url, fileName = fileName, mimeType = mimeType,
        kind = when (sourceLayer) {
            "HLS" -> DownloadTask.Kind.HLS
            "DASH" -> DownloadTask.Kind.DASH
            "YTDLP" -> DownloadTask.Kind.YTDLP
            "BLOB" -> DownloadTask.Kind.BLOB
            else -> DownloadTask.Kind.DIRECT
        },
        workDir = File(applicationContext.cacheDir, "downloads/${id}").absolutePath,
        pageUrl = url
    )

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
