package com.browxer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.browxer.core.common.JankReporter
import com.browxer.core.ui.BrowxerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var startupRecovery: StartupRecovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Dev-only frame watchdog — Logcat tag "BrowxerJank", budget 16.6ms.
        if (BuildConfig.DEBUG) JankReporter(window).start()
        // Resume support: re-enqueue downloads killed by process death
        lifecycleScope.launch { startupRecovery.requeueInterrupted() }
        setContent {
            BrowxerTheme {
                BrowxerNavHost()
            }
        }
    }
}
