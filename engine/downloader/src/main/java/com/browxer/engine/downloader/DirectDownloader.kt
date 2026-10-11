package com.browxer.engine.downloader

import android.content.Context
import com.browxer.core.common.MediaEngines
import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import com.yausername.aria2c.Aria2c
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectDownloader @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    /**
     * aria2c tuned for speed AND sequential preview:
     * 16 connections / 16 splits / 1 MB min split, native resume (-c),
     * fast allocation (falloc), 64 MB RAM disk cache, inorder piece
     * selection so playback can start while the download runs.
     */
    fun download(task: DownloadTask, onProgress: (Float) -> Unit): DownloadResult {
        MediaEngines.ensureInitialized(appContext)
        val dir = File(task.workDir).apply { mkdirs() }
        val part = File(dir, task.fileName + ".part")
        val cmd = mutableListOf(
            Aria2c.getInstance().binFile.absolutePath,
            "-x", "16",
            "-s", "16",
            "-k", "1M",
            "-c",
            "--max-connection-per-server=16",
            "--min-split-size=1M",
            "--file-allocation=falloc",
            "--disk-cache=64M",
            "--optimize-concurrent-downloads=true",
            "--stream-piece-selector=inorder",
            "--check-certificate=true",
            "--summary-interval=1",
            "-d", dir.absolutePath,
            "-o", part.name
        )
        task.userAgent?.let { cmd += listOf("--user-agent=" + it) }
        task.cookies?.let { cmd += listOf("--header=Cookie: " + it) }
        task.pageUrl?.let { cmd += listOf("--referer=" + it) }
        cmd += task.url

        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().forEachLine { line ->
            Regex("""\((\d+)%\)""").find(line)?.groupValues?.get(1)
                ?.toIntOrNull()?.let { onProgress(it / 100f) }
        }
        check(proc.waitFor() == 0) { "aria2c exited with error" }
        val final = File(dir, task.fileName)
        check(part.renameTo(final)) { "Could not finalize .part file" }
        return DownloadResult(output = final)
    }
}
