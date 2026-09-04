package com.chirag.arthix.ui.screen.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionEvent
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.report.split.SplitGroupSuggestionHeuristic
import com.chirag.arthix.report.split.SuggestedSplitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplitTriggerState {
    object Idle : SplitTriggerState()
    data class Prompting(
        val transactionId: Long,
        val suggestedGroup: SuggestedSplitGroup?,
        val initialParticipantNames: List<String> = emptyList()
    ) : SplitTriggerState()
}

@HiltViewModel
class SplitTriggerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
    private val splitGroupSuggestionHeuristic: SplitGroupSuggestionHeuristic
) : ViewModel() {

    private val _state = MutableStateFlow<SplitTriggerState>(SplitTriggerState.Idle)
    val state: StateFlow<SplitTriggerState> = _state

    init {
        // Explicitly removed: Do not automatically prompt for splits on every committed transaction.
        // The split bottom sheet should only open when explicitly requested.
    }

    fun triggerManualPrompt(transactionId: Long, initialParticipantNames: List<String> = emptyList()) {
        viewModelScope.launch {
            val txn = transactionRepository.getById(transactionId)
            if (txn != null) {
                val suggestion = splitGroupSuggestionHeuristic.suggestGroup(
                    category = txn.category,
                    timestampMs = txn.timestamp
                )
                _state.value = SplitTriggerState.Prompting(txn.id, suggestion, initialParticipantNames)
            }
        }
    }

    fun dismissPrompt() {
        _state.value = SplitTriggerState.Idle
    }
}
