package com.chirag.arthix.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
        color = Color(0xFFFF8A65),
        bgColor = Color(0x33FF8A65),
    ),
    OverlayCategoryItem(
        name = "Travel",
        icon = Icons.Outlined.Flight,
        color = Color(0xFF38BDF8),
        bgColor = Color(0x3338BDF8),
    ),
    OverlayCategoryItem(
        name = "Shopping",
        icon = Icons.Outlined.ShoppingBag,
        color = Color(0xFFC084FC),
        bgColor = Color(0x33C084FC),
    ),
    OverlayCategoryItem(
        name = "Other",
        icon = Icons.Outlined.MoreHoriz,
        color = Color(0xFF94A3B8),
        bgColor = Color(0x3394A3B8),
    ),
)

/**
 * Floating on-screen pop-up overlay that appears when the user shakes their phone.
 *
 * Key features:
 * - Completely isolated from the Android notification system (won't be displaced by GPay/Bank alerts).
 * - Smooth countdown progress bar (default 7s).
 * - 4 vibrant category chips with 1-tap logging.
 * - Discard action to ignore false-positive shakes.
 * - Deep-link / open app action.
 */
@Composable
fun FloatingChipPopup(
    correlationId: String,
    categories: List<String> = listOf("Food", "Travel", "Shopping", "Other"),
    durationMs: Long = 7000L,
    onCategorySelected: (String) -> Unit,
    onDiscard: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(1f) }

    // Start entrance animation
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Animated countdown progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing),
        label = "countdown_progress"
    )

    // Trigger timer countdown and auto-dismissal
    LaunchedEffect(durationMs) {
        progress = 0f
        delay(durationMs)
        isVisible = false
        delay(300) // allow exit animation to finish
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(250)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(250)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141722),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0x88000000),
                        spotColor = Color(0xAA00F59B)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A344A),
                                Color(0xFF00E5FF).copy(alpha = 0.4f),
                                Color(0xFF00F59B).copy(alpha = 0.4f),
                                Color(0xFF2A344A)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
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
                        // Glowing badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00F59B).copy(alpha = 0.35f),
                                            Color(0xFF00E5FF).copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFF00F59B).copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = "Shake Detected",
                                tint = Color(0xFF00F59B),
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
                                    color = Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open App",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "Select category to log payment",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Discard / Close Button
                        IconButton(
                            onClick = {
                                isVisible = false
                                onDiscard()
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222838))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Discard shake",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
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
                            .background(Color(0xFF1E2638))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF00F59B)
                                        )
                                    )
                                )
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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(categoryItem.bgColor)
                                    .border(
                                        width = 1.dp,
                                        color = categoryItem.color.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        isVisible = false
                                        onCategorySelected(categoryItem.name)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = categoryItem.icon,
                                        contentDescription = categoryItem.name,
                                        tint = categoryItem.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = categoryItem.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
