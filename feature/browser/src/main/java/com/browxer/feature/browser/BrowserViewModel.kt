package com.browxer.feature.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browxer.core.data.DownloadRepository
import com.browxer.core.domain.DownloadTask
import com.browxer.core.domain.MediaCandidate
import com.browxer.core.domain.SourceLayer
import com.browxer.engine.downloader.DownloadService
import com.browxer.engine.downloader.friendlyError
import com.browxer.engine.ranker.MediaCandidateRanker
import com.browxer.engine.ranker.MediaSniffAdmission
import com.browxer.engine.ranker.ScoredCandidate
import com.browxer.engine.resolver.DashParser
import com.browxer.engine.resolver.HlsParser
import com.browxer.engine.resolver.YtdlpResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val ranker: MediaCandidateRanker,
    private val admission: MediaSniffAdmission,
    private val hlsParser: HlsParser,
    private val dashParser: DashParser,
    private val ytdlp: YtdlpResolver,
    private val downloads: DownloadRepository
) : ViewModel() {

    private val _fabState = MutableStateFlow<FabState>(FabState.Idle)
    val fabState: StateFlow<FabState> = _fabState.asStateFlow()

    private val _qualitySheetState = MutableStateFlow<QualitySheetState?>(null)
    val qualitySheetState: StateFlow<QualitySheetState?> = _qualitySheetState.asStateFlow()

    // 200-capped buffer, normalized-URL dedupe (bug rule #6)
    private val buffer = ArrayDeque<MediaCandidate>(MAX_CANDIDATES)

    fun onMediaDetected(candidate: MediaCandidate) {
        val key = admission.normalize(candidate.url)
        if (buffer.any { admission.normalize(it.url) == key }) return
        if (buffer.size >= MAX_CANDIDATES) buffer.removeFirst()
        buffer.addLast(candidate)
        recomputeFab()
    }

    private fun recomputeFab() {
        if (_fabState.value is FabState.Downloading) return
        val ranked = ranker.rank(buffer.toList())
        ranked.firstOrNull()?.let { top ->
            _fabState.value = FabState.MediaDetected(ranked.size, top.candidate)
        }
    }

    fun onFabClicked(pageUrl: String, cookies: String?, userAgent: String?) {
        if (_fabState.value !is FabState.MediaDetected) return
        val ranked = ranker.rank(buffer.toList())
        val top = ranked.firstOrNull() ?: return
        _qualitySheetState.value = QualitySheetState(
            mainCandidate = top.candidate,
            allCandidates = ranked.map { it.candidate }
        )
        resolveQualities(ranked, pageUrl, cookies, userAgent)
    }

    private fun resolveQualities(
        ranked: List<ScoredCandidate>,
        pageUrl: String, cookies: String?, userAgent: String?
    ) = viewModelScope.launch {
        _qualitySheetState.update { it?.copy(isLoading = true, errorMessage = null) }
        runCatching {
            val top = ranked.first()
            val variants: List<MediaCandidate> = when {
                top.candidate.isHls ->
                    hlsParser.parseManifest(top.candidate.url).map { v ->
                        top.candidate.copy(
                            url = v.url, resolution = v.resolution,
                            bandwidth = v.bandwidth.toLong(),
                            source = SourceLayer.HLS_VARIANT)
                    }
                top.candidate.isDash ->
                    dashParser.parseManifest(top.candidate.url)
                        .filter { !it.isAudio }
                        .map { r -> top.candidate.copy(
                            url = r.url,
                            resolution = r.width.toString() + "x" + r.height.toString(),
                            bandwidth = r.bandwidth,
                            source = SourceLayer.DASH_REP) }
                else -> ranked.map { it.candidate }
            }
            _qualitySheetState.update {
                it?.copy(mainCandidate = variants.first(),
                    allCandidates = variants, isLoading = false)
            }
        }.onFailure { e ->
            // If sniffing is stuck (top score < 60) -> honest yt-dlp fallback (score 95)
            if (ranker.needsYtdlpFallback(ranked)) {
                runCatching { ytdlp.extractCandidates(pageUrl, cookies) }
                    .onSuccess { fallback ->
                        fallback.forEach { onMediaDetected(it) }
                        _qualitySheetState.update {
                            it?.copy(mainCandidate = fallback.first(),
                                allCandidates = fallback,
                                isLoading = false, errorMessage = null)
                        }
                    }
                    .onFailure { fe ->
                        _qualitySheetState.update {
                            it?.copy(isLoading = false, errorMessage = friendlyError(fe))
                        }
                    }
            } else {
                _qualitySheetState.update {
                    it?.copy(isLoading = false, errorMessage = friendlyError(e))
                }
            }
        }
    }

    fun startDownload(
        candidate: MediaCandidate, fileName: String,
        cookies: String?, userAgent: String?
    ) {
        viewModelScope.launch {
            val kind = when {
                candidate.isHls -> DownloadTask.Kind.HLS
                candidate.isDash -> DownloadTask.Kind.DASH
                candidate.source == SourceLayer.YTDLP -> DownloadTask.Kind.YTDLP
                candidate.url.startsWith("blob:") -> DownloadTask.Kind.BLOB
                else -> DownloadTask.Kind.DIRECT
            }
            val id = downloads.enqueueDownload(
                candidate.url, fileName, candidate.mimeType, candidate.source.name)
            val task = DownloadTask(
                id = id, url = candidate.url, fileName = fileName,
                mimeType = candidate.mimeType, kind = kind,
                workDir = File(appContext.cacheDir, "downloads/" + id).absolutePath,
                pageUrl = candidate.pageUrl, cookies = cookies, userAgent = userAgent)
            DownloadService.enqueue(appContext, task)
            _fabState.value = FabState.Downloading(0)
            _qualitySheetState.value = null
        }
    }

    fun dismissSheet() { _qualitySheetState.value = null }

    companion object { private const val MAX_CANDIDATES = 200 }
}
