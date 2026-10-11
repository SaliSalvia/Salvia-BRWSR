package com.browxer.app

import android.content.Context
import com.browxer.core.data.DownloadRepository
import com.browxer.core.database.DownloadEntity
import com.browxer.core.domain.DownloadTask
import com.browxer.engine.downloader.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level glue: downloads that were QUEUED/DOWNLOADING when the process died
 * are reset by DownloadRecoveryWorker (Room) and re-enqueued here into the
 * foreground DownloadService. Lives in :app so core:database never depends on
 * the downloader engine (no module cycle).
 */
@Singleton
class StartupRecovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloads: DownloadRepository
) {
    suspend fun requeueInterrupted() {
        downloads.getActiveDownloads().forEach { entity ->
            if (entity.status == DownloadEntity.DOWNLOADING) {
                downloads.updateStatus(entity.id, DownloadEntity.QUEUED)
            }
            DownloadService.enqueue(context, entity.toTask())
        }
    }

    private fun DownloadEntity.toTask() = DownloadTask(
        id = id,
        url = url,
        fileName = fileName,
        mimeType = mimeType,
        kind = when {
            sourceLayer.startsWith("HLS") -> DownloadTask.Kind.HLS
            sourceLayer.startsWith("DASH") -> DownloadTask.Kind.DASH
            sourceLayer == "YTDLP" -> DownloadTask.Kind.YTDLP
            sourceLayer == "BLOB" -> DownloadTask.Kind.BLOB
            else -> DownloadTask.Kind.DIRECT
        },
        workDir = File(context.cacheDir, "downloads/" + id).absolutePath,
        pageUrl = url
    )
}
