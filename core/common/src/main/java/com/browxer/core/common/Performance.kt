package com.browxer.core.common

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight frame-timing watchdog for dev builds. Uses the platform
 * Window.OnFrameMetricsAvailableListener (API 24+) — zero external
 * benchmark dependencies in the production classpath.
 *
 * Wire it in debug builds only:
 *   if (BuildConfig.DEBUG) JankReporter(window).start()
 *
 * Watch Logcat tag "BrowxerJank" for a 5-second frame/jank summary.
 */
class JankReporter(private val window: Window) {

    private val running = AtomicBoolean(false)
    private val frames = AtomicLong()
    private val jankFrames = AtomicLong()
    private val handler = Handler(Looper.getMainLooper())
    private var lastSummaryAt = 0L

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics ->
        onFrameMetrics(metrics)
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        lastSummaryAt = System.nanoTime()
        runCatching { window.addOnFrameMetricsAvailableListener(listener, handler) }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
    }

    val frameCount: Long get() = frames.get()
    val jankCount: Long get() = jankFrames.get()
    val jankRatePercent: Double
        get() {
            val f = frames.get()
            return if (f == 0L) 0.0 else jankFrames.get() * 100.0 / f
        }

    private fun onFrameMetrics(metrics: FrameMetrics) {
        // FrameMetrics durations are in nanoseconds.
        val totalMs = metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000.0
        frames.incrementAndGet()
        if (totalMs > JANK_THRESHOLD_MS) jankFrames.incrementAndGet()

        val now = System.nanoTime()
        if (now - lastSummaryAt >= 5_000_000_000L) { // every ~5s of frames
            lastSummaryAt = now
            Log.d(
                TAG,
                "frames=" + frames.get() +
                    " jank=" + jankFrames.get() +
                    " rate=" + String.format("%.1f", jankRatePercent) + "%"
            )
        }
    }

    companion object {
        private const val TAG = "BrowxerJank"

        /** 16.6ms — one dropped frame at 60Hz. */
        const val JANK_THRESHOLD_MS = 16.6
    }
}
