package com.browxer.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Single source of truth for durations and spring specs. Feature code must
 * never scatter raw tween(300) — always reference BrowxerMotion.
 */
object BrowxerMotion {
    /** State toggles where a snap is acceptable. */
    const val Instant = 80

    /** Ripple, checkbox, small affordances. */
    const val Quick = 150

    /** Screen transitions. */
    const val Standard = 250

    /** Shared elements, sheets. */
    const val Emphasized = 400

    /** FAB pulse, success celebrations. */
    const val Slow = 700

    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy, // 0.7
        stiffness = Spring.StiffnessMediumLow           // 400
    )
    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,      // 1.0
        stiffness = Spring.StiffnessLow                  // 200
    )
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,   // 0.5
        stiffness = Spring.StiffnessMedium              // 800
    )
}

/**
 * Accessibility: when the user disables system animations (animator
 * duration scale == 0) every decorative/infinite animation must be
 * neutralized. All Browxer components consult this before pulsing.
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }.getOrDefault(true)
    }
}
