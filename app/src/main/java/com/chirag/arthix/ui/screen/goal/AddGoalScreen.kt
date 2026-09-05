package com.chirag.arthix.ui.screen.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.model.GoalPlanType
import kotlinx.coroutines.delay

private object AddGoalColors {
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
    val Border = Color(0xFFF0EDE8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(
    onBack: () -> Unit = {},
    onGoalCreated: () -> Unit = {},
    viewModel: GoalListViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    var title by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var initialSavedStr by remember { mutableStateOf("") }

    val planPreview by viewModel.planPreview.collectAsState()

    // Debounce amount changes to update plan preview
    LaunchedEffect(targetAmountStr) {
        val amount = targetAmountStr.toDoubleOrNull() ?: 0.0
        if (amount > 0) {
            delay(250)
            viewModel.updatePlanPreview((amount * 100).toLong())
        } else {
            viewModel.clearPlanPreview()
        }
    }

    val quickIdeas = listOf("Mouse", "AirPods", "Sneakers", "Weekend Trip", "Desk Setup", "Emergency Fund")

    Scaffold(
        containerColor = AddGoalColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AddGoalColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
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
                        tint = AddGoalColors.TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Savings Goal",
                    color = AddGoalColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subtitle
            Text(
                "Set an item or milestone you're saving for. ARTHIX analyzes your local spending patterns to suggest a realistic, zero-friction plan.",
                color = AddGoalColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(24.dp))

            // 1. Goal Title Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AddGoalColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "WHAT ARE YOU SAVING FOR?",
                        color = AddGoalColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Mechanical Keyboard, Goa Trip", color = AddGoalColors.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AddGoalColors.Coral,
                            unfocusedBorderColor = AddGoalColors.Border,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AddGoalColors.TextPrimary,
                            unfocusedTextColor = AddGoalColors.TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(Modifier.height(14.dp))

                    // Quick suggestion chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickIdeas) { idea ->
                            val isSelected = title.equals(idea, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AddGoalColors.Coral else AddGoalColors.Background)
                                    .clickable { title = idea }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    idea,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else AddGoalColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. Target Amount Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AddGoalColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "TARGET AMOUNT (₹)",
                        color = AddGoalColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = targetAmountStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                targetAmountStr = input
                            }
                        },
                        placeholder = { Text("2000", color = AddGoalColors.TextMuted) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = AddGoalColors.TextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AddGoalColors.Coral,
                            unfocusedBorderColor = AddGoalColors.Border,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AddGoalColors.TextPrimary,
                            unfocusedTextColor = AddGoalColors.TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "ALREADY SAVED SO FAR (OPTIONAL)",
                        color = AddGoalColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = initialSavedStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                initialSavedStr = input
                            }
                        },
                        placeholder = { Text("0", color = AddGoalColors.TextMuted) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = AddGoalColors.TextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AddGoalColors.Coral,
                            unfocusedBorderColor = AddGoalColors.Border,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = AddGoalColors.TextPrimary,
                            unfocusedTextColor = AddGoalColors.TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. Live AI Plan Preview Card
            AnimatedVisibility(
                visible = planPreview != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                planPreview?.let { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AddGoalColors.Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(AddGoalColors.PastelBlush),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = AddGoalColors.Coral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Tailored On-Device Plan",
                                        color = AddGoalColors.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AddGoalColors.SageBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "~${plan.estimatedDaysToTarget} days",
                                        color = AddGoalColors.Sage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                plan.recommendationHeadline,
                                color = AddGoalColors.Coral,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                plan.recommendationDetail,
                                color = AddGoalColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save Action Button
            val targetVal = targetAmountStr.toDoubleOrNull() ?: 0.0
            val isEnabled = title.isNotBlank() && targetVal > 0.0

            Button(
                onClick = {
                    val targetPaise = (targetVal * 100).toLong()
                    val savedPaise = (initialSavedStr.toDoubleOrNull() ?: 0.0 * 100).toLong()
                    viewModel.createGoal(
                        title = title,
                        targetAmountPaise = targetPaise,
                        initialSavedPaise = savedPaise,
                        onSuccess = onGoalCreated
                    )
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AddGoalColors.Coral,
                    disabledContainerColor = AddGoalColors.Border
                )
            ) {
                Icon(Icons.Filled.Savings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create Savings Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) Color.White else AddGoalColors.TextMuted
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
