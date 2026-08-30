package com.chirag.arthix.ui.screen.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.domain.split.ParticipantShare
import com.chirag.arthix.domain.split.SplitMode
import com.chirag.arthix.domain.split.computeSplitShares
import com.chirag.arthix.report.split.SuggestedSplitGroup
import com.chirag.arthix.voice.VoskSttEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SplitParticipantUiModel(
    val participantId: String,
    val displayName: String,
    val contactId: String?,
    val isAppUser: Boolean,
    var sharePaise: Long = 0L,
    var customOverridePaise: Long? = null,
    var customOverrideString: String = ""
)

sealed class SplitEditState {
    object Loading : SplitEditState()
    data class Active(
        val transactionId: Long,
        val totalAmountPaise: Long,
        val participants: List<SplitParticipantUiModel>,
        val isCustomMode: Boolean,
        val remainderToAllocate: Long
    ) : SplitEditState()
    object Done : SplitEditState()
}

@HiltViewModel
class SplitEditViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
    val sttEngine: VoskSttEngine
) : ViewModel() {

    private val _state = MutableStateFlow<SplitEditState>(SplitEditState.Loading)
    val state: StateFlow<SplitEditState> = _state

    private var currentTxnId: Long = 0L
    private var totalAmount: Long = 0L
    private var isCustom: Boolean = false
    private val currentParticipants = mutableListOf<SplitParticipantUiModel>()

    fun initForTransaction(txnId: Long, suggestion: SuggestedSplitGroup?) {
        viewModelScope.launch {
            val txn = transactionRepository.getById(txnId)
            if (txn == null || txn.amountPaise == null) {
                _state.value = SplitEditState.Done
                return@launch
            }
            
            currentTxnId = txnId
            totalAmount = txn.amountPaise
            
            // Check if split already exists
            val existing = splitRepository.getSplitsForTransaction(txnId).firstOrNull()
            if (existing != null) {
                val (_, dbParticipants) = existing
                // Check if any has a custom amount that doesn't equal an even split?
                // Actually for now just load them.
                isCustom = false // Or infer from data
                currentParticipants.clear()
                currentParticipants.addAll(dbParticipants.map {
                    SplitParticipantUiModel(
                        participantId = it.participantId,
                        displayName = it.displayName,
                        contactId = it.contactId,
                        isAppUser = it.isAppUser,
                        sharePaise = it.sharePaise,
                        customOverridePaise = it.sharePaise,
                        customOverrideString = String.format(java.util.Locale.US, "%.2f", it.sharePaise / 100.0)
                    )
                })
                recalculateShares()
            } else {
                // New split
                currentParticipants.clear()
                // App user is always index 0
                currentParticipants.add(
                    SplitParticipantUiModel(
                        participantId = UUID.randomUUID().toString(),
                        displayName = "You",
                        contactId = null, // Or actual ID if available
                        isAppUser = true
                    )
                )
                
                // Add suggested participants
                suggestion?.participantNames?.forEach { name ->
                    currentParticipants.add(
                        SplitParticipantUiModel(
                            participantId = UUID.randomUUID().toString(), // Or lookup contactId
                            displayName = name,
                            contactId = null,
                            isAppUser = false
                        )
                    )
                }
                
                recalculateShares()
            }
        }
    }

    fun setCustomMode(custom: Boolean) {
        isCustom = custom
        if (custom) {
            // Copy current shares to overrides so they start where even left off
            currentParticipants.forEach { 
                it.customOverridePaise = it.sharePaise
                it.customOverrideString = String.format(java.util.Locale.US, "%.2f", it.sharePaise / 100.0)
            }
        } else {
            currentParticipants.forEach { 
                it.customOverridePaise = null
                it.customOverrideString = ""
            }
        }
        recalculateShares()
    }

    fun addParticipant(name: String, contactId: String?) {
        if (name.isBlank()) return
        currentParticipants.add(
            SplitParticipantUiModel(
                participantId = contactId ?: UUID.randomUUID().toString(),
                displayName = name.trim(),
                contactId = contactId,
                isAppUser = false
            )
        )
        recalculateShares()
    }

    fun addParticipants(names: List<String>) {
        val validNames = names.map { it.trim() }.filter { it.isNotBlank() }
        if (validNames.isEmpty()) return
        validNames.forEach { name ->
            currentParticipants.add(
                SplitParticipantUiModel(
                    participantId = UUID.randomUUID().toString(),
                    displayName = name,
                    contactId = null,
                    isAppUser = false
                )
            )
        }
        recalculateShares()
    }

    fun removeParticipant(participantId: String) {
        currentParticipants.removeAll { it.participantId == participantId && !it.isAppUser }
        recalculateShares()
    }

    fun updateCustomShare(participantId: String, amountStr: String) {
        if (!isCustom) setCustomMode(true)
        val p = currentParticipants.find { it.participantId == participantId }
        if (p != null) {
            p.customOverrideString = amountStr
            
            // Try to parse using AmountParser, default to 0 if invalid
            val parseResult = com.chirag.arthix.util.AmountParser.parse(amountStr)
            p.customOverridePaise = if (parseResult is com.chirag.arthix.util.AmountParseResult.Success) {
                parseResult.amountPaise
            } else {
                0L
            }
            recalculateShares()
        }
    }

    private fun recalculateShares() {
        if (currentParticipants.isEmpty()) return

        var remainder = 0L
        if (isCustom) {
            val sum = currentParticipants.sumOf { it.customOverridePaise ?: 0L }
            remainder = totalAmount - sum
            currentParticipants.forEach { it.sharePaise = it.customOverridePaise ?: 0L }
        } else {
            val mode = SplitMode.Even
            val pIds = currentParticipants.map { it.participantId }
            val shares = computeSplitShares(totalAmount, pIds, mode)
            
            shares.forEach { share ->
                currentParticipants.find { it.participantId == share.participantId }?.sharePaise = share.sharePaise
            }
            remainder = 0L
        }

        _state.value = SplitEditState.Active(
            transactionId = currentTxnId,
            totalAmountPaise = totalAmount,
            participants = currentParticipants.toList(),
            isCustomMode = isCustom,
            remainderToAllocate = remainder
        )
    }

    fun confirmSplit(confirmedVia: SplitConfirmedVia) {
        if (isCustom) {
            val sum = currentParticipants.sumOf { it.sharePaise }
            if (sum != totalAmount) return // Block confirmation
        }

        viewModelScope.launch {
            val record = SplitRecordEntity(
                transactionId = currentTxnId,
                confirmedVia = confirmedVia,
                createdAt = System.currentTimeMillis(),
                lockedAmountPaise = null,
                recalculatedFlag = false
            )
            
            val participantEntities = currentParticipants.map {
                SplitParticipantEntity(
                    splitRecordId = 0L, // will be set in repo
                    participantId = it.participantId,
                    displayName = it.displayName,
                    contactId = it.contactId,
                    isAppUser = it.isAppUser,
                    sharePaise = it.sharePaise,
                    previousSharePaise = null
                )
            }
            
            // Check if updating or creating
            val existing = splitRepository.getSplitsForTransaction(currentTxnId).firstOrNull()
            if (existing != null) {
                splitRepository.updateSplit(existing.first, participantEntities)
            } else {
                splitRepository.createSplit(record, participantEntities)
            }
            _state.value = SplitEditState.Done
        }
    }

    fun cancel() {
        _state.value = SplitEditState.Done
    }
}
