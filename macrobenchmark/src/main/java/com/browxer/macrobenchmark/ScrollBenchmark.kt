package com.browxer.macrobenchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.metrics.FrameTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame budget: P99 <= 16 ms while scrolling the browser WebView
 * (design spec section 2.2 — 60 fps floor everywhere).
 *
 * Run on a real device with the app installed:
 *  ./gradlew :macrobenchmark:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollBrowserContent() = benchmarkRule.measureRepeated(
        packageName = "com.browxer.app.debug",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
    ) {
        startActivityAndWait()

        // The browser hosts a WebView (never nested in a LazyColumn);
        // fling it four times and measure frame pacing.
        val centerX = device.displayWidth / 2
        val startY = (device.displayHeight * 0.8).toInt()
        val endY = (device.displayHeight * 0.2).toInt()
        repeat(4) {
            device.swipe(centerX, startY, centerX, endY, 150)
            device.waitForIdle()
        }
    }
}
