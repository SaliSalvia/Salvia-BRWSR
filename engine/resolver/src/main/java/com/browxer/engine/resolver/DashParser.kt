package com.browxer.engine.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.END_DOCUMENT
import org.xmlpull.v1.XmlPullParser.START_TAG
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

class DashDrmException : Exception("DASH manifest uses Widevine/PlayReady DRM")
class DashUnsupportedException : Exception("DASH manifest has no downloadable representation")

data class DashRepresentation(
    val id: String,
    val url: String,
    val mimeType: String,
    val bandwidth: Long,
    val width: Int,
    val height: Int,
    val codecs: String?,
    val isAudio: Boolean
) {
    val label: String
        get() = if (isAudio) "audio · ${bandwidth / 1000} kbps"
                else "${height}p · ${width}x${height} · ${bandwidth / 1000} kbps"
}

@Singleton
class DashParser @Inject constructor(
    private val http: OkHttpClient
) {
    suspend fun parseManifest(mpdUrl: String): List<DashRepresentation> =
        withContext(Dispatchers.IO) {
            val xml = http.newCall(Request.Builder().url(mpdUrl).build())
                .execute().use { resp ->
                    check(resp.isSuccessful) { "Failed to fetch DASH manifest: ${resp.code}" }
                    resp.body.string()
                }

            val parser = androidx.core.util.Xml.newPullParser().apply { setInput(StringReader(xml)) }
            val reps = mutableListOf<DashRepresentation>()
            var baseUrl = mpdUrl.substringBeforeLast('/') + "/"
            var drmFound = false
            var currentMime = ""

            var event = parser.eventType
            while (event != END_DOCUMENT) {
                if (event == START_TAG) when (parser.name) {
                    "BaseURL" -> baseUrl = parser.nextText().trim().let {
                        if (it.startsWith("http")) it else baseUrl + it
                    }
                    "ContentProtection" -> {
                        val scheme = parser.getAttributeValue(null, "schemeIdUri") ?: ""
                        if (scheme.contains("widevine", true) ||
                            scheme.contains("playready", true)) drmFound = true
                    }
                    "AdaptationSet" ->
                        currentMime = parser.getAttributeValue(null, "mimeType") ?: ""
                    "Representation" -> {
                        val mime = parser.getAttributeValue(null, "mimeType") ?: currentMime
                        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                            reps += DashRepresentation(
                                id = parser.getAttributeValue(null, "id") ?: "",
                                url = baseUrl, // segment URLs resolved by DashDownloader via SegmentTemplate
                                mimeType = mime,
                                bandwidth = parser.getAttributeValue(null, "bandwidth")
                                    ?.toLongOrNull() ?: 0L,
                                width = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: -1,
                                height = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: -1,
                                codecs = parser.getAttributeValue(null, "codecs"),
                                isAudio = mime.startsWith("audio/")
                            )
                        }
                    }
                }
                event = parser.next()
            }
            if (drmFound) throw DashDrmException()
            if (reps.none { !it.isAudio }) throw DashUnsupportedException()
            reps.sortedByDescending { it.bandwidth }
        }
}
