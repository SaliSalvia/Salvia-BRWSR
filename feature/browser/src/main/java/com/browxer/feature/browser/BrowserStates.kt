package com.browxer.feature.browser

import com.browxer.core.domain.MediaCandidate

sealed interface FabState {
    data object Idle : FabState
    data class MediaDetected(val count: Int, val top: MediaCandidate) : FabState
    data class Downloading(val progress: Int) : FabState
}

data class QualitySheetState(
    val mainCandidate: MediaCandidate,
    val allCandidates: List<MediaCandidate>,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    /** Explicit unsupported state — honest UI, never a fake download. */
    val isUnsupported: Boolean get() = errorMessage != null
}
