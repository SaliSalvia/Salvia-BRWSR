package com.browxer.engine.resolver

import android.util.Base64
import com.browxer.core.domain.MediaCandidate
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reassembles chunked blob payloads (512 KB chunks, 50 MB cap enforced in JS)
 * into a temp file, handed off to the downloader.
 */
@Singleton
class BlobResolver @Inject constructor() {

    private class Assembly(val chunks: MutableMap<Int, String> = ConcurrentHashMap()) {
        fun receivedBytes(): Long =
            chunks.values.sumOf { it.length * 3L / 4L }
    }

    private val assemblies = ConcurrentHashMap<String, Assembly>()
    private val sizes = ConcurrentHashMap<String, Long>()

    /** Called for each onBlobChunk bridge event. */
    fun onChunk(blobId: String, chunkIndex: Int, declaredSize: Long, base64: String) {
        sizes.putIfAbsent(blobId, declaredSize)
        assemblies.getOrPut(blobId) { Assembly() }
            .chunks.putIfAbsent(chunkIndex, base64)
    }

    fun isComplete(blobId: String): Boolean {
        val asm = assemblies[blobId] ?: return false
        val declared = sizes[blobId] ?: return false
        return declared > 0 && asm.receivedBytes() >= declared
    }

    /** Writes the assembled blob to a temp file under app-scoped cache. */
    fun assembleTo(blobId: String, output: File): Boolean {
        val asm = assemblies.remove(blobId) ?: return false
        sizes.remove(blobId)
        output.parentFile?.mkdirs()
        output.outputStream().use { out ->
            asm.chunks.toSortedMap().values.forEach { b64 ->
                val bytes = runCatching { Base64.decode(b64, Base64.DEFAULT) }
                    .getOrNull() ?: return false
                out.write(bytes)
            }
        }
        return true
    }
}
