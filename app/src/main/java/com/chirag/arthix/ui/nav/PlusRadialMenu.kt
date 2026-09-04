package com.chirag.arthix.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Plus button AND its radial menu, as one component so tap and press-hold-drag
 * can never visually drift apart again.
 *
 * Behavior:
 *  - Quick tap            -> menu fans open statically. Tap an option to select it,
 *                             tap anywhere outside to dismiss (see [onExpandedChange]).
 *  - Press, hold, drag     -> menu fans open the instant the long-press threshold is
 *                             hit. Whichever option the thumb is currently over is
 *                             highlighted in real time. Releasing over an option
 *                             selects it; releasing anywhere else cancels.
 *
 * Arc geometry:
 *   The three options fan in a tight arc ABOVE the plus button at a generous radius
 *   (120dp) so they clear the capsule bar and voice button entirely. The arc spans
 *   roughly 100° centered on the upper-left quadrant (since the Plus button sits
 *   at the far right of the capsule):
 *     - Streaks  at 130° (upper-left, farthest from edge)
 *     - Account  at 100° (upper-center-left)
 *     - Camera   at  70° (nearly straight up, closest to edge)
 *
 * [onExpandedChange] is provided so the caller can draw a full-screen transparent
 * scrim behind the whole nav bar to catch outside taps and dismiss the menu.
 */
@Composable
fun PlusRadialMenu(
    modifier: Modifier = Modifier,
    onOptionSelected: (PlusOption) -> Unit,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    var mode by remember { mutableStateOf(PlusMenuMode.CLOSED) }
    var highlighted by remember { mutableStateOf<PlusOption?>(null) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val options = remember { listOf(PlusOption.CAMERA, PlusOption.STREAKS, PlusOption.ACCOUNT) }
    
    val buttonSizeDp = 44.dp
    val spacingPx = with(density) { 56.dp.toPx() } // vertical distance between items
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }

    fun setMode(newMode: PlusMenuMode) {
        mode = newMode
        onExpandedChange(newMode != PlusMenuMode.CLOSED)
        if (newMode == PlusMenuMode.CLOSED) highlighted = null
    }

    val gapDp = 12.dp
    val pillHeightDp = 36.dp
    val navbarHalfHeightDp = 32.dp
    val firstOffsetDp = navbarHalfHeightDp + gapDp + (pillHeightDp / 2f)
    val spacingDp = pillHeightDp + gapDp

    fun optionOffset(index: Int): Offset {
        val reversedIndex = options.size - 1 - index
        val yOffsetDp = firstOffsetDp + (spacingDp * reversedIndex.toFloat())
        val yOffset = -with(density) { yOffsetDp.toPx() }
        return Offset(x = 0f, y = yOffset)
    }

    fun nearestOption(localPos: Offset): PlusOption? {
        val center = Offset(buttonSizePx / 2f, buttonSizePx / 2f)
        val relY = localPos.y - center.y
        val spacingPxVal = with(density) { spacingDp.toPx() }
        val firstOffsetPx = with(density) { firstOffsetDp.toPx() }
        
        if (relY > -(firstOffsetPx - spacingPxVal * 0.5f)) return null // Below the first item

        val distFromFirst = -relY - firstOffsetPx
        val slotIndex = (distFromFirst / spacingPxVal).roundToInt().coerceIn(0, options.size - 1)
        val actualIndex = options.size - 1 - slotIndex
        return options.getOrNull(actualIndex)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // --- Vertical options: stack out above the plus button, high z-index ---
        options.forEachIndexed { index, option ->
            val open = mode != PlusMenuMode.CLOSED
            val target = if (open) optionOffset(index) else Offset.Zero
            val animatedX by animateFloatAsState(
                targetValue = target.x,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
                label = "arcX$index"
            )
            val animatedY by animateFloatAsState(
                targetValue = target.y,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
                label = "arcY$index"
            )
            val isHighlighted = highlighted == option
            val pillScale by animateFloatAsState(
                targetValue = if (isHighlighted) 1.16f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                label = "pillScale$index"
            )

            AnimatedVisibility(
                visible = open,
                enter = fadeIn(tween(250, delayMillis = index * 45)) +
                    scaleIn(initialScale = 0.4f, animationSpec = tween(300, delayMillis = index * 45)),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.4f, animationSpec = tween(200)),
                modifier = Modifier
                    .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                    .zIndex(10f) // above everything in the nav bar
            ) {
                ArcOptionPill(
                    option = option,
                    highlighted = isHighlighted,
                    scale = pillScale,
                    onTap = {
                        onOptionSelected(option)
                        setMode(PlusMenuMode.CLOSED)
                    }
                )
            }
        }

        // --- Central Plus button ---
        val rotation by animateFloatAsState(
            targetValue = if (mode != PlusMenuMode.CLOSED) 45f else 0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
            label = "plusRotation"
        )

        Box(
            modifier = Modifier
                .size(buttonSizeDp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(ArthixNavColors.CapsuleBackgroundElevated)
                .pointerInput(Unit) {
                    detectPlusGesture(
                        onTapToggle = {
                            setMode(if (mode == PlusMenuMode.TAP_OPEN) PlusMenuMode.CLOSED else PlusMenuMode.TAP_OPEN)
                        },
                        onLongPressStart = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            setMode(PlusMenuMode.DRAG_OPEN)
                        },
                        onDragMove = { pos ->
                            val newHighlight = nearestOption(pos)
                            if (newHighlight != highlighted) {
                                highlighted = newHighlight
                                if (newHighlight != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragRelease = {
                            highlighted?.let { onOptionSelected(it) }
                            setMode(PlusMenuMode.CLOSED)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PlusIcon,
                contentDescription = "More options",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

/**
 * Manual low-level gesture disambiguator:
 *  - released before long-press threshold -> onTapToggle()
 *  - held past threshold -> onLongPressStart(), then every subsequent move ->
 *    onDragMove(position), and release -> onDragRelease()
 */
private suspend fun PointerInputScope.detectPlusGesture(
    onTapToggle: () -> Unit,
    onLongPressStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragRelease: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        val timeoutMs = viewConfiguration.longPressTimeoutMillis
        var becameLongPress = false

        // Phase 1: race the long-press timeout against a release.
        while (true) {
            val event = withTimeoutOrNull(timeoutMs) { awaitPointerEvent() }
            if (event == null) {
                becameLongPress = true
                onLongPressStart()
                break
            }
            val change = event.changes.firstOrNull { it.id == pointerId }
            if (change == null || !change.pressed) {
                // Released before threshold -> simple tap.
                onTapToggle()
                return@awaitEachGesture
            }
        }

        // Phase 2: long-press confirmed — track the drag until release.
        if (becameLongPress) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                if (!change.pressed) {
                    onDragRelease()
                    break
                } else {
                    onDragMove(change.position)
                    change.consume()
                }
            }
        }
    }
}

/** One pill in the arc: identical visual language whether reached by tap or drag. */
@Composable
private fun ArcOptionPill(
    option: PlusOption,
    highlighted: Boolean,
    scale: Float,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (highlighted) 16.dp else 8.dp,
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) ArthixNavColors.Coral else ArthixNavColors.CapsuleBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = option.label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
