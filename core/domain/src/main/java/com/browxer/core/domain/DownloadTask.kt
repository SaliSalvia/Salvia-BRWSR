package com.browxer.core.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadTask(
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String? = null,
    val kind: Kind = Kind.DIRECT,
    val workDir: String,
    val pageUrl: String = "",
    val cookies: String? = null,
    val userAgent: String? = null,
    val dashVideoUrl: String? = null, // DASH: separate A/V reps
    val dashAudioUrl: String? = null
) : Parcelable {
    enum class Kind { DIRECT, HLS, DASH, YTDLP, BLOB }
}

data class DownloadResult(
    val output: java.io.File,
    val videoFile: java.io.File? = null,
    val audioFile: java.io.File? = null,
    val needsMux: Boolean = false
)
