package com.browxer.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a REAL baseline profile on a connected device/emulator:
 *
 *   ./gradlew :macrobenchmark:connectedDebugAndroidTest
 *
 * Output lands in
 *   macrobenchmark/build/outputs/connected_android_test_additional_output/
 * Copy the BaselineProfileGenerator*.txt to app/src/main/baseline-prof.txt,
 * re-add androidx.profileinstaller to :app and commit (see README-BUILD.md).
 *
 * Prefer a release-style (non-debuggable) build for a realistic profile;
 * on an emulator add
 *   -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
 * and treat the numbers as non-representative.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.browxer.app.debug",
    ) {
        pressHome()
        startActivityAndWait()

        // Exercise the main flows so the profile covers the browser screen.
        val centerX = device.displayWidth / 2
        val startY = (device.displayHeight * 0.8).toInt()
        val endY = (device.displayHeight * 0.2).toInt()
        repeat(2) {
            device.swipe(centerX, startY, centerX, endY, 150)
            device.waitForIdle()
        }
    }
}
