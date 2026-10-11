package com.browxer.engine.resolver

import com.browxer.core.domain.DownloadTask
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import com.yausername.youtubedl.YoutubeDL
import com.yausername.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * yt-dlp wrapper. youtubedl-android 0.15.x is the Kotlin rewrite — this class
 * deliberately couples to the SMALLEST stable API surface only:
 *   - YoutubeDLRequest(String)            (README contract)
 *   - addOption(String, String?)           (README contract)
 *   - execute(request, processId, callback (Float, Long, String) -> Unit)  (0.15.x)
 *   - YoutubeDLResponse.out                (README contract)
 * No VideoInfo/getInfo coupling: extraction uses -J dump-json + org.json,
 * so a VideoInfo field rename can never break the build.
 */
@Singleton
class YtdlpResolver @Inject constructor() {

    /** Extraction-only (quality sheet): dump single JSON, parse defensively. */
    suspend fun extractCandidates(pageUrl: String, cookies: String?): List<MediaCandidate> =
        withContext(Dispatchers.IO) {
            val req = YoutubeDLRequest(pageUrl).apply {
                addOption("--no-playlist")
                addOption("-J") // dump single-video JSON, no download
                addOption("--socket-timeout", "15")
                cookies?.let { addOption("--add-headers", "Cookie:" + it) }
            }
            val out = YoutubeDL.getInstance().execute(req, null as String?).out
            val jsonLine = out.lineSequence().firstOrNull { it.trim().startsWith("{") }
                ?: error("yt-dlp returned no JSON for " + pageUrl)
            val info = JSONObject(jsonLine)

            val width = info.optInt("width", -1)
            val height = info.optInt("height", -1)
            listOf(
                MediaCandidate(
                    url = pageUrl,
                    mimeType = "video",
                    sizeBytes = info.optLong("filesize",
                        info.optLong("filesize_approx", -1L)),
                    durationSeconds = info.optDouble("duration", -1.0),
                    posterUrl = info.optString("thumbnail").takeIf { it.isNotBlank() },
                    pageUrl = pageUrl,
                    resolution = if (width > 0 && height > 0)
                        width.toString() + "x" + height.toString() else "unknown",
                    source = SourceLayer.YTDLP
                )
            )
        }

    /** Full download: yt-dlp + aria2c (16 connections) + FFmpeg mux to MP4. */
    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val req = YoutubeDLRequest(task.url).apply {
                addOption("-f", "bestvideo+bestaudio/best")
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", "aria2c:-x 16 -s 16 -k 1M")
                addOption("--merge-output-format", "mp4")
                addOption("--no-playlist")
                addOption("-o", File(task.workDir, "%(title).80s.%(ext)s").absolutePath)
                task.cookies?.let { addOption("--add-headers", "Cookie:" + it) }
                task.userAgent?.let { addOption("--add-headers", "User-Agent:" + it) }
            }
            // 0.15.x: execute(request, processId, (progress, eta, line) -> Unit)
            YoutubeDL.getInstance().execute(req, null as String?) {
                    prog: Float, _: Long, _: String ->
                onProgress(prog / 100f)
            }
            File(task.workDir) // yt-dlp writes here; pick newest finished file
                .walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }
                .maxByOrNull { it.lastModified() }
                ?: error("yt-dlp finished but produced no output file")
        }
}
