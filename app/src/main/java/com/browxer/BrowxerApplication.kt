package com.browxer

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.browxer.app.BuildConfig
import com.browxer.core.common.MediaEngines
import com.browxer.core.database.DownloadRecoveryWorker
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
        // Startup budget: NOTHING heavy on the main thread here. The engine
        // binaries (yt-dlp / FFmpeg / aria2c) extract well AFTER the first
        // frame — and MediaEngines.ensureInitialized is also called lazily
        // by every engine consumer, so an early download still works.
        Handler(Looper.getMainLooper()).postDelayed({
            thread(name = "browxer-engine-init", start = true) {
                MediaEngines.ensureInitialized(applicationContext)
            }
        }, ENGINE_INIT_DELAY_MS)
        // Reset interrupted downloads to QUEUED (re-enqueue happens in StartupRecovery)
        DownloadRecoveryWorker.schedule(this)
        // CVE-hardening posture: WebView debugging bridge off in release
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }

    companion object {
        /** The first frame is long gone by then — no startup-path contention. */
        private const val ENGINE_INIT_DELAY_MS = 1500L
    }
}
