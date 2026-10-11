package com.browxer.engine.muxer

import com.yausername.ffmpeg.FFmpeg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FFmpeg wrapper. youtubedl-android 0.15.x (Kotlin rewrite) takes a
 * List<String> of RAW args — no shell, no quoting, one token per element.
 * Success is verified by checking the output file, NOT by coupling to
 * FFmpegResponse fields (a response rename can then never break the build).
 */
@Singleton
class FFmpegMuxer @Inject constructor() {

    /** Merge separate A/V streams, stream-copy, web-optimized MP4 (+faststart). */
    fun merge(video: File, audio: File, output: File) {
        execute(
            listOf(
                "-i", video.absolutePath,
                "-i", audio.absolutePath,
                "-c", "copy", "-movflags", "+faststart",
                output.absolutePath
            ),
            output
        )
    }

    /** Remux a transport stream (HLS concat) into MP4. */
    fun remuxToMp4(input: File, output: File) {
        execute(
            listOf(
                "-i", input.absolutePath,
                "-c", "copy", "-movflags", "+faststart",
                output.absolutePath
            ),
            output
        )
    }

    /** Optional: extract audio track to M4A. */
    fun extractAudio(input: File, output: File) {
        execute(listOf("-i", input.absolutePath, "-vn", "-c", "copy", output.absolutePath), output)
    }

    private fun execute(args: List<String>, output: File) {
        output.parentFile?.mkdirs()
        runCatching { FFmpeg.getInstance().execute(args) }
            .getOrElse { throw IllegalStateException("FFmpeg invocation failed: " + it.message) }
        check(output.exists() && output.length() > 0) {
            "FFmpeg produced no output (args: " + args.joinToString(" ") + ")"
        }
    }
}
