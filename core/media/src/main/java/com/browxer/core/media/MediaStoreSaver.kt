package com.browxer.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped-storage compliant saver: final files go to MediaStore (Movies/Browxer). */
@Singleton
class MediaStoreSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun save(file: File, subDir: String): Uri {
        val mime = when (file.extension.lowercase()) {
            "mp3", "m4a", "aac", "opus", "flac", "ogg" -> "audio/" + file.extension
            "jpg", "jpeg", "png", "webp" -> "image/" + file.extension
            else -> "video/mp4"
        }
        val collection =
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, subDir) // e.g. Movies/Browxer
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: error("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: error("Could not open MediaStore output stream")
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
