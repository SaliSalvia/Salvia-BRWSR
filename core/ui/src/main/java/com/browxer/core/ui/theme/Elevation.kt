package com.browxer.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Semantic elevation levels. Feature code must not scatter arbitrary
 * Modifier.shadow(7.dp) — reference a level from here instead.
 */
object BrowxerElevation {
    val flat   = 0.dp
    val card   = 2.dp
    val fab    = 8.dp
    val sheet  = 12.dp
    val dialog = 24.dp
}
