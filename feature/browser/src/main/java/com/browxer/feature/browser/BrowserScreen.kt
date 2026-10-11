package com.browxer.feature.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.browxer.feature.download_sheet.MediaQualitySelectionSheet

// Cyberpunk palette
private val NeonPurple = Color(0xFF8A2BE2)
private val HotPink = Color(0xFFFF1493)
private val DeepBlack = Color(0xFF0A0A0A)
private val IdleGray = Color(0x66555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val fabState by viewModel.fabState.collectAsState()
    val sheetState by viewModel.qualitySheetState.collectAsState()
    var urlText by remember { mutableStateOf("") }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val webView = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    Scaffold(
        containerColor = DeepBlack,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = NeonPurple)
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Search or type URL", color = Color.Gray) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0xFF222222)),
                    trailingIcon = {
                        IconButton(onClick = { webView.value?.reload() }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                    }
                )
                BadgedBox(badge = { Badge { Text("1") } }) {
                    Icon(Icons.Default.Tab, null, tint = Color.White)
                }
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { webView.value?.goBack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                IconButton(onClick = { webView.value?.loadUrl("about:blank") }) {
                    Icon(Icons.Default.Home, "Home", tint = Color.White) }
                IconButton(onClick = { /* open tabs grid */ }) {
                    Icon(Icons.Default.Tab, "Tabs", tint = Color.White) }
                IconButton(onClick = { /* open settings */ }) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
            }
        },
        floatingActionButton = {
            MagicFab(
                state = fabState,
                onIdleTap = {
                    snackbarMsg = "Scanning… no playable media detected yet."
                },
                onMediaTap = {
                    val wv = webView.value
                    viewModel.onFabClicked(
                        pageUrl = wv?.url ?: "",
                        cookies = CookieManager.getInstance().getCookie(wv?.url ?: ""),
                        userAgent = wv?.settings?.userAgentString
                    )
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    createBrowserWebView(ctx, viewModel).also { webView.value = it }
                },
                update = { /* settings already applied */ }
            )
        }
        sheetState?.let { state ->
            MediaQualitySelectionSheet(
                state = state,
                onDismiss = viewModel::dismissSheet,
                onDownload = { candidate, fileName ->
                    val wv = webView.value
                    viewModel.startDownload(
                        candidate, fileName,
                        cookies = CookieManager.getInstance().getCookie(wv?.url ?: ""),
                        userAgent = wv?.settings?.userAgentString
                    )
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createBrowserWebView(
    context: android.content.Context,
    viewModel: BrowserViewModel
): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            // document-start sniffer injection (Layer 2)
            view?.evaluateJavascript(SNIFFER_JS, null)
        }
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            url?.let { /* update urlText state via callback */ }
        }
    }
    webChromeClient = WebChromeClient()
    loadUrl("https://duckduckgo.com")
}

private const val SNIFFER_JS =
    "fetch('file:///android_asset/media_sniffer.js').then(r=>r.text()).then(eval);"
// NOTE: assets aren't fetchable from http pages; production wiring reads the asset
// on the Kotlin side (context.assets.open("media_sniffer.js")) and passes the
// script text to evaluateJavascript. Sniffer events flow: bridge -> ViewModel.

@Composable
private fun MagicFab(state: FabState, onIdleTap: () -> Unit, onMediaTap: () -> Unit) {
    val pulse = if (state is FabState.MediaDetected) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.85f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseScale").value
    } else 1f

    Box(
        Modifier
            .scale(pulse)
            .size(60.dp)
            .clip(CircleShape)
            .background(
                when (state) {
                    is FabState.Idle -> Brush.linearGradient(IdleGray, IdleGray)
                    is FabState.MediaDetected -> Brush.linearGradient(NeonPurple, HotPink)
                    is FabState.Downloading -> Brush.linearGradient(NeonPurple, HotPink)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is FabState.Idle -> IconButton(onClick = onIdleTap) {
                Icon(Icons.Default.Search, "Scan media", tint = Color.White) }
            is FabState.MediaDetected -> IconButton(onClick = onMediaTap) {
                Icon(Icons.Default.Download, "Download", tint = Color.White) }
            is FabState.Downloading -> CircularProgressIndicator(
                progress = { state.progress / 100f },
                color = Color.White, trackColor = Color(0x33FFFFFF),
                modifier = Modifier.padding(14.dp))
        }
    }
}
