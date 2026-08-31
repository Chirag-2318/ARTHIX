package com.chirag.arthix.ui.screen.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * These are deliberately drawn with Canvas rather than imported PNGs/SVGs so
 * this file compiles and runs with zero extra assets. Swap any of these for
 * a Lottie composition later (see LottieOnboardingIllustration at the
 * bottom) once your designer hands off real animation files — the call
 * site in OnboardingScreen.kt doesn't need to change.
 */

private const val STROKE_FRACTION = 0.055f

@Composable
fun IllustrationDisc(
    discColor: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 132.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(color = discColor)
        }
        content()
    }
}

/** Page 1 — a phone mid-shake, motion arcs fanning out on both sides. */
@Composable
fun ShakeIllustration(foreground: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * STROKE_FRACTION

        val phoneWidth = w * 0.36f
        val phoneHeight = h * 0.64f
        val left = (w - phoneWidth) / 2f
        val top = (h - phoneHeight) / 2f

        // Phone body
        drawRoundRect(
            color = foreground,
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.26f, phoneWidth * 0.26f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Home indicator
        drawLine(
            color = foreground,
            start = Offset(left + phoneWidth * 0.35f, top + phoneHeight * 0.90f),
            end = Offset(left + phoneWidth * 0.65f, top + phoneHeight * 0.90f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
        // Rupee glyph on the screen to tie the icon to "money"
        drawLine(
            color = foreground,
            start = Offset(left + phoneWidth * 0.38f, top + phoneHeight * 0.30f),
            end = Offset(left + phoneWidth * 0.62f, top + phoneHeight * 0.30f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(left + phoneWidth * 0.38f, top + phoneHeight * 0.42f),
            end = Offset(left + phoneWidth * 0.62f, top + phoneHeight * 0.42f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(left + phoneWidth * 0.40f, top + phoneHeight * 0.30f),
            end = Offset(left + phoneWidth * 0.46f, top + phoneHeight * 0.58f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(left + phoneWidth * 0.46f, top + phoneHeight * 0.42f),
            end = Offset(left + phoneWidth * 0.60f, top + phoneHeight * 0.58f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )

        // Motion arcs, left side
        drawArc(
            color = foreground.copy(alpha = 0.55f),
            startAngle = 150f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(left - w * 0.24f, top - h * 0.02f),
            size = Size(phoneWidth * 0.95f, phoneHeight * 1.05f),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
        // Motion arcs, right side
        drawArc(
            color = foreground.copy(alpha = 0.55f),
            startAngle = -50f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(left + phoneWidth * 0.30f, top - h * 0.02f),
            size = Size(phoneWidth * 0.95f, phoneHeight * 1.05f),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
    }
}

/** Page 2 — a notification bell with a small "captured" checkmark badge. */
@Composable
fun NotificationCaptureIllustration(foreground: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * STROKE_FRACTION
        val cx = w / 2f
        val bellTop = h * 0.24f
        val bellWidth = w * 0.42f
        val bellHeight = h * 0.40f

        // Bell body (arc + sides)
        drawArc(
            color = foreground,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - bellWidth / 2f, bellTop),
            size = Size(bellWidth, bellHeight),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = foreground,
            start = Offset(cx - bellWidth / 2f, bellTop + bellHeight / 2f),
            end = Offset(cx - bellWidth / 2f - w * 0.04f, bellTop + bellHeight * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(cx + bellWidth / 2f, bellTop + bellHeight / 2f),
            end = Offset(cx + bellWidth / 2f + w * 0.04f, bellTop + bellHeight * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = foreground,
            start = Offset(cx - bellWidth / 2f - w * 0.04f, bellTop + bellHeight * 0.92f),
            end = Offset(cx + bellWidth / 2f + w * 0.04f, bellTop + bellHeight * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Clapper
        drawArc(
            color = foreground,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.06f, bellTop + bellHeight * 0.92f),
            size = Size(w * 0.12f, w * 0.10f),
            style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
        )

        // Captured badge, bottom-right
        val badgeRadius = w * 0.16f
        val badgeCenter = Offset(cx + bellWidth * 0.55f, bellTop + bellHeight * 1.15f)
        drawCircle(color = foreground, radius = badgeRadius, center = badgeCenter)
        drawPoints(
            points = listOf(
                Offset(badgeCenter.x - badgeRadius * 0.45f, badgeCenter.y),
                Offset(badgeCenter.x - badgeRadius * 0.10f, badgeCenter.y + badgeRadius * 0.35f),
                Offset(badgeCenter.x + badgeRadius * 0.5f, badgeCenter.y - badgeRadius * 0.35f)
            ),
            pointMode = androidx.compose.ui.graphics.PointMode.Polygon,
            color = discBackgroundContrast(foreground),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

/** Falls back to a dark check mark on light badges, light check on dark badges. */
private fun discBackgroundContrast(foreground: Color): Color {
    val luminance = 0.299f * foreground.red + 0.587f * foreground.green + 0.114f * foreground.blue
    return if (luminance > 0.6f) Color(0xFF171526) else Color.White
}

/** Page 3 — an upward bar chart with a trend line, for the insights page. */
@Composable
fun InsightsIllustration(foreground: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * STROKE_FRACTION
        val baseline = h * 0.78f
        val barWidth = w * 0.12f
        val gap = w * 0.08f
        val heights = listOf(0.30f, 0.50f, 0.38f, 0.68f)
        val startX = w * 0.16f

        heights.forEachIndexed { index, fraction ->
            val barHeight = h * 0.5f * fraction
            val x = startX + index * (barWidth + gap)
            drawRoundRect(
                color = foreground.copy(alpha = if (index == heights.lastIndex) 1f else 0.45f),
                topLeft = Offset(x, baseline - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.3f, barWidth * 0.3f)
            )
        }
        // Trend line across the bar tops
        val path = androidx.compose.ui.graphics.Path().apply {
            heights.forEachIndexed { index, fraction ->
                val barHeight = h * 0.5f * fraction
                val x = startX + index * (barWidth + gap) + barWidth / 2f
                val y = baseline - barHeight - h * 0.06f
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = foreground,
            style = Stroke(width = strokeWidth * 0.6f, cap = StrokeCap.Round)
        )
        // Baseline
        drawLine(
            color = foreground.copy(alpha = 0.3f),
            start = Offset(w * 0.10f, baseline),
            end = Offset(w * 0.90f, baseline),
            strokeWidth = strokeWidth * 0.4f,
            cap = StrokeCap.Round
        )
    }
}

/** Page 4 — a shield with a lock, for the privacy / permission page. */
@Composable
fun PrivacyShieldIllustration(foreground: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * STROKE_FRACTION
        val cx = w / 2f

        val shieldPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, h * 0.16f)
            lineTo(w * 0.74f, h * 0.28f)
            lineTo(w * 0.74f, h * 0.55f)
            cubicTo(w * 0.74f, h * 0.78f, cx, h * 0.90f, cx, h * 0.90f)
            cubicTo(cx, h * 0.90f, w * 0.26f, h * 0.78f, w * 0.26f, h * 0.55f)
            lineTo(w * 0.26f, h * 0.28f)
            close()
        }
        drawPath(
            path = shieldPath,
            color = foreground,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Lock body
        val lockWidth = w * 0.22f
        val lockHeight = h * 0.18f
        drawRoundRect(
            color = foreground,
            topLeft = Offset(cx - lockWidth / 2f, h * 0.50f),
            size = Size(lockWidth, lockHeight),
            cornerRadius = CornerRadius(lockWidth * 0.2f, lockWidth * 0.2f),
            style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
        )
        // Lock shackle
        drawArc(
            color = foreground,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - lockWidth * 0.32f, h * 0.36f),
            size = Size(lockWidth * 0.64f, lockWidth * 0.64f),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
    }
}
