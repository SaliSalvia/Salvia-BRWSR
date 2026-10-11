package com.browxer.engine.ranker

import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import javax.inject.Inject
import javax.inject.Singleton

/** Strict gatekeeper — kills false positives before scoring. */
@Singleton
class MediaSniffAdmission @Inject constructor() {

    companion object {
        private val SEGMENT_EXT = Regex(
            """\.(ts|m4s|cmfv|cmfa)(\?|$)|/seg-|/segment|/chunk|range=|/frag-""",
            RegexOption.IGNORE_CASE)
        private val WEB_MANIFEST = Regex("""\.webmanifest|manifest\.json""", RegexOption.IGNORE_CASE)
        private val AD_DOMAINS = Regex(
            """doubleclick|googlesyndication|adservice|taboola|outbrain""",
            RegexOption.IGNORE_CASE)
        private val TRACKING_PARAMS = listOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "_t", "cachebust", "rnd", "timestamp")
        private val DATA_IMAGE_OK =
            Regex("""^data:image/(jpeg|png|webp|gif);base64,""", RegexOption.IGNORE_CASE)
    }

    fun admit(c: MediaCandidate): Boolean {
        val url = c.url
        if (url.isBlank() || url.length > 4096) return false
        if (url.startsWith("data:"))
            return DATA_IMAGE_OK.containsMatchIn(url) && c.sizeBytes in 1..(256 * 1024)
        if (url.startsWith("blob:")) return c.source == SourceLayer.BLOB
        if (SEGMENT_EXT.containsMatchIn(url) && c.source == SourceLayer.NETWORK_XHR) return false
        if (WEB_MANIFEST.containsMatchIn(url)) return false
        if (AD_DOMAINS.containsMatchIn(url)) return false
        if (c.mimeType.contains("text/html", true)) return false

        // Tiny images rejected unless they'd be the only candidate on the page
        if (c.source == SourceLayer.DOM_IMAGE &&
            c.width in 1..199 && c.height in 1..199) return false

        // 0-byte responses are junk; partial-range fragments are re-probed by resolver
        if (c.sizeBytes == 0L) return false
        return true
    }

    /** Strips tracking params so dedupe works across ad/cdn variants. */
    fun normalize(url: String): String {
        val base = url.substringBefore('#')
        val qStart = base.indexOf('?')
        if (qStart == -1) return base.lowercase().take(128)
        val path = base.substring(0, qStart)
        val kept = base.substring(qStart + 1).split('&')
            .filter { it.substringBefore('=').lowercase() !in TRACKING_PARAMS }
            .sorted()
            .joinToString("&")
        return (if (kept.isEmpty()) path else "$path?$kept").lowercase().take(128)
    }
}
