package com.chirag.arthix.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme

/**
 * Skeleton loader — grey placeholder blocks (PRD §5, §6.7).
 *
 * Shimmer animation for loading states. Never shows a blank/frozen
 * screen per NFR-4 (report generation up to 15s).
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Hero card skeleton
        SkeletonBlock(height = 120.dp, alpha = alpha)
        Spacer(Modifier.height(16.dp))

        // Insight cards skeleton
        repeat(3) {
            SkeletonBlock(height = 80.dp, alpha = alpha)
            Spacer(Modifier.height(12.dp))
        }

        // List rows skeleton
        repeat(4) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SkeletonBlock(
                    height = 48.dp,
                    alpha = alpha,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                SkeletonBlock(
                    height = 48.dp,
                    alpha = alpha,
                    modifier = Modifier.width(80.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SkeletonBlock(
    height: Dp,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.border.copy(alpha = alpha)),
    )
}
