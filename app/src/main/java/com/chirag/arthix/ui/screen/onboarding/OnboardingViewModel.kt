package com.chirag.arthix.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Onboarding ViewModel — drives the 8-step onboarding flow.
 *
 * Steps: WELCOME → GESTURES → NOTIFICATION_EXPLAINER → BATTERY_OPTIMIZATION
 *        → SYSTEM_PERMISSION → CAMERA_MIC → READY → COMPLETE
 */

enum class OnboardingStep {
    WELCOME,
    GESTURES,
    NOTIFICATION_EXPLAINER,
    SMS_EXPLAINER,          // Kept for backward compat, skipped in flow
    BATTERY_OPTIMIZATION,
    SYSTEM_PERMISSION,
    CAMERA_MIC,
    READY,
    COMPLETE,
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Ordered flow — SMS_EXPLAINER is skipped
    private val stepOrder = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.GESTURES,
        OnboardingStep.NOTIFICATION_EXPLAINER,
        OnboardingStep.BATTERY_OPTIMIZATION,
        OnboardingStep.SYSTEM_PERMISSION,
        OnboardingStep.CAMERA_MIC,
        OnboardingStep.READY,
        OnboardingStep.COMPLETE,
    )

    fun proceedToNextStep() {
        val currentIndex = stepOrder.indexOf(_uiState.value.currentStep)
        val nextIndex = (currentIndex + 1).coerceAtMost(stepOrder.lastIndex)
        _uiState.value = _uiState.value.copy(currentStep = stepOrder[nextIndex])
    }
}
