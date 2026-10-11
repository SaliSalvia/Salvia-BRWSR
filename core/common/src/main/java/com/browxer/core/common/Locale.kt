package com.browxer.core.common

import android.content.Context
import android.text.format.Formatter
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * Locale-aware formatters. ALL user-visible numbers, sizes, percentages
 * and dates in Browxer must go through these helpers so Persian users get
 * native digits automatically — never hardcode "%.1f MB" or raw 1234.
 */
object BrowxerLocale {

    /**
     * Locale-aware file size (Formatter.formatShortFileSize).
     * Returns an empty string for unknown/invalid sizes.
     */
    fun formatFileSize(context: Context, bytes: Long): String =
        if (bytes > 0) Formatter.formatShortFileSize(context, bytes) else ""

    /** Locale-aware count with native digits: 3 / ۳. */
    fun formatCount(value: Int): String =
        NumberFormat.getIntegerInstance().format(value)

    /** Locale-aware percentage: 45% / ۴۵%. */
    fun formatPercent(value: Int): String =
        formatCount(value.coerceIn(0, 100)) + "%"

    /** Locale-aware medium date for download timestamps. */
    fun formatDate(epochMillis: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(Date(epochMillis))
}
