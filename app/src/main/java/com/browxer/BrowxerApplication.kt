package com.browxer

import android.app.Application
import android.webkit.WebView
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.browxer.core.database.DownloadRecoveryWorker
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltAndroidApp
class BrowxerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Never init media engines on the main thread
        thread(name = "browxer-engine-init", start = true) {
            runCatching {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                Aria2c.getInstance().init(this)
            } // failures surfaced in Settings -> Diagnostics
        }
        // Recover interrupted downloads from the previous session
        DownloadRecoveryWorker.schedule(this)
        // CVE-hardening posture: WebView debugging bridge off in release
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }
}
