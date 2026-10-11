package com.browxer.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Brand ────────────────────────────────────────────────────────────────
val NeonPurple = Color(0xFF8A2BE2)
val HotPink    = Color(0xFFFF1493)
val DeepBlack  = Color(0xFF0A0A0A)

// ── Surface scale (dark theme — primary) ─────────────────────────────────
val Surface0 = Color(0xFF0A0A0A) // app background
val Surface1 = Color(0xFF141414) // cards, sheets
val Surface2 = Color(0xFF1E1E1E) // elevated
val Surface3 = Color(0xFF2A2A2A) // dialogs
val Surface4 = Color(0xFF363636) // hover/pressed

// ── Text scale ───────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFEDEDED) // AA contrast on Surface0
val TextSecondary = Color(0xFFB0B0B0)
// WCAG 2.1 AA fix: 0x7A7A7A on Surface0 was only 3.8:1 — bumped to 8A8A8A.
val TextTertiary = Color(0xFF8A8A8A)
val TextDisabled = Color(0xFF4A4A4A)

// ── Semantic ─────────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF22C55E)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed     = Color(0xFFEF4444)
val InfoBlue     = Color(0xFF3B82F6)

// ── Overlays & effects ───────────────────────────────────────────────────
val GlassOverlay   = Color(0x1AFFFFFF) // 10% white
val GlassBorder    = Color(0x33FFFFFF) // 20% white
val ShimmerBase    = Color(0xFF1E1E1E)
val ShimmerHighlight = Color(0xFF2E2E2E)

/** Signature neon gradient for the FAB / primary buttons. */
val BrowxerGradient = Brush.linearGradient(listOf(NeonPurple, HotPink))
