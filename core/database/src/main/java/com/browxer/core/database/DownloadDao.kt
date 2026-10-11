package com.browxer.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING','QUEUED')")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    // DIRECT update — never a read-modify-write of the full entity (bug rule #4)
    @Query("""UPDATE downloads SET progress = :progress, downloadedBytes = :downloaded,
        totalBytes = :total, speedBps = :speed WHERE id = :id""")
    suspend fun updateProgress(id: Long, progress: Int, downloaded: Long, total: Long, speed: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String? = null)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)
}
