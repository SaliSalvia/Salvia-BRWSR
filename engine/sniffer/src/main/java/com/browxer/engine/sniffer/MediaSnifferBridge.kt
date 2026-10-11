package com.browxer.engine.sniffer

import android.os.Handler
import android.os.HandlerThread
import android.webkit.JavascriptInterface
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import com.browxer.engine.resolver.BlobResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardened JS->Kotlin bridge (CVE-2024-46961 posture):
 *  - primitive-only @JavascriptInterface args (String)
 *  - hard length caps per call (8 KB metadata / 1 MB chunk)
 *  - scheme allow-list (http/https/blob only)
 *  - JSON parse only — the payload is NEVER evaluated
 *  - all work posted to a dedicated background HandlerThread
 */
@Singleton
class MediaSnifferBridge @Inject constructor(
    private val scope: CoroutineScope, // limited to Dispatchers.Default
    private val blobResolver: BlobResolver // reassembles chunked blob payloads
) {
    private val handlerThread = HandlerThread("browxer-sniffer").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private val _candidates = MutableSharedFlow<MediaCandidate>(replay = 128, extraBufferCapacity = 512)
    val candidates: SharedFlow<MediaCandidate> = _candidates

    private val _tooLargeBlobs = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val tooLargeBlobs: SharedFlow<Long> = _tooLargeBlobs

    companion object {
        private const val MAX_META_JSON = 8 * 1024          // 8 KB per metadata call
        private const val MAX_CHUNK_BASE64 = 512 * 1024 * 2 // base64 of a 512 KB chunk
        private val ALLOWED_SCHEMES = setOf("http", "https", "blob")
    }

    @JavascriptInterface
    fun onMediaFound(json: String) {
        if (json.length > MAX_META_JSON) return
        handler.post { parseCandidates(json) }
    }

    @JavascriptInterface
    fun onIframeMediaFound(json: String) {
        if (json.length > MAX_META_JSON) return
        handler.post { parseCandidates(json) }
    }

    @JavascriptInterface
    fun onBlobChunk(metaJson: String, base64Chunk: String) {
        if (metaJson.length > 2048 || base64Chunk.length > MAX_CHUNK_BASE64) return
        handler.post {
            runCatching {
                val meta = JSONObject(metaJson)
                val blobId = meta.optString("blobId")
                val chunkIndex = meta.optInt("chunkIndex", 0)
                // Persist the chunk so BlobResolver can reassemble the payload later
                blobResolver.onChunk(blobId, chunkIndex, meta.optLong("size", -1), base64Chunk)
                // Surface the blob as a downloadable candidate once, on its first chunk
                if (chunkIndex == 0) {
                    _candidates.tryEmit(
                        MediaCandidate(
                            url = "blob:" + blobId,
                            mimeType = meta.optString("type", "video/mp4"),
                            sizeBytes = meta.optLong("size", -1),
                            source = SourceLayer.BLOB,
                            pageUrl = meta.optString("pageUrl")
                        )
                    )
                }
            }
        }
    }

    @JavascriptInterface
    fun onBlobTooLarge(metaJson: String) {
        if (metaJson.length > 2048) return
        handler.post {
            runCatching {
                val meta = JSONObject(metaJson)
                _tooLargeBlobs.tryEmit(meta.optLong("size", -1)) // honest signal, never faked
            }
        }
    }

    private fun parseCandidates(json: String) {
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val url = o.optString("url")
                if (!isAllowedUrl(url)) continue
                _candidates.tryEmit(
                    MediaCandidate(
                        url = url,
                        mimeType = o.optString("type", "video"),
                        sizeBytes = o.optLong("size", -1),
                        width = o.optInt("width", -1),
                        height = o.optInt("height", -1),
                        durationSeconds = o.optDouble("duration", -1.0),
                        posterUrl = o.optString("poster").takeIf { it.isNotBlank() },
                        pageUrl = o.optString("pageUrl"),
                        source = SourceLayer.from(o.optString("source"))
                    )
                )
            }
        }
    }

    private fun isAllowedUrl(url: String): Boolean {
        if (url.isBlank() || url.length > 4096) return false
        if (url.startsWith("blob:")) return true
        val scheme = url.substringBefore("://").lowercase()
        return scheme in ALLOWED_SCHEMES
    }
}
