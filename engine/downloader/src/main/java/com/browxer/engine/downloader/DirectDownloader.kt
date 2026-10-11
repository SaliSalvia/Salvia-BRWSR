package com.browxer.engine.downloader

import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import com.yausername.aria2c.Aria2c
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectDownloader @Inject constructor() {
    /** aria2c: 16 connections, 1 MB chunks, resume via .aria2 control file, .part rename. */
    fun download(task: DownloadTask, onProgress: (Float) -> Unit): DownloadResult {
        val dir = File(task.workDir).apply { mkdirs() }
        val part = File(dir, task.fileName + ".part")
        val cmd = mutableListOf(
            Aria2c.getInstance().binFile.absolutePath,
            "-x", "16", "-s", "16", "-k", "1M", "-c",
            "--file-allocation=none", "--summary-interval=1",
            "-d", dir.absolutePath, "-o", part.name
        )
        task.userAgent?.let { cmd += listOf("--user-agent=${task.userAgent}") }
        task.cookies?.let { cmd += listOf("--header=Cookie: ${task.cookies}") }
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
