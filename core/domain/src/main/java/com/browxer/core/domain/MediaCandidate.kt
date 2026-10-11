package com.browxer.core.domain

data class MediaCandidate(
    val url: String,
    val mimeType: String,
    val sizeBytes: Long = -1L,
    val width: Int = -1,
    val height: Int = -1,
    val durationSeconds: Double = -1.0,
    val posterUrl: String? = null,
    val pageUrl: String = "",
    val resolution: String = "unknown",
    val bandwidth: Long = 0L,
    val source: SourceLayer = SourceLayer.NETWORK_XHR
) {
    val isHls: Boolean get() = url.contains(".m3u8", ignoreCase = true)
    val isDash: Boolean get() = url.contains(".mpd", ignoreCase = true)
    val suggestedName: String
        get() = url.substringBefore('?').substringAfterLast('/')
            .substringBefore('.').ifBlank { "browxer_media" }.take(80)
}
