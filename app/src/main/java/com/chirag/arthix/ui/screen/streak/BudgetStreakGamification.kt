package com.chirag.arthix.ui.screen.streak

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.chirag.arthix.ui.components.FloatingEmbers
import com.chirag.arthix.ui.components.ConfettiBurst

private object GamificationColors {
    val FlameEmber = Color(0xFFFFB74D)
    val FlameMedium = Color(0xFFFF9800)
    val FlameBright = Color(0xFFFF5722)
    val FlamePeak = Color(0xFFFFD54F)
    
    val BadgeEarned = Color(0xFFFFC107)
    val BadgeUnearned = Color(0xFFF0EDE8)
    val BadgeText = Color(0xFF1A1A1C)
}

val Milestones = listOf(3, 7, 14, 21, 30)

@Composable
fun StreakFlameHeader(heldStreak: Int) {
    val transition = rememberInfiniteTransition(label = "flame_pulse")
    
    val (flameColor, flameSize, textStr) = when {
        heldStreak >= 30 -> Triple(GamificationColors.FlamePeak, 48.dp, "Peak Streak!")
        heldStreak >= 14 -> Triple(GamificationColors.FlameBright, 40.dp, "On Fire!")
        heldStreak >= 7 -> Triple(GamificationColors.FlameMedium, 32.dp, "Heating Up!")
        heldStreak >= 3 -> Triple(GamificationColors.FlameEmber, 28.dp, "Gaining Momentum")
        else -> Triple(GamificationColors.FlameEmber.copy(alpha = 0.5f), 24.dp, "Building the Habit")
    }

    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(flameSize * 1.5f),
            contentAlignment = Alignment.Center
        ) {
            if (heldStreak >= 14) {
                FloatingEmbers(
                    modifier = Modifier.fillMaxSize().padding(bottom = flameSize * 0.2f),
                    color = flameColor
                )
            }
            if (heldStreak >= 30) {
                // simple glowing halo
                Box(modifier = Modifier.fillMaxSize().scale(scale * 1.2f).background(flameColor.copy(alpha = 0.2f), CircleShape))
            }
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak Flame",
                tint = flameColor,
                modifier = Modifier
                    .size(flameSize)
                    .scale(if (heldStreak >= 3) scale else 1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = textStr,
            color = Color(0xFF1A1A1C),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MilestoneBadgesRow(heldStreak: Int, daysElapsed: Int) {
    var triggerPop by remember { mutableStateOf(-1) }
    var triggerConfetti by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(heldStreak, daysElapsed) {
        // If we exactly hit a milestone today, trigger the pop
        if (heldStreak == daysElapsed && Milestones.contains(heldStreak)) {
            triggerPop = heldStreak
            triggerConfetti = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(3000)
            triggerPop = -1
            triggerConfetti = false
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Milestones.forEach { milestone ->
                val isEarned = heldStreak >= milestone
                val isPopping = triggerPop == milestone
                
                val scaleAnim by animateFloatAsState(
                    targetValue = if (isPopping) 1.3f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "badge_scale"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(scaleAnim)
                            .clip(CircleShape)
                            .background(if (isEarned) GamificationColors.BadgeEarned else GamificationColors.BadgeUnearned)
                            .border(
                                2.dp, 
                                if (isEarned) Color.White.copy(alpha = 0.5f) else Color.Transparent, 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Milestone $milestone",
                            tint = if (isEarned) Color.White else Color(0xFF9A9AA5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$milestone",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEarned) GamificationColors.BadgeText else Color(0xFF9A9AA5)
                    )
                }
            }
        }
        
        if (triggerConfetti) {
            ConfettiBurst(modifier = Modifier.size(200.dp), isRunning = true)
        }
    }
}

@Composable
fun ProgressToNextMilestone(heldStreak: Int) {
    val nextMilestone = Milestones.firstOrNull { it > heldStreak }
    if (nextMilestone != null) {
        val prevMilestone = Milestones.lastOrNull { it <= heldStreak } ?: 0
        val totalNeeded = nextMilestone - prevMilestone
        val currentProgress = heldStreak - prevMilestone
        val progressPercent = (currentProgress.toFloat() / totalNeeded.toFloat()).coerceIn(0f, 1f)

        val animatedProgress by animateFloatAsState(
            targetValue = progressPercent,
            animationSpec = tween(500),
            label = "progress"
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${nextMilestone - heldStreak} days to next badge!",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B6B75)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF0EDE8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF34A853)) // Sage
                )
            }
        }
    } else {
        Text(
            text = "All badges unlocked!",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = GamificationColors.BadgeEarned
        )
    }
}

@Composable
fun StreakCompleteOverlay(
    isVisible: Boolean,
    totalSaved: Long,
    longestChain: Int,
    onStartNew: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF7F2).copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfettiBurst(modifier = Modifier.fillMaxSize(), particleCount = 100, isRunning = isVisible)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment, 
                    contentDescription = null,
                    tint = GamificationColors.FlamePeak,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Streak Complete!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GamificationColors.BadgeText
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You've successfully finished your 30-day budget challenge.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B6B75)
                )
                
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("₹$totalSaved", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34A853))
                        Text("Total Saved", fontSize = 12.sp, color = Color(0xFF9A9AA5))
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFF0EDE8)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$longestChain", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GamificationColors.FlameMedium)
                        Text("Longest Chain", fontSize = 12.sp, color = Color(0xFF9A9AA5))
                    }
                }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onStartNew,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4463A)), // Coral
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Start a New Streak", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
