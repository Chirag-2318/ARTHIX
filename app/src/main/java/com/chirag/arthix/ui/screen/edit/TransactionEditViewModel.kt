package com.chirag.arthix.ui.screen.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.ui.navigation.ArthixRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the transaction edit screen.
 */
data class TransactionEditUiState(
    val transaction: TransactionEntity? = null,
    val hasSplit: Boolean = false,
    val isSplitRecalculated: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val saveComplete: Boolean = false,
    val deleteComplete: Boolean = false,
)

/**
 * ViewModel for [TransactionEditScreen] (PRD §6 / EC-52).
 *
 * Handles:
 * - Loading a single transaction by ID
 * - Saving edits with atomic status/confidence flag transition
 * - Delete with confirmation dialog guard
 *
 * On save: if the record was auto_resolved or needs_review, the save
 * transitions confidence_flag → CLEAN and status → CONFIRMED atomically
 * (§6's non-negotiable requirement).
 */
@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    private val splitRepository: SplitRepository,
) : ViewModel() {

    private val txnId: Long = checkNotNull(savedStateHandle[ArthixRoute.Edit.ARG_TXN_ID])

    private val _uiState = MutableStateFlow(TransactionEditUiState())
    val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val txn = repository.getById(txnId)
            val splits = splitRepository.getSplitsForTransaction(txnId)
            val hasSplit = splits.isNotEmpty()
            val isSplitRecalculated = splits.firstOrNull()?.first?.recalculatedFlag == true
            
            _uiState.update { 
                it.copy(
                    transaction = txn, 
                    hasSplit = hasSplit,
                    isSplitRecalculated = isSplitRecalculated,
                    isLoading = false 
                ) 
            }
        }
    }

    /**
     * Save an edited transaction.
     *
     * PRD §6 atomic transition: on any manual edit, if the record's
     * confidence_flag is not CLEAN, force it to CLEAN and status to CONFIRMED.
     */
    fun save(
        amountPaise: Long?,
        payee: String?,
        category: String?,
    ) {
        val current = _uiState.value.transaction ?: return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val amountChanged = current.amountPaise != null && amountPaise != null && current.amountPaise != amountPaise
            
            val updated = current.copy(
                amountPaise = amountPaise,
                payee = payee,
                category = category,
                confidenceFlag = ConfidenceFlag.CLEAN,
                status = if (amountPaise != null && category != null)
                    TransactionStatus.CONFIRMED
                else
                    current.status,
            )
            repository.update(updated)
            
            // EC-38 Live Recalculation
            if (amountChanged) {
                val splits = splitRepository.getSplitsForTransaction(current.id)
                splits.firstOrNull()?.let { (record, participants) ->
                    val newShares = com.chirag.arthix.domain.split.recalculateProportional(
                        oldShares = participants.map { 
                            com.chirag.arthix.domain.split.ParticipantShare(it.participantId, it.sharePaise) 
                        },
                        oldTotalPaise = current.amountPaise!!,
                        newTotalPaise = amountPaise!!,
                    )
                    
                    val updatedParticipants = participants.map { p ->
                        val newShare = newShares.find { it.participantId == p.participantId }?.sharePaise ?: p.sharePaise
                        p.copy(
                            previousSharePaise = p.sharePaise,
                            sharePaise = newShare
                        )
                    }
                    val updatedRecord = record.copy(recalculatedFlag = true)
                    splitRepository.updateSplit(updatedRecord, updatedParticipants)
                }
            }
            
            _uiState.update { it.copy(isSaving = false, saveComplete = true) }
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    /**
     * Delete (discard) the transaction after user confirms the dialog.
     */
    fun confirmDelete() {
        viewModelScope.launch {
            repository.discard(txnId)
            _uiState.update { it.copy(showDeleteConfirmation = false, deleteComplete = true) }
        }
    }
}
