package com.chirag.arthix.ui.nav

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.CancellationException

/**
 * The center Voice button.
 *
 * Interaction contract:
 *  - finger down            -> onPressStart() fires immediately, animation begins
 *  - finger up (normal)     -> onPressEnd() fires, animation settles down
 *  - finger dragged away / pointer canceled -> onPressCancel() fires, no log is created
 *
 * [amplitudeProvider] should return a 0f..1f value sampled from your actual audio
 * input (e.g. MediaRecorder.getMaxAmplitude() normalized). While idle it can just
 * return 0f — the rings will still animate on a gentle idle pulse.
 */
@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    amplitudeProvider: () -> Float,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceRings")
    val ringPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ringPhase"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.08f else 1f,
        label = "buttonScale"
    )

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // --- Animated listening rings, only drawn while actively recording ---
        if (isRecording) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val amp = amplitudeProvider().coerceIn(0f, 1f)
                val baseRadius = size.minDimension / 2f
                repeat(3) { i ->
                    val t = (ringPhase + i / 3f) % 1f
                    val radius = baseRadius * (0.5f + t * (0.5f + amp * 0.35f))
                    val alpha = (1f - t).coerceIn(0f, 1f) * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ArthixNavColors.Coral.copy(alpha = alpha),
                                ArthixNavColors.CoralLight.copy(alpha = 0f)
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = radius.coerceAtLeast(1f)
                        ),
                        radius = radius.coerceAtLeast(1f),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
        }

        // --- The solid tappable circle ---
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(
                    elevation = if (isRecording) 22.dp else 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = ArthixNavColors.Coral.copy(alpha = 0.5f),
                    spotColor = ArthixNavColors.Coral.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(ArthixNavColors.Coral, ArthixNavColors.CoralLight)
                    )
                )
                .pointerInput(Unit) {
                    detectVoicePress(
                        onPressStart = onPressStart,
                        onPressEnd = onPressEnd,
                        onPressCancel = onPressCancel
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Hold to speak",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Low-level press/release/cancel detector. Using onPress + awaitRelease (rather than
 * a plain clickable) is what makes this a true "hold to talk" button instead of a tap
 * toggle — the mic is live for exactly as long as the finger is down.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectVoicePress(
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onPressCancel: () -> Unit
) {
    detectTapGestures(
        onPress = {
            onPressStart()
            try {
                val released = tryAwaitRelease()
                if (released) onPressEnd() else onPressCancel()
            } catch (c: CancellationException) {
                onPressCancel()
                throw c
            }
        }
    )
}
