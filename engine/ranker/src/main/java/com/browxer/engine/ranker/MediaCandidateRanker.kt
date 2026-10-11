package com.browxer.engine.ranker

import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredCandidate(val candidate: MediaCandidate, val score: Int, val groupKey: String)

/**
 * Scoring table (production):
 *  OG/Twitter/JSON-LD 100 | YTDLP 95 | HLS variant 90 | DASH rep 85 |
 *  DOM video/audio 70 | DOM image >=800px 50 | network XHR 40 | blob 35
 * Penalties: ad domain -80 | ad keywords -60 | <720p (if higher exists) -30 |
 *  duration<3s -40 | watermark/logo filename -25
 * Candidates of the same logical media group into ONE quality dropdown entry.
 */
@Singleton
class MediaCandidateRanker @Inject constructor(
    private val admission: MediaSniffAdmission
) {
    private val adKeywords = Regex("""/ad(s)?/|/banner/|/promo/""", RegexOption.IGNORE_CASE)
    private val watermark = Regex("""(watermark|logo)""", RegexOption.IGNORE_CASE)

    fun rank(all: List<MediaCandidate>): List<ScoredCandidate> {
        val admitted = all.filter(admission::admit)
        val scored = admitted.map { it to score(it) }
        val hasHighRes = scored.any { (c, _) -> c.height >= 720 }

        val adjusted = scored.map { (c, s) ->
            var sc = s
            if (hasHighRes && c.height in 200..719) sc -= 30
            c to sc
        }.filter { it.second > 0 }

        return adjusted
            .groupBy { (c, _) -> groupKey(c) }
            .map { (_, group) -> group.maxBy { it.second } }
            .map { (c, s) -> ScoredCandidate(c, s, groupKey(c)) }
            .sortedByDescending { it.score }
    }

    /** Top candidate below 60 -> trigger the yt-dlp fallback. */
    fun needsYtdlpFallback(ranked: List<ScoredCandidate>): Boolean =
        (ranked.firstOrNull()?.score ?: 0) < 60

    private fun score(c: MediaCandidate): Int {
        var s = when (c.source) {
            SourceLayer.OG_META, SourceLayer.JSON_LD -> 100
            SourceLayer.YTDLP -> 95
            SourceLayer.HLS_VARIANT -> 90
            SourceLayer.DASH_REP -> 85
            SourceLayer.DOM_VIDEO, SourceLayer.DOM_AUDIO -> 70
            SourceLayer.DOM_IMAGE -> 50
            SourceLayer.NETWORK_XHR -> 40
            SourceLayer.BLOB -> 35
        }
        if (Regex("""doubleclick|googlesyndication|adservice|taboola|outbrain""",
                RegexOption.IGNORE_CASE).containsMatchIn(c.url)) s -= 80
        if (adKeywords.containsMatchIn(c.url)) s -= 60
        if (c.durationSeconds in 0.0..3.0) s -= 40
        if (watermark.containsMatchIn(c.url)) s -= 25
        if (c.sizeBytes > 5_000_000) s += 5
        if (c.durationSeconds > 30) s += 5
        if (!c.posterUrl.isNullOrBlank() && c.source == SourceLayer.OG_META) s += 5
        return s
    }

    private fun groupKey(c: MediaCandidate): String {
        val norm = admission.normalize(c.url)
        // collapse master-playlist variants into one logical media
        val collapsed = norm.replace(Regex("""/(index|master|playlist).*\.m3u8.*$"""), "/_m")
        return collapsed.take(96)
    }
}
