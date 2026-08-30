package com.chirag.arthix.ui.screen.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.SecondaryButton
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.LabelCaps

/**
 * 8-step onboarding flow matching Stitch Design DNA.
 *
 * Flow: Welcome → Gestures → Notifications → Battery → System Permission
 *       → Camera & Mic → Ready → Complete
 *
 * Each step uses Stitch's full-screen layout:
 * - Step indicator (label-caps)
 * - Icon in circle (surface-icon-chip bg)
 * - Title (display-hero-mobile)
 * - Description (body-primary)
 * - Primary CTA (pill) + "Skip for now" secondary
 */
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
        modifier = Modifier
            .fillMaxSize()
            .background(ArthixTheme.colors.bg),
    ) {
        when (uiState.currentStep) {
            OnboardingStep.WELCOME -> {
                StyledOnboardingStep(
                    stepIndicator = "Welcome",
                    icon = Icons.Outlined.WavingHand,
                    title = "Welcome to\nShake & Audit",
                    description = "The fastest way to log expenses. Shake your phone " +
                            "after any payment and Arthix captures it instantly — " +
                            "no typing, no opening apps.",
                    primaryAction = "Get Started",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    showSkip = false,
                )
            }

            OnboardingStep.GESTURES -> {
                StyledOnboardingStep(
                    stepIndicator = "1 of 6",
                    icon = Icons.Outlined.Vibration,
                    title = "Shake to Log",
                    description = "Just paid for something? Give your phone a quick " +
                            "double-shake. Arthix detects the gesture and captures the " +
                            "transaction from your UPI notification automatically.\n\n" +
                            "No shake? No problem — the notification still gets logged.",
                    primaryAction = "Next",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    onSkip = { viewModel.proceedToNextStep() },
                )
            }

            OnboardingStep.NOTIFICATION_EXPLAINER -> {
                StyledOnboardingStep(
                    stepIndicator = "2 of 6",
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Access",
                    description = "Arthix reads payment notifications from GPay, PhonePe, " +
                            "and Paytm so it can log your spending automatically — nothing else.\n\n" +
                            "Android's next screen will say it can access \"all notifications\"; " +
                            "that's a system-wide permission, but Arthix only ever looks at the " +
                            "three payment apps above and immediately ignores everything else.",
                    primaryAction = "Enable Notification Access",
                    onPrimaryAction = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                        viewModel.proceedToNextStep()
                    },
                    onSkip = { viewModel.proceedToNextStep() },
                )
            }

            OnboardingStep.BATTERY_OPTIMIZATION -> {
                StyledOnboardingStep(
                    stepIndicator = "3 of 6",
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = "Background Access",
                    description = "To detect shakes and capture payment notifications reliably, " +
                            "Arthix needs to stay active in the background.\n\n" +
                            "Please exempt Arthix from battery optimization on the next screen " +
                            "so Android doesn't pause it while you're using other apps.",
                    primaryAction = "Open Battery Settings",
                    onPrimaryAction = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                        viewModel.proceedToNextStep()
                    },
                    onSkip = { viewModel.proceedToNextStep() },
                )
            }

            OnboardingStep.SYSTEM_PERMISSION -> {
                StyledOnboardingStep(
                    stepIndicator = "4 of 6",
                    icon = Icons.Outlined.Security,
                    title = "System Permissions",
                    description = "Android requires explicit consent for each type of data " +
                            "Arthix accesses. On the following screens, you'll see Android's " +
                            "standard permission dialogs.\n\n" +
                            "You can always change these later in Settings → Apps → Arthix.",
                    primaryAction = "Continue",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    onSkip = { viewModel.proceedToNextStep() },
                )
            }

            OnboardingStep.CAMERA_MIC -> {
                StyledOnboardingStep(
                    stepIndicator = "5 of 6",
                    icon = Icons.Outlined.CameraAlt,
                    title = "Camera & Microphone",
                    description = "📷 Camera is used to scan receipts — point at any bill and " +
                            "Arthix extracts the amount and vendor automatically using on-device OCR.\n\n" +
                            "🎙️ Microphone enables voice entry — just say the amount and category " +
                            "instead of typing. All voice processing happens on-device using Vosk.",
                    primaryAction = "Grant Access",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    onSkip = { viewModel.proceedToNextStep() },
                )
            }

            OnboardingStep.READY -> {
                StyledOnboardingStep(
                    stepIndicator = "6 of 6",
                    icon = Icons.Outlined.RocketLaunch,
                    title = "You're All Set!",
                    description = "Arthix is ready to track your spending. Here's what happens next:\n\n" +
                            "• Shake after a payment to capture it\n" +
                            "• UPI notifications are logged automatically\n" +
                            "• Tap + to log manually\n" +
                            "• Check Insights for spending trends",
                    primaryAction = "Start Using Arthix",
                    onPrimaryAction = { viewModel.proceedToNextStep() },
                    showSkip = false,
                )
            }

            OnboardingStep.COMPLETE -> {
                // Handled by LaunchedEffect
            }

            // Legacy step — kept for enum compat but skipped in flow
            OnboardingStep.SMS_EXPLAINER -> {
                viewModel.proceedToNextStep()
            }
        }
    }
}

@Composable
private fun StyledOnboardingStep(
    stepIndicator: String,
    icon: ImageVector,
    title: String,
    description: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onSkip: (() -> Unit)? = null,
    showSkip: Boolean = true,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Step indicator
        Text(
            text = stepIndicator,
            style = LabelCaps,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(spacing.xxl))

        // Icon in circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colors.surfaceIconChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(spacing.xl))

        // Title
        Text(
            text = title,
            style = DisplayHeroMobile,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.lg))

        // Description
        Text(
            text = description,
            style = BodyPrimary,
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

        if (showSkip && onSkip != null) {
            Spacer(Modifier.height(spacing.md))
            SecondaryButton(
                text = "Skip for now",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
