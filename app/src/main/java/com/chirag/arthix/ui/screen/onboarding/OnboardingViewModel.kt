package com.chirag.arthix.ui.screen.onboarding

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.NOTIFICATION_EXPLAINER,
)

enum class OnboardingStep {
    NOTIFICATION_EXPLAINER,
    SMS_EXPLAINER,
    BATTERY_OPTIMIZATION,
    COMPLETE,
}

/**
 * ViewModel for the onboarding flow (PRD §9).
 *
 * Three-step flow:
 * 1. Notification-listener permission explainer (EC-58)
 * 2. SMS permission explainer
 * 3. Battery-optimization whitelist request
 *
 * Both are skippable but revisitable from settings.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun proceedToNextStep() {
        _uiState.update { current ->
            when (current.currentStep) {
                OnboardingStep.NOTIFICATION_EXPLAINER ->
                    current.copy(currentStep = OnboardingStep.SMS_EXPLAINER)
                OnboardingStep.SMS_EXPLAINER ->
                    current.copy(currentStep = OnboardingStep.BATTERY_OPTIMIZATION)
                OnboardingStep.BATTERY_OPTIMIZATION ->
                    current.copy(currentStep = OnboardingStep.COMPLETE)
                OnboardingStep.COMPLETE -> current
            }
        }
    }

    fun skip() {
        _uiState.update { it.copy(currentStep = OnboardingStep.COMPLETE) }
    }
}
