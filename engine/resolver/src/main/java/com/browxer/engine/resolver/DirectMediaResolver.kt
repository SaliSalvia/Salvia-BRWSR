package com.browxer.engine.resolver

import com.browxer.core.domain.MediaCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class DirectProbe(
    val finalUrl: String,
    val mimeType: String,
    val lengthBytes: Long,
    val acceptsRanges: Boolean,
    val fileName: String?
)

@Singleton
class DirectMediaResolver @Inject constructor(
    private val http: OkHttpClient
) {
    /** HEAD/Range(0-1) probe. Suspend + IO — never call on Main. */
    suspend fun probe(url: String): DirectProbe = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("Range", "bytes=0-1").build()
        http.newCall(req).execute().use { resp ->
            val mime = resp.header("Content-Type") ?: ""
            check(!mime.contains("text/html", true)) {
                "URL returned HTML, not media — likely a redirect/login page"
            }
            check(resp.isSuccessful) { "Server returned ${resp.code}" }
            val total = resp.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                ?: resp.header("Content-Length")?.toLongOrNull() ?: -1L
            DirectProbe(
                finalUrl = resp.request.url.toString(), // follows redirects
                mimeType = mime,
                lengthBytes = total,
                acceptsRanges = resp.header("Accept-Ranges")?.contains("bytes") == true,
                fileName = resp.header("Content-Disposition")
                    ?.substringAfter("filename=")?.trim('"')
            )
        }
    }

    suspend fun probe(c: MediaCandidate) = probe(c.url)
}
