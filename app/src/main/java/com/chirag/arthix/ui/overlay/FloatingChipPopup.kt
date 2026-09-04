package com.chirag.arthix.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Display modes for the floating capture overlay.
 */
enum class OverlayDisplayState {
    EXPANDED,
    COLLAPSED
}

/**
 * Visual model for category buttons in the Floating Chip Popup.
 */
data class OverlayCategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
)

val DEFAULT_OVERLAY_CATEGORIES = listOf(
    OverlayCategoryItem(
        name = "Food",
        icon = Icons.Outlined.Fastfood,
        color = Color(0xFF1A1A1C),
        bgColor = Color(0xFFFFE0B2), // warm peach
    ),
    OverlayCategoryItem(
        name = "Travel",
        icon = Icons.Outlined.Flight,
        color = Color(0xFF1A1A1C),
        bgColor = Color(0xFFB3E5FC), // sky blue
    ),
    OverlayCategoryItem(
        name = "Shopping",
        icon = Icons.Outlined.ShoppingBag,
        color = Color(0xFF1A1A1C),
        bgColor = Color(0xFFE6E6FA), // lavender
    ),
    OverlayCategoryItem(
        name = "Other",
        icon = Icons.Outlined.MoreHoriz,
        color = Color(0xFF1A1A1C),
        bgColor = Color(0xFFEAEAEA), // neutral warm gray
    ),
)

/**
 * Floating on-screen pop-up overlay that appears when the user shakes their phone.
 *
 * Key features:
 * - Completely isolated from the Android notification system.
 * - Dual modes: [OverlayDisplayState.EXPANDED] (full-width 4-category bar with 5s countdown)
 *   and [OverlayDisplayState.COLLAPSED] (compact persistent glowing badge anchored to screen edge).
 * - Smooth auto-collapse transition after countdown, without losing pending capture context.
 * - Tap to re-expand collapsed badge into category chips.
 * - 4 vibrant category chips with 1-tap logging.
 * - Discard action to ignore false-positive shakes.
 * - Deep-link / open app action.
 */
@Composable
fun FloatingChipPopup(
    correlationId: String,
    categories: List<String> = listOf("Food", "Travel", "Shopping", "Other"),
    durationMs: Long = 7000L,
    initialState: OverlayDisplayState = OverlayDisplayState.EXPANDED,
    pendingCount: Int = 1,
    onStateChange: (OverlayDisplayState) -> Unit = {},
    onCategorySelected: (String) -> Unit,
    onDiscard: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    var displayState by remember { mutableStateOf(initialState) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(250)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(250)),
    ) {
        AnimatedContent(
            targetState = displayState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 50)) +
                    slideInVertically(animationSpec = tween(220, delayMillis = 50))) togetherWith
                    (fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(animationSpec = tween(180)))
            },
            label = "overlay_state_transition"
        ) { state ->
            when (state) {
                OverlayDisplayState.EXPANDED -> {
                    ExpandedChipView(
                        correlationId = correlationId,
                        categories = categories,
                        durationMs = durationMs,
                        onCategorySelected = { category ->
                            isVisible = false
                            onCategorySelected(category)
                        },
                        onDiscard = {
                            isVisible = false
                            onDiscard()
                        },
                        onOpenApp = onOpenApp,
                        onTimeout = {
                            displayState = OverlayDisplayState.COLLAPSED
                            onStateChange(OverlayDisplayState.COLLAPSED)
                        }
                    )
                }

                OverlayDisplayState.COLLAPSED -> {
                    CollapsedBadgeView(
                        pendingCount = pendingCount,
                        onExpand = {
                            displayState = OverlayDisplayState.EXPANDED
                            onStateChange(OverlayDisplayState.EXPANDED)
                        },
                        onDismiss = {
                            isVisible = false
                            onDiscard()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Expanded full-width category selection card with countdown progress bar.
 */
@Composable
private fun ExpandedChipView(
    correlationId: String,
    categories: List<String>,
    durationMs: Long,
    onCategorySelected: (String) -> Unit,
    onDiscard: () -> Unit,
    onOpenApp: () -> Unit,
    onTimeout: () -> Unit,
) {
    var progress by remember { mutableStateOf(1f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing),
        label = "countdown_progress"
    )

    LaunchedEffect(durationMs) {
        progress = 0f
        delay(durationMs)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFAF7F2), // soft cream
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0x22000000),
                    spotColor = Color(0x11000000)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ── Header Row ──────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Glowing bolt badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE4463A)) // Coral
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = "Shake Detected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenApp
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Shake Detected",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1C)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open App",
                                tint = Color(0xFF6B6B75),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "Select category to log payment",
                            fontSize = 12.sp,
                            color = Color(0xFF6B6B75),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Discard / Close Button
                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Discard shake",
                            tint = Color(0xFF1A1A1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Animated Countdown Progress Bar ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFFEAEAEA))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(3.dp)
                            .background(Color(0xFFE4463A))
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Category Chips Row ──────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val availableCategories = DEFAULT_OVERLAY_CATEGORIES.filter {
                        categories.isEmpty() || categories.any { c -> c.equals(it.name, ignoreCase = true) }
                    }.ifEmpty { DEFAULT_OVERLAY_CATEGORIES }

                    availableCategories.forEach { categoryItem ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onCategorySelected(categoryItem.name) }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(categoryItem.bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryItem.icon,
                                    contentDescription = categoryItem.name,
                                    tint = categoryItem.color,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = categoryItem.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact persistent floating pill badge anchored to the screen edge.
 * Tapping expands back into category selection.
 */
@Composable
private fun CollapsedBadgeView(
    pendingCount: Int = 1,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(end = 16.dp, top = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFFFFFFF),
            modifier = Modifier
                .scale(scalePulse)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x11000000)
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFE4463A).copy(alpha = glowAlpha),
                    shape = RoundedCornerShape(50)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                // Bolt Icon in coral
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = "Shake Captured",
                    tint = Color(0xFFE4463A),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = "Categorize",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1C)
                )

                if (pendingCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE4463A))
                    ) {
                        Text(
                            text = pendingCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF6B6B75),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
