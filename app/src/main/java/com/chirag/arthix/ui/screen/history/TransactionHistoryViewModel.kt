package com.chirag.arthix.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI state for the transaction history screen.
 */
data class TransactionHistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for [TransactionHistoryScreen].
 *
 * Observes the full transaction history reactively via Room Flow.
 * Any write from anywhere in the app (Phase 2 engine, manual entry,
 * edit screen) propagates here automatically — no polling needed.
 */
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    repository: TransactionRepository,
    val sttEngine: com.chirag.arthix.voice.VoskSttEngine,
) : ViewModel() {

    val uiState: StateFlow<TransactionHistoryUiState> = repository
        .observeHistory()
        .map { transactions ->
            TransactionHistoryUiState(
                transactions = transactions,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionHistoryUiState(),
        )
}
