package com.browxer.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.browxer.core.ui.FabState
import com.browxer.core.ui.R
import com.browxer.core.ui.motion.BrowxerMotion
import com.browxer.core.ui.motion.rememberSystemAnimationsEnabled
import com.browxer.core.ui.theme.BrowxerGradient
import com.browxer.core.ui.theme.GlassBorder
import com.browxer.core.ui.theme.HotPink
import com.browxer.core.ui.theme.Surface2
import com.browxer.core.ui.theme.Surface3
import com.browxer.core.ui.theme.TextPrimary
import com.browxer.core.ui.theme.TextTertiary
import java.text.NumberFormat

/**
 * The three-state Magic FAB (Idle / MediaDetected / Downloading).
 *
 * Spec highlights:
 * - Pulse + glow ring only while media is detected, driven through
 *   graphicsLayer so animation frames never recompose.
 * - AnimatedContent for state swaps (never when + hard snap).
 * - Download progress ring is a Canvas drawArc — NOT a
 *   CircularProgressIndicator (neon aesthetic, gradient stroke).
 * - Haptics: LongPress when media is first detected, TextHandleMove at
 *   each 25% download tick.
 * - Honors Settings.Global.ANIMATOR_DURATION_SCALE == 0.
 */
@Composable
fun BrowxerFab(
    state: FabState,
    onIdleTap: () -> Unit,
    onMediaTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val detected = state is FabState.MediaDetected
    val animationsEnabled = rememberSystemAnimationsEnabled()

    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (detected && animationsEnabled) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BrowxerMotion.Slow, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (detected && animationsEnabled) 0.9f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(BrowxerMotion.Slow, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Press micro-interaction: shrink to 0.95 then spring back.
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = BrowxerMotion.SnappySpring,
        label = "press_scale"
    )

    // Haptics fire exactly once per meaningful transition / 25% tick.
    var lastProgressTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) {
        when (val s = state) {
            is FabState.MediaDetected ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            is FabState.Downloading -> {
                if (s.progress >= lastProgressTick + 25) {
                    lastProgressTick = s.progress
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            FabState.Idle -> lastProgressTick = 0
        }
    }

    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        // Glow ring — only while media is detected. graphicsLayer keeps the
        // pulse/glow outside of composition (zero recomposition per frame).
        if (detected) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = glowAlpha }
                    .background(
                        Brush.radialGradient(
                            listOf(HotPink.copy(alpha = 0.55f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        Box(
            Modifier
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        pressed = true
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            pressed = false
                        }
                    }
                }
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(BrowxerMotion.Quick)) +
                        scaleIn(initialScale = 0.9f, animationSpec = tween(BrowxerMotion.Quick)))
                        .togetherWith(
                            fadeOut(tween(BrowxerMotion.Quick)) +
                                scaleOut(targetScale = 0.9f, animationSpec = tween(BrowxerMotion.Quick))
                        )
                },
                label = "fab_state"
            ) { s ->
                when (s) {
                    FabState.Idle -> FabIdle(onIdleTap)
                    is FabState.MediaDetected -> FabDetected(onMediaTap, s.count)
                    is FabState.Downloading -> FabDownloading(s.progress)
                }
            }
        }
    }
}

@Composable
private fun FabIdle(onTap: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Surface3.copy(alpha = 0.85f))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = stringResource(R.string.fab_content_scan),
            tint = TextTertiary
        )
    }
}

@Composable
private fun FabDetected(onTap: () -> Unit, count: Int) {
    val countText = remember(count) {
        NumberFormat.getIntegerInstance().format(count)
    }
    Box {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BrowxerGradient)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = stringResource(R.string.fab_content_download),
                tint = Color.White
            )
        }
        if (count > 1) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HotPink)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    countText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FabDownloading(progress: Int) {
    val fraction = progress.coerceIn(0, 100) / 100f
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Surface2),
        contentAlignment = Alignment.Center
    ) {
        // Gradient progress ring — Canvas drawArc, NOT CircularProgressIndicator.
        Canvas(Modifier.size(64.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2f
            val ringSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = GlassBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = BrowxerGradient,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            remember(progress) { progress.coerceIn(0, 100).toString() + "%" },
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
