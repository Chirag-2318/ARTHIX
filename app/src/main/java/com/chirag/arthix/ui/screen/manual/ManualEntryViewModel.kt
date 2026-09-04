package com.chirag.arthix.ui.screen.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.voice.WhisperSttEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ManualEntryUiState(
    val amount: String = "",
    val payee: String = "",
    val selectedCategory: String? = null,
    val direction: Direction = Direction.OUTFLOW,
    val splitNames: List<String> = emptyList(),
    val wantsToSplit: Boolean = false,
    val isSaving: Boolean = false,
    val savedTransactionId: Long? = null,
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
    private val splitRepository: SplitRepository,
    val sttEngine: WhisperSttEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    /**
     * Pre-fill or reset the form.
     */
    fun reset(prefill: ManualEntryPrefill? = null) {
        val direction = prefill?.direction ?: Direction.OUTFLOW
        val payee = prefill?.payee ?: ""
        val category = prefill?.category
            ?: if (payee.isNotBlank()) com.chirag.arthix.domain.category.TransactionCategoryAiClassifier.classify(payee, null, direction)
               else if (direction == Direction.OUTFLOW) "Food" else "Salary"
        val rawAmount = prefill?.amount ?: ""
        val cleanAmount = if (rawAmount.endsWith(".00")) rawAmount.removeSuffix(".00") else rawAmount

        _uiState.value = ManualEntryUiState(
            amount = cleanAmount,
            payee = payee,
            selectedCategory = category,
            direction = direction,
            splitNames = prefill?.splitNames ?: emptyList(),
            wantsToSplit = false,
            isSaving = false,
            savedTransactionId = null,
        )
    }

    fun openWithPrefill(prefill: ManualEntryPrefill?) {
        reset(prefill)
    }

    fun onSaveCompleteHandled() {
        _uiState.update { it.copy(savedTransactionId = null) }
    }

    fun updateAmount(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun updatePayee(payee: String) {
        _uiState.update { current ->
            val autoCat = if (current.selectedCategory == null) {
                com.chirag.arthix.domain.category.TransactionCategoryAiClassifier.classify(payee, null, current.direction)
            } else {
                current.selectedCategory
            }
            current.copy(payee = payee, selectedCategory = autoCat)
        }
    }

    fun updateDirection(direction: Direction) {
        _uiState.update { it.copy(direction = direction) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSplitNames(names: List<String>) {
        _uiState.update { it.copy(splitNames = names) }
    }

    fun clearSplitNames() {
        _uiState.update { it.copy(splitNames = emptyList()) }
    }

    fun updateWantsToSplit(wantsToSplit: Boolean) {
        _uiState.update { it.copy(wantsToSplit = wantsToSplit) }
    }

    fun save() {
        val state = _uiState.value
        val rawAmount = state.amount.trim().replace(",", ".")
        val paise = rawAmount.toDoubleOrNull()?.let { (it * 100).toLong() }
            ?: rawAmount.filter { it.isDigit() }.toLongOrNull()?.times(100)
            ?: return

        if (paise <= 0L) return

        _uiState.update { it.copy(isSaving = true) }

        val resolvedCategory = state.selectedCategory 
            ?: if (state.direction == Direction.OUTFLOW) "Food" else "Salary"

        val resolvedPayee = state.payee.ifBlank {
            resolvedCategory
        }

        viewModelScope.launch {
            try {
                val txnId = repository.commit(
                    TransactionEntity(
                        amountPaise = paise,
                        payee = resolvedPayee,
                        category = resolvedCategory,
                        timestamp = System.currentTimeMillis(),
                        direction = state.direction,
                        source = CaptureSource.MANUAL,
                        status = TransactionStatus.CONFIRMED,
                        sourceCaptureId = null,
                        sourceNotificationId = null,
                        confidenceFlag = ConfidenceFlag.CLEAN,
                        createdAt = System.currentTimeMillis(),
                    )
                )

                // Split logic removed: We no longer auto-save the split.
                // Instead, we just surface the savedTransactionId, and the UI will
                // trigger the SplitBottomSheet if the user wanted to split.

                _uiState.update { it.copy(isSaving = false, savedTransactionId = txnId) }
            } catch (e: Exception) {
                android.util.Log.e("ManualEntry", "Failed to save transaction", e)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
