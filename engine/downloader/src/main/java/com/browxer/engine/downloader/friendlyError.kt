package com.browxer.engine.downloader

import com.browxer.engine.resolver.DashDrmException
import com.browxer.engine.resolver.DashUnsupportedException
import com.browxer.engine.resolver.HlsEncryptedException
import com.browxer.engine.resolver.HlsLiveException

/** Typed error mapping — never string-guessing on generic Exceptions. */
fun friendlyError(t: Throwable): String = when (t) {
    is HlsLiveException -> "Live streams are not supported."
    is HlsEncryptedException -> "Encrypted HLS (AES-128) is not supported."
    is DashDrmException -> "This media is DRM-protected and cannot be downloaded."
    is DashUnsupportedException -> "DASH format is not supported for this media."
    is java.io.FileNotFoundException -> "File not found on server."
    is java.io.IOException -> "Network error. Please check your connection."
    is IllegalArgumentException -> t.message ?: "Invalid request."
    else -> when {
        t.message?.contains("ENOSPC") == true -> "Not enough storage space."
        else -> "Download failed: ${t.message ?: "Unknown error"}"
    }
}
