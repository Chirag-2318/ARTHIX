package com.chirag.arthix.ui.screen.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.R
import com.chirag.arthix.ui.theme.DisplayHeroMobile

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.COMPLETE) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState.currentStep) {
            OnboardingStep.GESTURES -> {
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "Log an expense in one shake, no typing needed",
                    primaryAction = "Enable Shake to Log",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    backgroundImageRes = R.drawable.n1
                )
            }

            OnboardingStep.NOTIFICATION_EXPLAINER -> {
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "Never miss an expense or a split reminder",
                    primaryAction = "Turn on Notifications",
                    onPrimaryAction = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                        viewModel.proceedToNextStep()
                    },
                    backgroundImageRes = R.drawable.n2
                )
            }

            OnboardingStep.BATTERY_OPTIMIZATION -> {
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "Keep tracking expenses even when Arthix is in the background",
                    primaryAction = "Allow Background Access",
                    onPrimaryAction = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                        viewModel.proceedToNextStep()
                    },
                    backgroundImageRes = R.drawable.n3
                )
            }

            OnboardingStep.SYSTEM_PERMISSION -> {
                val hasOverlay = Settings.canDrawOverlays(context)
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "Log expenses instantly, without leaving your current app",
                    primaryAction = if (hasOverlay) "Continue" else "Allow Display Over Apps",
                    onPrimaryAction = {
                        if (!hasOverlay) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        }
                        viewModel.proceedToNextStep()
                    },
                    backgroundImageRes = R.drawable.n4
                )
            }

            OnboardingStep.CAMERA_MIC -> {
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "Scan receipts and log expenses just by talking",
                    primaryAction = "Enable Camera & Mic",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    backgroundImageRes = R.drawable.n5
                )
            }

            OnboardingStep.READY -> {
                StyledOnboardingStep(
                    step = uiState.currentStep,
                    headline = "You're all set — Arthix is ready to work for you",
                    primaryAction = "Get Started",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    backgroundImageRes = R.drawable.n6
                )
            }

            else -> {
                // Should not reach here in new flow
                LaunchedEffect(Unit) { viewModel.proceedToNextStep() }
            }
        }

        // Progress Indicator at top
        val currentStepIndex = when (uiState.currentStep) {
            OnboardingStep.GESTURES -> 0
            OnboardingStep.NOTIFICATION_EXPLAINER -> 1
            OnboardingStep.BATTERY_OPTIMIZATION -> 2
            OnboardingStep.SYSTEM_PERMISSION -> 3
            OnboardingStep.CAMERA_MIC -> 4
            OnboardingStep.READY -> 5
            else -> 0
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0 until 6) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(4.dp)
                        .width(if (i == currentStepIndex) 24.dp else 12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i == currentStepIndex) BrandCoral else Color.Black.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Composable
private fun StyledOnboardingStep(
    step: OnboardingStep,
    headline: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    backgroundImageRes: Int
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-bleed background image
        Image(
            painter = painterResource(id = backgroundImageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Bottom Content Area (~45% of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.85f),
                            Color.White
                        ),
                        startY = 0f
                    )
                )
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                
                Text(
                    text = headline,
                    style = DisplayHeroMobile,
                    fontWeight = FontWeight.Bold,
                    color = TextNearBlack,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Primary Button
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1C))
                ) {
                    Text(
                        text = primaryAction,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
