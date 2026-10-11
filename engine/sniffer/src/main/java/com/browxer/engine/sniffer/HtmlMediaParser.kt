package com.browxer.engine.sniffer

import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage-2 fallback (InShot-parity, done right): the rendered HTML of the
 * finished page is parsed with jsoup off the main thread. Catches media the
 * JS injection missed — blocked scripts, SSR-only markup, and malformed
 * JSON-LD edge cases. Raw string is NEVER evaluated; only structural parsing.
 */
@Singleton
class HtmlMediaParser @Inject constructor() {

    companion object {
        private const val MAX_HTML = 4 * 1024 * 1024 // hard cap, OOM guard
        private const val MAX_JSON_DEPTH = 8
    }

    /** [jsonHtml] is the evaluateJavascript callback value: a JSON-encoded string. */
    fun parseJsonEncoded(jsonHtml: String, baseUrl: String): List<MediaCandidate> {
        if (jsonHtml.isBlank() || jsonHtml == "null") return emptyList()
        val html = runCatching { JSONArray("[" + jsonHtml + "]").optString(0) }
            .getOrDefault("")
        return parse(html, baseUrl)
    }

    fun parse(html: String, baseUrl: String): List<MediaCandidate> {
        if (html.isBlank() || html.length > MAX_HTML) return emptyList()
        val doc = runCatching { Jsoup.parse(html, baseUrl) }.getOrNull()
            ?: return emptyList()
        val out = mutableListOf<MediaCandidate>()

        // <video> + nested <source>
        doc.select("video").forEach { video ->
            abs(video, "src")?.let {
                out.add(candidate(it, "video/mp4", SourceLayer.DOM_VIDEO, baseUrl)) }
            video.select("source").forEach { source ->
                abs(source, "src")?.let {
                    out.add(candidate(it, source.attr("type").ifBlank { "video" },
                        SourceLayer.DOM_VIDEO, baseUrl)) }
            }
        }

        // <audio>
        doc.select("audio").forEach { audio ->
            abs(audio, "src")?.let {
                out.add(candidate(it, "audio/mpeg", SourceLayer.DOM_AUDIO, baseUrl)) }
        }

        // OG / Twitter cards
        doc.select(
            "meta[property=og:video], meta[property=og:video:secure_url], " +
            "meta[name=twitter:player:stream]"
        ).forEach { meta ->
            abs(meta, "content")?.let {
                out.add(candidate(it, "video", SourceLayer.OG_META, baseUrl)) }
        }
        doc.select("meta[property=og:image]").forEach { meta ->
            abs(meta, "content")?.let {
                out.add(candidate(it, "image", SourceLayer.DOM_IMAGE, baseUrl)) }
        }

        // JSON-LD — recursive contentUrl/embedUrl walk (superset of InShot's VideoObject check)
        doc.select("script[type=application/ld+json]").forEach { script ->
            runCatching {
                val txt = script.data().trim()
                val node: Any = try { JSONObject(txt) } catch (e: Exception) { JSONArray(txt) }
                extractContentUrls(node, 0).forEach { url ->
                    out.add(candidate(url, "video", SourceLayer.JSON_LD, baseUrl)) }
            }
        }
        return out
    }

    private fun abs(el: Element, attr: String): String? {
        val v = el.absUrl(attr).ifBlank { el.attr(attr) }
        if (!v.startsWith("http://") && !v.startsWith("https://")) return null
        return v.takeIf { it.isNotBlank() }
    }

    private fun candidate(url: String, type: String, layer: SourceLayer, baseUrl: String) =
        MediaCandidate(url = url, mimeType = type, source = layer, pageUrl = baseUrl)

    /** Depth-capped recursive search for contentUrl / embedUrl. */
    private fun extractContentUrls(node: Any?, depth: Int): List<String> {
        if (depth > MAX_JSON_DEPTH) return emptyList()
        return when (node) {
            is JSONObject -> {
                val urls = mutableListOf<String>()
                if (node.has("contentUrl"))
                    node.optString("contentUrl").takeIf { it.startsWith("http") }
                        ?.let { urls.add(it) }
                if (node.has("embedUrl"))
                    node.optString("embedUrl").takeIf { it.startsWith("http") }
                        ?.let { urls.add(it) }
                node.keys().forEach { k ->
                    urls.addAll(extractContentUrls(node.opt(k), depth + 1)) }
                urls
            }
            is JSONArray ->
                (0 until node.length()).flatMap { extractContentUrls(node.opt(it), depth + 1) }
            else -> emptyList()
        }
    }
}
