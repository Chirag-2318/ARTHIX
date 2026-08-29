package com.chirag.arthix.ui.screen.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.SecondaryButton
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Display

/**
 * Onboarding screens (PRD §6.9, EC-58/60).
 *
 * Each step shows an in-app explainer BEFORE the system permission
 * dialog, explaining *why* in the app's own words before Android's
 * alarming system copy appears.
 *
 * Three-step flow:
 * 1. Notification-listener permission (EC-58)
 * 2. SMS permission
 * 3. Battery-optimization whitelist
 *
 * All steps are skippable and revisitable from settings.
 *
 * Styled with design system: dark bg, accent icon, pill buttons,
 * role-named typography.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = ArthixTheme.colors

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.COMPLETE) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (uiState.currentStep) {
                OnboardingStep.NOTIFICATION_EXPLAINER -> {
                    OnboardingStep(
                        icon = Icons.Outlined.Notifications,
                        title = "Notification Access",
                        description = "Arthix reads payment notifications from GPay, PhonePe, " +
                                "and Paytm so it can log your spending automatically — nothing else.\n\n" +
                                "Android's next screen will say it can access \"all notifications\"; " +
                                "that's a system-wide permission, but Arthix only ever looks at the " +
                                "three payment apps above and immediately ignores everything else.",
                        primaryAction = "Enable Notification Access",
                        onPrimaryAction = {
                            android.util.Log.d("Onboarding", "step=NOTIFICATION_EXPLAINER completed (enabled)")
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                            viewModel.proceedToNextStep()
                        },
                        onSkip = {
                            android.util.Log.d("Onboarding", "step=NOTIFICATION_EXPLAINER completed (skipped)")
                            viewModel.proceedToNextStep()
                        },
                        stepIndicator = "1 of 3",
                    )
                }

                OnboardingStep.SMS_EXPLAINER -> {
                    OnboardingStep(
                        icon = Icons.Outlined.Sms,
                        title = "SMS Access",
                        description = "To track your expenses, Arthix needs to read bank SMS messages. " +
                                "It only processes messages from trusted bank sender IDs and never uploads " +
                                "them to any server. All processing happens locally on your device.",
                        primaryAction = "Grant SMS Permission",
                        onPrimaryAction = {
                            android.util.Log.d("Onboarding", "step=SMS_EXPLAINER completed (enabled)")
                            onRequestSmsPermission()
                            viewModel.proceedToNextStep()
                        },
                        onSkip = {
                            android.util.Log.d("Onboarding", "step=SMS_EXPLAINER completed (skipped)")
                            viewModel.proceedToNextStep()
                        },
                        stepIndicator = "2 of 3",
                    )
                }

                OnboardingStep.BATTERY_OPTIMIZATION -> {
                    OnboardingStep(
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = "Background Access",
                        description = "To detect shakes and capture payment notifications reliably, " +
                                "Arthix needs to stay active in the background.\n\n" +
                                "Please exempt Arthix from battery optimization on the next screen " +
                                "so Android doesn't pause it while you're using other apps.",
                        primaryAction = "Open Battery Settings",
                        onPrimaryAction = {
                            android.util.Log.d("Onboarding", "step=BATTERY_OPTIMIZATION completed (enabled)")
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                            viewModel.proceedToNextStep()
                        },
                        onSkip = {
                            android.util.Log.d("Onboarding", "step=BATTERY_OPTIMIZATION completed (skipped)")
                            viewModel.proceedToNextStep()
                        },
                        stepIndicator = "3 of 3",
                    )
                }

                OnboardingStep.COMPLETE -> {
                    // Handled by LaunchedEffect
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onSkip: () -> Unit,
    stepIndicator: String,
) {
    val colors = ArthixTheme.colors

    // Step indicator
    Text(
        text = stepIndicator,
        style = Caption,
        color = colors.textSecondary,
    )

    Spacer(Modifier.height(32.dp))

    // Icon
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(64.dp),
    )

    Spacer(Modifier.height(24.dp))

    // Title
    Text(
        text = title,
        style = Display,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(16.dp))

    // Description
    Text(
        text = description,
        style = Body,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(48.dp))

    // Primary action
    PrimaryButton(
        text = primaryAction,
        onClick = onPrimaryAction,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))

    // Skip
    SecondaryButton(
        text = "Skip for now",
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth(),
    )
}
