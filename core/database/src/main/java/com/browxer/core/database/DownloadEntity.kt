package com.browxer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String? = null,
    val mimeType: String? = null,
    val status: String,              // QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED
    val progress: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBps: Long = 0L,
    val errorMessage: String? = null,
    val sourceLayer: String,         // OG_META, DOM_VIDEO, NETWORK_XHR, HLS, DASH, YTDLP, BLOB
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val QUEUED = "QUEUED"; const val DOWNLOADING = "DOWNLOADING"
        const val COMPLETED = "COMPLETED"; const val FAILED = "FAILED"; const val PAUSED = "PAUSED"
    }
}
