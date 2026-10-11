package com.browxer.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.browxer.core.ui.theme.BrowxerShapes
import com.browxer.core.ui.theme.BrowxerTypography

private val BrowxerDark = darkColorScheme(
    primary = com.browxer.core.ui.theme.NeonPurple,
    onPrimary = Color.Black,
    primaryContainer = com.browxer.core.ui.theme.NeonPurple,
    onPrimaryContainer = Color.White,
    secondary = com.browxer.core.ui.theme.HotPink,
    onSecondary = Color.Black,
    background = com.browxer.core.ui.theme.Surface0,
    onBackground = com.browxer.core.ui.theme.TextPrimary,
    surface = com.browxer.core.ui.theme.Surface1,
    onSurface = com.browxer.core.ui.theme.TextPrimary,
    surfaceVariant = com.browxer.core.ui.theme.Surface2,
    onSurfaceVariant = com.browxer.core.ui.theme.TextSecondary,
    surfaceTint = com.browxer.core.ui.theme.NeonPurple,
    inverseSurface = com.browxer.core.ui.theme.Surface3,
    inverseOnSurface = com.browxer.core.ui.theme.TextPrimary,
    error = com.browxer.core.ui.theme.ErrorRed,
    onError = Color.Black,
    outline = com.browxer.core.ui.theme.TextTertiary,
    outlineVariant = com.browxer.core.ui.theme.Surface3
)

/** The cyberpunk dark palette is enforced; no light theme is offered. */
@Composable
fun BrowxerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrowxerDark,
        typography = BrowxerTypography,
        shapes = BrowxerShapes,
        content = content
    )
}

// Back-compat re-exports — the brand tokens now live in
// com.browxer.core.ui.theme, but existing modules import them from here.
val NeonPurple: Color = com.browxer.core.ui.theme.NeonPurple
val HotPink: Color = com.browxer.core.ui.theme.HotPink
val DeepBlack: Color = com.browxer.core.ui.theme.DeepBlack
val BrowxerGradient: Brush = com.browxer.core.ui.theme.BrowxerGradient
