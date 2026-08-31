package com.chirag.arthix.ui.screen.split

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.AmountLock
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.ui.navigation.ArthixRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SplitParticipant(
    val id: String,
    val name: String,
    val avatarInitial: String,
    val avatarTint: Color,
    val sharePaise: Long,
    val isAppUser: Boolean,
    val isPaid: Boolean = false
)

enum class SplitMode {
    EQUALLY, MANUALLY
}

data class SplitBillUiState(
    val transactionId: Long = 0L,
    val isNewTransaction: Boolean = false,
    val payee: String = "",
    val totalAmountPaise: Long = 0L,
    val participants: List<SplitParticipant> = emptyList(),
    val splitMode: SplitMode = SplitMode.EQUALLY,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SplitBillViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val splitRepository: SplitRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val txnId: Long = savedStateHandle[ArthixRoute.SplitBill.ARG_TXN_ID] ?: 0L

    private val _uiState = MutableStateFlow(SplitBillUiState(
        transactionId = txnId,
        isNewTransaction = txnId == 0L
    ))
    val uiState: StateFlow<SplitBillUiState> = _uiState

    init {
        if (txnId != 0L) {
            viewModelScope.launch {
                val txn = transactionRepository.getById(txnId)
                if (txn != null) {
                    val existingSplits = splitRepository.getAllSplits().filter { it.first.transactionId == txnId }
                    if (existingSplits.isNotEmpty()) {
                        // Load existing split
                        val (record, parts) = existingSplits.first()
                        val uiParts = parts.map {
                            SplitParticipant(
                                id = it.participantId,
                                name = it.displayName,
                                avatarInitial = it.displayName.take(1).uppercase(),
                                avatarTint = getColorForName(it.displayName),
                                sharePaise = it.sharePaise,
                                isAppUser = it.isAppUser,
                                isPaid = it.isPaid
                            )
                        }
                        _uiState.update {
                            it.copy(
                                payee = txn.payee ?: txn.category ?: "Unknown",
                                totalAmountPaise = txn.amountPaise ?: 0L,
                                participants = uiParts,
                                splitMode = SplitMode.MANUALLY // Default to manual when loading existing to avoid overriding their specific shares if they had a remainder
                            )
                        }
                    } else {
                        // New split on existing transaction
                        val appUser = SplitParticipant(
                            id = "app_user",
                            name = "You",
                            avatarInitial = "Y",
                            avatarTint = Color(0xFFE8355A),
                            sharePaise = txn.amountPaise ?: 0L,
                            isAppUser = true
                        )
                        _uiState.update {
                            it.copy(
                                payee = txn.payee ?: txn.category ?: "Unknown",
                                totalAmountPaise = txn.amountPaise ?: 0L,
                                participants = listOf(appUser)
                            )
                        }
                    }
                }
            }
        } else {
            val appUser = SplitParticipant(
                id = "app_user",
                name = "You",
                avatarInitial = "Y",
                avatarTint = Color(0xFFE8355A),
                sharePaise = 0L,
                isAppUser = true
            )
            _uiState.update { it.copy(participants = listOf(appUser)) }
        }
    }

    private fun getColorForName(name: String): Color {
        val colors = listOf(Color(0xFFE8355A), Color(0xFFF5A623), Color(0xFF4C8CFF), Color(0xFFB56BFF), Color(0xFF34D399))
        return colors[name.hashCode().absoluteValue % colors.size]
    }

    private val Int.absoluteValue: Int get() = if (this < 0) -this else this

    fun updatePayee(payee: String) {
        _uiState.update { it.copy(payee = payee) }
    }

    fun updateAmount(amount: Long) {
        _uiState.update { state ->
            val newState = state.copy(totalAmountPaise = amount)
            if (state.splitMode == SplitMode.EQUALLY) {
                newState.copy(participants = recalculateEvenly(state.participants, amount))
            } else {
                newState
            }
        }
    }

    fun addParticipant(name: String) {
        if (name.isBlank()) return
        _uiState.update { state ->
            val newPart = SplitParticipant(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                avatarInitial = name.trim().take(1).uppercase(),
                avatarTint = getColorForName(name),
                sharePaise = 0L,
                isAppUser = false
            )
            val newParts = state.participants + newPart
            if (state.splitMode == SplitMode.EQUALLY) {
                state.copy(participants = recalculateEvenly(newParts, state.totalAmountPaise))
            } else {
                state.copy(participants = newParts)
            }
        }
    }

    fun removeParticipant(id: String) {
        _uiState.update { state ->
            val newParts = state.participants.filter { it.id != id }
            if (newParts.isEmpty()) return@update state
            if (state.splitMode == SplitMode.EQUALLY) {
                state.copy(participants = recalculateEvenly(newParts, state.totalAmountPaise))
            } else {
                state.copy(participants = newParts)
            }
        }
    }

    fun setSplitMode(mode: SplitMode) {
        _uiState.update { state ->
            if (mode == SplitMode.EQUALLY && state.participants.isNotEmpty()) {
                state.copy(splitMode = mode, participants = recalculateEvenly(state.participants, state.totalAmountPaise))
            } else {
                state.copy(splitMode = mode)
            }
        }
    }

    fun updateShare(participantId: String, newSharePaise: Long) {
        _uiState.update { state ->
            if (state.splitMode == SplitMode.EQUALLY) return@update state
            
            val newParts = state.participants.map { p ->
                if (p.id == participantId) p.copy(sharePaise = newSharePaise) else p
            }
            state.copy(participants = newParts)
        }
    }

    fun togglePaidStatus(participantId: String) {
        _uiState.update { state ->
            val newParts = state.participants.map { p ->
                if (p.id == participantId) p.copy(isPaid = !p.isPaid) else p
            }
            state.copy(participants = newParts)
        }
    }

    private fun recalculateEvenly(participants: List<SplitParticipant>, totalPaise: Long): List<SplitParticipant> {
        if (participants.isEmpty()) return participants
        val evenShare = totalPaise / participants.size
        val remainder = totalPaise - (evenShare * participants.size)
        return participants.mapIndexed { i, p ->
            p.copy(sharePaise = evenShare + if (i == 0) remainder else 0)
        }
    }

    fun confirmSplit() {
        val state = _uiState.value
        val sum = state.participants.sumOf { it.sharePaise }
        if (sum != state.totalAmountPaise) {
            _uiState.update { it.copy(errorMessage = "Shares must add up to the total amount.") }
            return
        }

        viewModelScope.launch {
            var targetTxnId = state.transactionId
            if (state.isNewTransaction) {
                val newTxn = TransactionEntity(
                    source = com.chirag.arthix.data.model.CaptureSource.MANUAL,
                    status = TransactionStatus.CONFIRMED,
                    direction = Direction.OUTFLOW,
                    amountPaise = state.totalAmountPaise,
                    payee = state.payee,
                    category = null,
                    timestamp = System.currentTimeMillis(),
                    sourceCaptureId = null,
                    sourceNotificationId = null,
                    confidenceFlag = com.chirag.arthix.data.model.ConfidenceFlag.CLEAN,
                    createdAt = System.currentTimeMillis()
                )
                targetTxnId = transactionRepository.commit(newTxn)
            }

            val record = SplitRecordEntity(
                transactionId = targetTxnId,
                confirmedVia = SplitConfirmedVia.TAP,
                amountLock = AmountLock.LIVE,
                lockedAmountPaise = null,
                createdAt = System.currentTimeMillis()
            )
            
            val participants = state.participants.map { p ->
                SplitParticipantEntity(
                    splitRecordId = 0L, // will be set by repo
                    participantId = p.id,
                    displayName = p.name,
                    contactId = null,
                    isAppUser = p.isAppUser,
                    sharePaise = p.sharePaise,
                    isPaid = p.isPaid
                )
            }
            
            splitRepository.createSplit(record, participants)
            _uiState.update { it.copy(saveComplete = true) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
