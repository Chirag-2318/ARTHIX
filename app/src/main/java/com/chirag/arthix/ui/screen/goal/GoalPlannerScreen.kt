package com.chirag.arthix.ui.screen.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.entity.GoalEntity
import com.chirag.arthix.data.model.GoalPlanType
import com.chirag.arthix.ui.components.ConfettiBurst

private object GoalColors {
    val Background = Color(0xFFFAF7F2)       // warm cream
    val Surface = Color(0xFFFFFFFF)          // white card
    val TextPrimary = Color(0xFF1A1A1C)      // near-black
    val TextSecondary = Color(0xFF6B6B75)    // muted gray
    val TextMuted = Color(0xFF9A9AA5)
    val Coral = Color(0xFFE4463A)            // action coral
    val Sage = Color(0xFF34A853)             // positive green
    val SageBg = Color(0xFFE5F5E0)
    val PastelBlush = Color(0xFFFFE8E5)
    val PastelSky = Color(0xFFE5F0FF)
    val PastelCream = Color(0xFFFFF5E5)
    val Border = Color(0xFFF0EDE8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalPlannerScreen(
    onBack: () -> Unit = {},
    onAddGoal: () -> Unit = {},
    viewModel: GoalListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Completed
    var goalToContribute by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }

    // Quick Add Contribution Dialog
    goalToContribute?.let { goal ->
        var amountStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { goalToContribute = null },
            title = {
                Text(
                    "Add to ${goal.title}",
                    fontWeight = FontWeight.Bold,
                    color = GoalColors.TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "How much did you save or set aside today?",
                        color = GoalColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                        placeholder = { Text("500") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addSavings(goal.id, (amount * 100).toLong())
                        }
                        goalToContribute = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoalColors.Coral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Savings")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToContribute = null }) {
                    Text("Cancel", color = GoalColors.TextSecondary)
                }
            },
            containerColor = GoalColors.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = {
                Text(
                    "Delete Goal?",
                    fontWeight = FontWeight.Bold,
                    color = GoalColors.TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${goal.title}\"? Progress will be cleared.",
                    color = GoalColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoalColors.Coral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel", color = GoalColors.TextSecondary)
                }
            },
            containerColor = GoalColors.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Celebration Overlay when Goal hits 100%
    uiState.newlyCompletedGoal?.let { completedGoal ->
        GoalCompletionOverlay(
            goal = completedGoal,
            onDismiss = { viewModel.dismissCelebration() }
        )
    }

    Scaffold(
        containerColor = GoalColors.Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGoal,
                containerColor = GoalColors.Coral,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                text = { Text("New Goal", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(GoalColors.Background)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GoalColors.TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI Goal Planner",
                    color = GoalColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Summary Card
                item {
                    val savedRupees = uiState.totalSavedPaise / 100
                    val targetRupees = uiState.totalTargetPaise / 100

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = GoalColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "TOTAL SAVINGS PROGRESS",
                                        color = GoalColors.TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "₹$savedRupees / ₹$targetRupees",
                                        color = GoalColors.TextPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GoalColors.SageBg)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "${uiState.overallProgressPercent}%",
                                        color = GoalColors.Sage,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Overall progress bar
                            val animatedProgress by animateFloatAsState(
                                targetValue = (uiState.overallProgressPercent / 100f).coerceIn(0f, 1f),
                                animationSpec = tween(600),
                                label = "overall_progress"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(GoalColors.Border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(GoalColors.Sage)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Plans are computed 100% on-device from your spending habits without bank credentials.",
                                color = GoalColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // Tab Row (Active vs Completed)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TabPill(
                            label = "Active (${uiState.activeGoals.size})",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        TabPill(
                            label = "Completed (${uiState.completedGoals.size})",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                val currentList = if (selectedTab == 0) uiState.activeGoals else uiState.completedGoals

                if (currentList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = GoalColors.Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    if (selectedTab == 0) Icons.Filled.Savings else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = GoalColors.TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    if (selectedTab == 0) "No active savings goals" else "No completed goals yet",
                                    color = GoalColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (selectedTab == 0) "Tap '+ New Goal' below to set up your first goal!" else "Keep going! Completed goals will appear here.",
                                    color = GoalColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(currentList, key = { it.id }) { goal ->
                        GoalItemCard(
                            goal = goal,
                            onContribute = { goalToContribute = goal },
                            onDelete = { goalToDelete = goal },
                            onComplete = { viewModel.markCompleted(goal.id) }
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GoalColors.Coral else GoalColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else GoalColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoalItemCard(
    goal: GoalEntity,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    val savedRupees = goal.savedAmountPaise / 100
    val targetRupees = goal.targetAmountPaise / 100
    val progress = goal.progressFraction

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "item_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GoalColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        color = GoalColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "₹$savedRupees of ₹$targetRupees",
                        color = GoalColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (goal.isCompleted) GoalColors.SageBg else GoalColors.PastelBlush)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (goal.isCompleted) "Completed" else "${goal.progressPercent}%",
                        color = if (goal.isCompleted) GoalColors.Sage else GoalColors.Coral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoalColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (goal.isCompleted) GoalColors.Sage else GoalColors.Coral)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Plan suggestion detail
            goal.notes?.let { note ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoalColors.Background)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GoalColors.Coral,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        note,
                        color = GoalColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = GoalColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!goal.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onComplete,
                            colors = ButtonDefaults.textButtonColors(contentColor = GoalColors.TextSecondary)
                        ) {
                            Text("Mark Done", fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = onContribute,
                            colors = ButtonDefaults.buttonColors(containerColor = GoalColors.Coral),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Saved", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCompletionOverlay(
    goal: GoalEntity,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2).copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ConfettiBurst(modifier = Modifier.fillMaxSize(), particleCount = 100, isRunning = true)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .clickable(enabled = false, onClick = {})
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GoalColors.SageBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = GoalColors.Sage,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Goal Reached! 🎉",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GoalColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "You've saved ₹${goal.targetAmountPaise / 100} for \"${goal.title}\"!",
                fontSize = 15.sp,
                color = GoalColors.TextSecondary
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoalColors.Coral),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Awesome!", fontWeight = FontWeight.Bold)
            }
        }
    }
}
