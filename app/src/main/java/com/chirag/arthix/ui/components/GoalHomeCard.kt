package com.chirag.arthix.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.data.entity.GoalEntity

private object GoalCardColors {
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1A1A1C)
    val TextSecondary = Color(0xFF6B6B75)
    val TextMuted = Color(0xFF9A9AA5)
    val Coral = Color(0xFFE4463A)
    val Sage = Color(0xFF34A853)
    val PastelBlush = Color(0xFFFFE8E5)
    val Border = Color(0xFFF0EDE8)
    val Background = Color(0xFFFAF7F2)
}

@Composable
fun GoalHomeCard(
    activeGoals: List<GoalEntity>,
    onNavigateToGoals: () -> Unit,
    onAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToGoals),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GoalCardColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoalCardColors.PastelBlush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Savings,
                            contentDescription = null,
                            tint = GoalCardColors.Coral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Savings Goals",
                        color = GoalCardColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    if (activeGoals.isNotEmpty()) "View All (${activeGoals.size}) →" else "+ Set Goal",
                    color = GoalCardColors.Coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = if (activeGoals.isNotEmpty()) onNavigateToGoals else onAddGoal)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (activeGoals.isEmpty()) {
                // Empty state nudge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Plan for a purchase (e.g. mouse, trip) with a smart on-device savings suggestion.",
                        color = GoalCardColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Display primary active goal
                val topGoal = activeGoals.first()
                val savedRupees = topGoal.savedAmountPaise / 100
                val targetRupees = topGoal.targetAmountPaise / 100

                val animatedProgress by animateFloatAsState(
                    targetValue = topGoal.progressFraction,
                    animationSpec = tween(500),
                    label = "home_goal_progress"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        topGoal.title,
                        color = GoalCardColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "₹$savedRupees / ₹$targetRupees (${topGoal.progressPercent}%)",
                        color = GoalCardColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GoalCardColors.Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoalCardColors.Sage)
                    )
                }

                topGoal.notes?.let { note ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = GoalCardColors.Coral,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            note,
                            color = GoalCardColors.TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
