package com.browxer.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val NeonPurple = Color(0xFF8A2BE2)
val HotPink = Color(0xFFFF1493)
val DeepBlack = Color(0xFF0A0A0A)

/** Signature neon gradient for the FAB / Download button. */
val BrowxerGradient = Brush.linearGradient(listOf(NeonPurple, HotPink))

private val BrowxerDark = darkColorScheme(
    primary = NeonPurple,            // Neon Purple
    onPrimary = Color.Black,
    secondary = HotPink,             // Hot Pink
    onSecondary = Color.Black,
    background = DeepBlack,          // Deep Black
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1E1E1E),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    error = Color(0xFFFF4444)
)

/** The cyberpunk dark palette is enforced; no light theme is offered. */
@Composable
fun BrowxerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BrowxerDark, content = content)
}
