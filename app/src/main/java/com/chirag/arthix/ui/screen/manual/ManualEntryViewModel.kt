package com.chirag.arthix.ui.screen.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualEntryUiState(
    val amount: String = "",
    val payee: String = "",
    val selectedCategory: String? = null,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
)

/**
 * ViewModel for the manual fallback entry flow (PRD §5 / FR-5 / EC-35).
 *
 * Creates a new Transaction with source = MANUAL, status = CONFIRMED
 * (amount and category are both present at entry time, so nothing is pending).
 */
@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    val sttEngine: com.chirag.arthix.voice.VoskSttEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    /**
     * Pre-fill the form from Phase 4's degraded paths (§15 handoff).
     */
    fun openWithPrefill(prefill: ManualEntryPrefill?) {
        if (prefill == null) return
        _uiState.update {
            it.copy(
                amount = prefill.amount ?: "",
                payee = prefill.payee ?: "",
                selectedCategory = prefill.category,
            )
        }
    }

    fun updateAmount(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun updatePayee(payee: String) {
        _uiState.update { it.copy(payee = payee) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun save() {
        val state = _uiState.value
        val paise = state.amount.toDoubleOrNull()?.let { (it * 100).toLong() } ?: return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            repository.commit(
                TransactionEntity(
                    amountPaise = paise,
                    payee = state.payee.ifBlank { null },
                    category = state.selectedCategory,
                    timestamp = System.currentTimeMillis(),
                    direction = Direction.OUTFLOW,
                    source = CaptureSource.MANUAL,
                    status = TransactionStatus.CONFIRMED,
                    sourceCaptureId = null,
                    sourceNotificationId = null,
                    confidenceFlag = ConfidenceFlag.CLEAN,
                    createdAt = System.currentTimeMillis(),
                )
            )
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }
}
