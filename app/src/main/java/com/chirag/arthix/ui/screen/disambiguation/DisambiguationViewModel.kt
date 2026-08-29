package com.chirag.arthix.ui.screen.disambiguation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.notification.DisambiguationPrompt
import com.chirag.arthix.notification.ReconciliationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisambiguationViewModel @Inject constructor(
    private val reconciliationEngine: ReconciliationEngine
) : ViewModel() {

    private val _currentPrompt = MutableStateFlow<DisambiguationPrompt?>(null)
    val currentPrompt: StateFlow<DisambiguationPrompt?> = _currentPrompt.asStateFlow()

    private val _selectedCandidateId = MutableStateFlow<String?>(null)
    val selectedCandidateId: StateFlow<String?> = _selectedCandidateId.asStateFlow()

    private val _timeLeftMs = MutableStateFlow(0L)
    val timeLeftMs: StateFlow<Long> = _timeLeftMs.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            reconciliationEngine.disambiguationPrompts.collect { prompt ->
                if (_currentPrompt.value == null) {
                    _currentPrompt.value = prompt
                    // Pre-select the best candidate (first one)
                    _selectedCandidateId.value = prompt.candidates.firstOrNull()?.captureId
                    startCountdown(prompt.timeoutMs)
                }
            }
        }
    }

    private fun startCountdown(timeoutMs: Long) {
        countdownJob?.cancel()
        _timeLeftMs.value = timeoutMs
        countdownJob = viewModelScope.launch {
            var remaining = timeoutMs
            while (remaining > 0) {
                delay(100)
                remaining -= 100
                _timeLeftMs.value = remaining
            }
            // Timeout reached, clear UI. Engine handles fallback autonomously (PRD §7.5)
            _currentPrompt.value = null
        }
    }

    fun selectCandidate(id: String) {
        _selectedCandidateId.value = id
    }

    fun confirmSelection() {
        val prompt = _currentPrompt.value ?: return
        val selectedId = _selectedCandidateId.value ?: return
        
        countdownJob?.cancel()
        _currentPrompt.value = null
        
        viewModelScope.launch {
            reconciliationEngine.resolveDisambiguation(prompt.notificationId, selectedId)
        }
    }

    fun ignore() {
        // User swiped down / dismissed. Engine will timeout and auto-resolve.
        countdownJob?.cancel()
        _currentPrompt.value = null
    }
}
