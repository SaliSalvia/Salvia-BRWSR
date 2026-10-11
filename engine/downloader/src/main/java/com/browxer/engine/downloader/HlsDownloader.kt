package com.browxer.engine.downloader

import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import com.browxer.engine.muxer.FFmpegMuxer
import com.browxer.engine.resolver.HlsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsDownloader @Inject constructor(
    private val http: OkHttpClient,
    private val parser: HlsParser,       // throws HlsLive/HlsEncrypted upstream
    private val muxer: FFmpegMuxer
) {
    companion object {
        private const val POOL = 4 // concurrent segment fetches (InShot-parity)
    }

    /**
     * Parallel segment download (pool of 4), resume-friendly (existing
     * segments skipped), then remux to MP4. AES-128 / live are already
     * rejected upstream with typed errors — never silently.
     */
    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): DownloadResult =
        withContext(Dispatchers.IO) {
            val base = URI(task.url)
            val playlistBody = fetch(task.url)
            val segments = playlistBody.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { base.resolve(it.trim()).toString() }
            check(segments.isNotEmpty()) { "Empty media playlist" }

            val segDir = File(task.workDir, ".segments-" + task.id).apply { mkdirs() }
            val done = AtomicInteger(0)

            coroutineScope {
                val gate = Semaphore(POOL)
                segments.mapIndexed { i, segUrl ->
                    async {
                        val segFile = File(segDir, "seg_%05d".format(i))
                        if (!segFile.exists()) {   // resume: skip downloaded segments
                            gate.withPermit { fetchTo(segUrl, segFile) }
                        }
                        val d = done.incrementAndGet()
                        onProgress(d.toFloat() / segments.size)
                    }
                }.awaitAll()
            }

            // Concatenate strictly in playlist order (parallel fetch lands OOO)
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
            check(resp.isSuccessful) { "Playlist fetch failed: " + resp.code }
            resp.body.string()
        }
    }

    private suspend fun fetchTo(url: String, out: File) = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            check(resp.isSuccessful) { "Segment fetch failed: " + resp.code }
            resp.body.byteStream().use { it.copyTo(out.outputStream()) }
        }
    }
}
