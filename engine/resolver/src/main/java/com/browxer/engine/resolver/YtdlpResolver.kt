package com.browxer.engine.resolver

import com.browxer.core.domain.DownloadTask
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import com.yausername.youtubedl.YoutubeDL
import com.yausername.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtdlpResolver @Inject constructor() {

    /** Extraction-only (for the quality sheet). Returns best format as a candidate. */
    suspend fun extractCandidates(pageUrl: String, cookies: String?): List<MediaCandidate> =
        withContext(Dispatchers.IO) {
            val req = YoutubeDLRequest(listOf(pageUrl)).apply {
                addOption("--no-playlist")
                addOption("--socket-timeout", "15")
                cookies?.let { addOption("--add-headers", "Cookie:$it") }
            }
            val info = YoutubeDL.getInstance().getInfo(req)
            listOf(
                MediaCandidate(
                    url = pageUrl,
                    mimeType = "video",
                    sizeBytes = info?.downloadSize ?: -1L,
                    durationSeconds = (info?.duration ?: -1.0).toDouble(),
                    posterUrl = info?.thumbnail,
                    pageUrl = pageUrl,
                    resolution = "${info?.width ?: -1}x${info?.height ?: -1}",
                    source = SourceLayer.YTDLP
                )
            )
        }

    /** Full download: yt-dlp + aria2c (16 connections) + FFmpeg mux to MP4. */
    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val req = YoutubeDLRequest(listOf(task.url)).apply {
                addOption("-f", "bestvideo+bestaudio/best")
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", "aria2c:-x 16 -k 1M")
                addOption("--merge-output-format", "mp4")
                addOption("--no-playlist")
                addOption("-o", File(task.workDir, "%(title).80s.%(ext)s").absolutePath)
                task.cookies?.let { addOption("--add-headers", "Cookie:$it") }
                task.userAgent?.let { addOption("--add-headers", "User-Agent:$it") }
            }
            YoutubeDL.getInstance().execute(req) { _, prog, _ ->
                onProgress(prog.toFloat() / 100f)
            }
            File(task.workDir) // yt-dlp writes here; pick newest finished file
                .walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }
                .maxByOrNull { it.lastModified() }
                ?: error("yt-dlp finished but produced no output file")
        }
}
