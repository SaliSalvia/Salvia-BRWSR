package com.browxer.core.common

import android.content.Context
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl.YoutubeDL

/**
 * Lazy, thread-safe one-shot initializer for the native media engines
 * (yt-dlp / FFmpeg / aria2c). Application.onCreate must NOT extract these
 * binaries on the startup path — engine consumers call ensureInitialized
 * right before first use (always on a background thread).
 */
object MediaEngines {

    @Volatile
    private var initialized = false

    /**
     * Idempotent; safe to call concurrently and from any thread.
     * Returns false when extraction failed so callers surface a typed
     * error instead of crashing — the next call will retry.
     */
    fun ensureInitialized(context: Context): Boolean {
        if (initialized) return true
        synchronized(this) {
            if (initialized) return true
            val appContext = context.applicationContext
            val ok = runCatching {
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                Aria2c.getInstance().init(appContext)
            }.isSuccess
            initialized = ok
            return ok
        }
    }
}
