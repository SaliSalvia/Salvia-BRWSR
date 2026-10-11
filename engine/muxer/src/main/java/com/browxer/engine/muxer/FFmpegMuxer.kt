package com.browxer.engine.muxer

import com.yausername.ffmpeg.FFmpeg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FFmpegMuxer @Inject constructor() {
    /** Merge separate A/V streams, stream-copy, web-optimized MP4 (+faststart). */
    fun merge(video: File, audio: File, output: File) {
        execute("-i ${q(video)} -i ${q(audio)} -c copy -movflags +faststart ${q(output)}")
    }

    /** Remux a transport stream (HLS concat) into MP4. */
    fun remuxToMp4(input: File, output: File) {
        execute("-i ${q(input)} -c copy -movflags +faststart ${q(output)}")
    }

    /** Optional: extract audio track to M4A. */
    fun extractAudio(input: File, output: File) {
        execute("-i ${q(input)} -vn -c copy ${q(output)}")
    }

    private fun q(f: File) = "\""${f.absolutePath}\""

    private fun execute(cmd: String) {
        val code = runCatching { FFmpeg.getInstance().execute(cmd) }.getOrNull()
        check(code == 0) { "FFmpeg failed (exit=$code): $cmd" }
    }
}
