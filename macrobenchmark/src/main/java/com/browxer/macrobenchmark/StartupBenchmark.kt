package com.browxer.macrobenchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.metrics.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup budget (design spec section 2.1):
 *  - cold start to first frame <= 1200 ms
 *  - warm start <= 400 ms
 *
 * Run on a real device with the app installed:
 *  ./gradlew :macrobenchmark:connectedDebugAndroidTest
 *
 * NOTE: the debug variant carries the ".debug" applicationId suffix and is
 * not representative (no R8, no baseline profile). For accurate numbers,
 * point [PACKAGE_NAME] at a release-style install ("com.browxer.app").
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun warmStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.WARM,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        // Debug: "com.browxer.app.debug" / release: "com.browxer.app".
        const val PACKAGE_NAME = "com.browxer.app.debug"
    }
}
