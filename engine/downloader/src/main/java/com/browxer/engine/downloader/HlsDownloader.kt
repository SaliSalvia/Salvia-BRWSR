package com.browxer.engine.downloader

import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import com.browxer.engine.muxer.FFmpegMuxer
import com.browxer.engine.resolver.HlsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsDownloader @Inject constructor(
    private val http: OkHttpClient,
    private val parser: HlsParser,       // throws HlsLive/HlsEncrypted upstream
    private val muxer: FFmpegMuxer
) {
    /**
     * Segment-wise download with resume (existing segments skipped), then remux to MP4.
     * NOTE (honest stub): currently sequential; a concurrency pool is the next task.
     */
    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): DownloadResult =
        withContext(Dispatchers.IO) {
            val base = URI(task.url)
            val playlistBody = fetch(task.url)
            val segments = playlistBody.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { base.resolve(it.trim()).toString() }
            check(segments.isNotEmpty()) { "Empty media playlist" }

            val segDir = File(task.workDir, ".segments-${task.id}").apply { mkdirs() }
            var done = 0
            segments.forEachIndexed { i, segUrl ->
                val segFile = File(segDir, "seg_%05d".format(i))
                if (!segFile.exists()) {   // resume: skip already-downloaded segments
                    fetchTo(segUrl, segFile)
                }
                done++
                onProgress(done.toFloat() / segments.size)
            }

            // Concatenate + remux (faststart). AES-128 already rejected upstream.
            val rawTs = File(task.workDir, task.fileName + ".ts")
            rawTs.outputStream().use { out ->
                segments.indices.forEach { i ->
                    File(segDir, "seg_%05d".format(i)).inputStream().use { it.copyTo(out) }
                }
            }
            val mp4 = File(task.workDir, task.fileName)
            muxer.remuxToMp4(rawTs, mp4)
            segDir.deleteRecursively(); rawTs.delete()
            DownloadResult(output = mp4)
        }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            check(resp.isSuccessful) { "Playlist fetch failed: ${resp.code}" }
            resp.body.string()
        }
    }

    private suspend fun fetchTo(url: String, out: File) = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            check(resp.isSuccessful) { "Segment fetch failed: ${resp.code}" }
            resp.body.byteStream().use { it.copyTo(out.outputStream()) }
        }
    }
}
