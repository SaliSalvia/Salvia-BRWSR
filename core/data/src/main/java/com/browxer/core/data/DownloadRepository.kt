package com.browxer.core.data

import com.browxer.core.database.DownloadDao
import com.browxer.core.database.DownloadEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(private val dao: DownloadDao) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()
    suspend fun getActiveDownloads(): List<DownloadEntity> = dao.getActiveDownloads()
    suspend fun getById(id: Long): DownloadEntity? = dao.getById(id)

    suspend fun enqueueDownload(
        url: String, fileName: String, mimeType: String?, sourceLayer: String
    ): Long = dao.insertDownload(
        DownloadEntity(
            url = url, fileName = fileName, mimeType = mimeType,
            status = DownloadEntity.QUEUED, sourceLayer = sourceLayer
        )
    )

    suspend fun updateProgress(id: Long, progress: Int, downloaded: Long, total: Long, speed: Long) =
        dao.updateProgress(id, progress, downloaded, total, speed)

    suspend fun updateStatus(id: Long, status: String, error: String? = null) =
        dao.updateStatus(id, status, error)

    suspend fun delete(id: Long) = dao.deleteDownload(id)
}
