package com.browxer.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.browxer.core.ui.motion.rememberSystemAnimationsEnabled
import com.browxer.core.ui.theme.ShimmerBase
import com.browxer.core.ui.theme.ShimmerHighlight

/** Neon shimmer placeholder for any loading surface. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    if (rememberSystemAnimationsEnabled()) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_x"
        )
        val brush = Brush.linearGradient(
            colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
            start = Offset(progress * 1600f - 800f, 0f),
            end = Offset(progress * 1600f, 0f)
        )
        Box(modifier.background(brush, shape))
    } else {
        // Reduced-motion: static placeholder, no animation loop.
        Box(modifier.background(ShimmerBase, shape))
    }
}

/** Skeleton row for lazy lists (thumbnail + two text lines). */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(Modifier.size(56.dp), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            ShimmerBox(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.height(8.dp))
            ShimmerBox(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }
    }
}
