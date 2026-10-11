package com.browxer.feature.browser

/**
 * Shared browser UI states live in :core:ui so :feature:browser and
 * :feature:download_sheet both depend on it one-way (no module cycle).
 */
typealias FabState = com.browxer.core.ui.FabState
typealias QualitySheetState = com.browxer.core.ui.QualitySheetState
