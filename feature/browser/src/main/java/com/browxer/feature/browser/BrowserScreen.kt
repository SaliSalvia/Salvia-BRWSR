package com.browxer.feature.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import com.browxer.engine.sniffer.MediaSnifferBridge
import com.browxer.feature.download_sheet.MediaQualitySelectionSheet
import java.net.URLEncoder

// Cyberpunk palette
private val NeonPurple = Color(0xFF8A2BE2)
private val HotPink = Color(0xFFFF1493)
private val DeepBlack = Color(0xFF0A0A0A)
private val IdleGray = Color(0x66555555)

private const val HOME_URL = "https://duckduckgo.com"

// Stage-3 network sniffing (InShot's filter set)
private val NETWORK_MEDIA_EXT = listOf(
    ".mp4", ".m4v", ".webm", ".mkv", ".mov", ".avi", ".wmv",
    ".m3u8", ".mpd", ".mp3", ".m4a", ".aac", ".ogg", ".opus", ".flac"
)
// Segments / partial-range requests are never offered as download candidates
private val SEGMENT_OR_PARTIAL = Regex(
    """\.(ts|m4s|cmfv|cmfa)(\?|$)|/seg-|/segment|/chunk|/frag-|range=""",
    RegexOption.IGNORE_CASE)
private val mainHandler = Handler(Looper.getMainLooper())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val fabState by viewModel.fabState.collectAsState()
    val sheetState by viewModel.qualitySheetState.collectAsState()
    var urlText by remember { mutableStateOf(HOME_URL) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val webView = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    fun navigateTo(input: String) {
        val query = input.trim()
        if (query.isEmpty()) return
        val target = when {
            query.startsWith("http://") || query.startsWith("https://") -> query
            query.contains(' ') || !query.contains('.') ->
                "https://duckduckgo.com/?q=" + URLEncoder.encode(query, "UTF-8")
            else -> "https://" + query
        }
        webView.value?.loadUrl(target)
    }

    Box(Modifier.fillMaxSize().background(DeepBlack)) {
        Scaffold(
            containerColor = DeepBlack,
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = NeonPurple)
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        singleLine = true,
                        placeholder = { Text("Search or type URL", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateTo(urlText) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0xFF222222)),
                        trailingIcon = {
                            IconButton(onClick = { webView.value?.reload() }) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White)
                            }
                        }
                    )
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Default.VideoLibrary, "Downloads", tint = HotPink)
                    }
                    BadgedBox(badge = { Badge { Text("1") } }) {
                        Icon(Icons.Default.Tab, "Tabs", tint = Color.White)
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
                    IconButton(onClick = { webView.value?.loadUrl(HOME_URL) }) {
                        Icon(Icons.Default.Home, "Home", tint = Color.White) }
                    IconButton(onClick = {
                        snackbarMsg = "Tab management is not available in this build."
                    }) {
                        Icon(Icons.Default.Tab, "Tabs", tint = Color.White) }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
                }
            },
            floatingActionButton = {
                MagicFab(
                    state = fabState,
                    onIdleTap = {
                        snackbarMsg = "Scanning... no playable media detected yet."
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
                        createBrowserWebView(
                            context = ctx,
                            viewModel = viewModel,
                            bridge = viewModel.snifferBridge,
                            onUrlChanged = { urlText = it }
                        ).also { webView.value = it }
                    },
                    update = { /* settings applied once at creation */ }
                )
            }
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
    viewModel: BrowserViewModel,
    bridge: MediaSnifferBridge,
    onUrlChanged: (String) -> Unit
): WebView {
    // Layer 2+3 script ships as an asset (merged into the APK from :engine:sniffer)
    val snifferJs = context.assets.open("media_sniffer.js")
        .bufferedReader().use { it.readText() }

    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        // Hardened bridge (CVE-2024-46961 posture): the JS side calls window.Browxer.*
        addJavascriptInterface(bridge, "Browxer")

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                // document-start injection of the DOM/blob sniffer (Layer 2 + 3)
                view?.evaluateJavascript(snifferJs, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Stage-2 fallback (InShot-parity): capture the rendered HTML
                // and hand it to the jsoup parser — runs off the main thread.
                view?.evaluateJavascript(
                    "document.documentElement.outerHTML"
                ) { result ->
                    viewModel.onHtmlAvailable(
                        jsonEncodedHtml = result ?: "",
                        baseUrl = url ?: view.url ?: ""
                    )
                }
            }

            // Stage-3 (InShot-parity): native media-player requests (<video>) never
            // go through page fetch/XHR — intercept them here, on the WebView's
            // background thread, and hop to main for the ViewModel buffer.
            override fun shouldInterceptRequest(
                view: WebView?, request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()
                    ?: return super.shouldInterceptRequest(view, request)
                if (NETWORK_MEDIA_EXT.any { url.contains(it, ignoreCase = true) } &&
                    !SEGMENT_OR_PARTIAL.containsMatchIn(url)
                ) {
                    val pageUrl = view?.url ?: url
                    mainHandler.post {
                        viewModel.onMediaDetected(
                            MediaCandidate(
                                url = url, mimeType = "video",
                                source = SourceLayer.NETWORK_XHR, pageUrl = pageUrl))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            // Manifests surface even when a page blocks script injection entirely.
            override fun onLoadResource(view: WebView?, url: String?) {
                if (url != null && (url.contains(".m3u8", true) ||
                        url.contains(".mpd", true)) &&
                    !SEGMENT_OR_PARTIAL.containsMatchIn(url)
                ) {
                    val pageUrl = view?.url ?: url
                    mainHandler.post {
                        viewModel.onMediaDetected(
                            MediaCandidate(
                                url = url, mimeType = "application/vnd.apple.mpegurl",
                                source = SourceLayer.NETWORK_XHR, pageUrl = pageUrl))
                    }
                }
            }

            override fun doUpdateVisitedHistory(
                view: WebView?, url: String?, isReload: Boolean
            ) {
                url?.let(onUrlChanged)
            }
        }
        webChromeClient = WebChromeClient()
        loadUrl(HOME_URL)
    }
}

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
