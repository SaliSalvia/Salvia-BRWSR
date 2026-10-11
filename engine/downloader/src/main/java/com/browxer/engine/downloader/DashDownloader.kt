package com.browxer.engine.downloader

import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DASH: best video rep + best audio rep downloaded via aria2c, merged by FFmpegMuxer.
 * DRM (DashDrmException) and unsupported manifests are rejected upstream in DashParser.
 * NOTE (honest stub): representation URLs come from DashParser (BaseURL-level);
 * full SegmentTemplate URL templating is the next incremental task.
 */
@Singleton
class DashDownloader @Inject constructor(
    private val direct: DirectDownloader,
    private val muxer: FFmpegMuxer
) {
    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): DownloadResult {
        require(!task.dashVideoUrl.isNullOrBlank()) {
            "DASH task missing video representation URL"
        }
        val dir = File(task.workDir).apply { mkdirs() }

        val videoTask = task.copy(url = task.dashVideoUrl!!,
            fileName = task.fileName + ".video.mp4")
        val video = direct.download(videoTask) { p -> onProgress(p / 2f) }.output

        val audio = task.dashAudioUrl?.let { audioUrl ->
            val audioTask = task.copy(url = audioUrl, fileName = task.fileName + ".audio.m4a")
            direct.download(audioTask) { p -> onProgress(0.5f + p / 2f) }.output
        }

        val final = File(dir, task.fileName)
        return if (audio != null) {
            muxer.merge(video, audio, final)
            video.delete(); audio.delete()
            DownloadResult(output = final)
        } else DownloadResult(output = video)
    }
}
