package com.browxer.engine.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

class HlsLiveException : Exception("HLS playlist is a live stream (no EXT-X-ENDLIST)")
class HlsEncryptedException : Exception("HLS playlist uses AES-128/SAMPLE-AES encryption")

data class HlsVariant(
    val url: String,
    val resolution: String,
    val bandwidth: Int,
    val codecs: String? = null
)

@Singleton
class HlsParser @Inject constructor(
    private val http: OkHttpClient
) {
    companion object { const val MAX_VARIANTS = 100 } // no hard 8-variant cap

    /**
     * @throws HlsLiveException       no #EXT-X-ENDLIST and no PLAYLIST-TYPE:VOD
     * @throws HlsEncryptedException  any #EXT-X-KEY with METHOD != NONE
     */
    suspend fun parseManifest(manifestUrl: String): List<HlsVariant> =
        withContext(Dispatchers.IO) {
            val body = http.newCall(Request.Builder().url(manifestUrl).build())
                .execute().use { resp ->
                    check(resp.isSuccessful) { "Failed to fetch HLS manifest: ${resp.code}" }
                    resp.body.string()
                }

            val lines = body.lines()
            val base = URI(manifestUrl)

            // --- Live detection — CORRECT logic (bug rule #1) ---
            val hasEndList = lines.any { it.startsWith("#EXT-X-ENDLIST") }
            val isVod = lines.firstOrNull { it.startsWith("#EXT-X-PLAYLIST-TYPE") }
                ?.contains("VOD", ignoreCase = true) == true
            val isEncrypted = lines.any {
                it.startsWith("#EXT-X-KEY") && !it.contains("METHOD=NONE", true)
            }
            if (!hasEndList && !isVod) throw HlsLiveException()
            if (isEncrypted) throw HlsEncryptedException()

            // --- Variants ---
            val variants = mutableListOf<HlsVariant>()
            var i = 0
            while (i < lines.size && variants.size < MAX_VARIANTS) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    val next = lines.getOrNull(i + 1)?.trim()
                    if (!next.isNullOrEmpty() && !next.startsWith("#")) {
                        variants += HlsVariant(
                            url = resolveUrl(base, next),
                            resolution = Regex("""RESOLUTION=(\d+x\d+)""").find(line)
                                ?.groupValues?.get(1) ?: "unknown",
                            bandwidth = Regex("""BANDWIDTH=(\d+)""").find(line)
                                ?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            codecs = Regex("""CODECS="([^"]+)"""").find(line)?.groupValues?.get(1)
                        )
                    }
                }
                i++
            }
            // A bare media playlist (no master) is a single variant
            if (variants.isEmpty()) {
                check(lines.any { it.startsWith("#EXTM3U") }) { "Not a valid m3u8 playlist" }
                variants += HlsVariant(resolveUrl(base, manifestUrl), "unknown", 0, null)
            }
            variants.sortedByDescending { it.bandwidth }
        }

    private fun resolveUrl(base: URI, relative: String): String =
        runCatching { base.resolve(relative).toString() }.getOrDefault(relative)
}
