package com.browxer.core.domain

/**
 * Where a candidate was discovered — seeds the ranker score.
 */
enum class SourceLayer(val baseScore: Int) {
    OG_META(100),        // og:video, twitter:player:stream
    JSON_LD(100),        // schema.org VideoObject contentUrl/embedUrl
    YTDLP(95),           // yt-dlp fallback extraction
    HLS_VARIANT(90),     // resolved #EXT-X-STREAM-INF variant
    DASH_REP(85),        // resolved <Representation> from .mpd
    DOM_VIDEO(70),
    DOM_AUDIO(70),
    DOM_IMAGE(50),       // <img> with naturalWidth >= 800
    NETWORK_XHR(40),     // fetch/XHR/WebView interception
    BLOB(35);            // captured via createObjectURL override

    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name.equals(s, true) } ?: NETWORK_XHR
    }
}
