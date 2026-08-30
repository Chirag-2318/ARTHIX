package com.chirag.arthix.ui.screen.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        val suggestedGroup: SuggestedSplitGroup?
    ) : SplitTriggerState()
}

@HiltViewModel
class SplitTriggerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitGroupSuggestionHeuristic: SplitGroupSuggestionHeuristic
) : ViewModel() {

    private val _state = MutableStateFlow<SplitTriggerState>(SplitTriggerState.Idle)
    val state: StateFlow<SplitTriggerState> = _state

    init {
        viewModelScope.launch {
            transactionRepository.events.collect { event ->
                if (event is TransactionEvent.TransactionCommitted) {
                    val txn = transactionRepository.getById(event.transactionId)
                    if (txn != null) {
                        // EC-41: fetch suggestion
                        val suggestion = splitGroupSuggestionHeuristic.suggestGroup(
                            category = txn.category,
                            timestampMs = txn.timestamp
                        )
                        _state.value = SplitTriggerState.Prompting(txn.id, suggestion)
                    }
                }
            }
        }
    }

    fun triggerManualPrompt(transactionId: Long) {
        viewModelScope.launch {
            val txn = transactionRepository.getById(transactionId)
            if (txn != null) {
                val suggestion = splitGroupSuggestionHeuristic.suggestGroup(
                    category = txn.category,
                    timestampMs = txn.timestamp
                )
                _state.value = SplitTriggerState.Prompting(txn.id, suggestion)
            }
        }
    }

    fun dismissPrompt() {
        _state.value = SplitTriggerState.Idle
    }
}
