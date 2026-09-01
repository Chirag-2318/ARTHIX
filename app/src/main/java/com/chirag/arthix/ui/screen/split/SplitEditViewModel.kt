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
import com.chirag.arthix.voice.WhisperSttEngine
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
    val sharePaise: Long = 0L,
    val customOverridePaise: Long? = null,
    val customOverrideString: String = "",
    val isPaid: Boolean = false
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
    val sttEngine: WhisperSttEngine
) : ViewModel() {

    private val _state = MutableStateFlow<SplitEditState>(SplitEditState.Loading)
    val state: StateFlow<SplitEditState> = _state

    private var currentTxnId: Long = 0L
    private var totalAmount: Long = 0L
    private var isCustom: Boolean = false
    private var currentParticipants: List<SplitParticipantUiModel> = emptyList()

    fun initForTransaction(txnId: Long, suggestion: SuggestedSplitGroup?, initialNames: List<String> = emptyList()) {
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
                val pIds = dbParticipants.map { it.participantId }
                val evenShares = if (pIds.isNotEmpty()) {
                    computeSplitShares(totalAmount, pIds, SplitMode.Even).associate { it.participantId to it.sharePaise }
                } else {
                    emptyMap()
                }
                val matchesEven = dbParticipants.isNotEmpty() && dbParticipants.all { it.sharePaise == (evenShares[it.participantId] ?: 0L) }
                isCustom = !matchesEven

                currentParticipants = dbParticipants.map {
                    val overrideStr = if (it.sharePaise > 0L) {
                        val rupees = it.sharePaise / 100.0
                        if (rupees == rupees.toLong().toDouble()) rupees.toLong().toString() else String.format(java.util.Locale.US, "%.2f", rupees)
                    } else ""
                    SplitParticipantUiModel(
                        participantId = it.participantId,
                        displayName = it.displayName,
                        contactId = it.contactId,
                        isAppUser = it.isAppUser,
                        sharePaise = it.sharePaise,
                        customOverridePaise = if (isCustom) it.sharePaise else null,
                        customOverrideString = if (isCustom) overrideStr else "",
                        isPaid = it.isPaid
                    )
                }
                recalculateShares()
            } else {
                // New split
                val initialList = mutableListOf<SplitParticipantUiModel>()
                // App user is always index 0
                initialList.add(
                    SplitParticipantUiModel(
                        participantId = UUID.randomUUID().toString(),
                        displayName = "You",
                        contactId = null, // Or actual ID if available
                        isAppUser = true
                    )
                )
                
                // Add initial names passed from caller
                val namesToAdd = (initialNames + (suggestion?.participantNames ?: emptyList()))
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals("You", ignoreCase = true) }
                    .distinct()

                namesToAdd.forEach { name ->
                    initialList.add(
                        SplitParticipantUiModel(
                            participantId = UUID.randomUUID().toString(),
                            displayName = name,
                            contactId = null,
                            isAppUser = false
                        )
                    )
                }
                
                isCustom = false
                currentParticipants = initialList
                recalculateShares()
            }
        }
    }

    fun setCustomMode(custom: Boolean) {
        isCustom = custom
        if (custom) {
            // Copy current shares to overrides so they start where even left off
            currentParticipants = currentParticipants.map { p ->
                val overrideStr = if (p.sharePaise > 0L) {
                    val rupees = p.sharePaise / 100.0
                    if (rupees == rupees.toLong().toDouble()) rupees.toLong().toString() else String.format(java.util.Locale.US, "%.2f", rupees)
                } else ""
                p.copy(
                    customOverridePaise = p.sharePaise,
                    customOverrideString = overrideStr
                )
            }
        } else {
            currentParticipants = currentParticipants.map { p ->
                p.copy(
                    customOverridePaise = null,
                    customOverrideString = ""
                )
            }
        }
        recalculateShares()
    }

    fun addParticipant(name: String, contactId: String?) {
        if (name.isBlank()) return
        currentParticipants = currentParticipants + SplitParticipantUiModel(
            participantId = contactId ?: UUID.randomUUID().toString(),
            displayName = name.trim(),
            contactId = contactId,
            isAppUser = false
        )
        recalculateShares()
    }

    fun addParticipants(names: List<String>) {
        val validNames = names.map { it.trim() }.filter { it.isNotBlank() }
        if (validNames.isEmpty()) return
        val newParticipants = validNames.map { name ->
            SplitParticipantUiModel(
                participantId = UUID.randomUUID().toString(),
                displayName = name,
                contactId = null,
                isAppUser = false
            )
        }
        currentParticipants = currentParticipants + newParticipants
        recalculateShares()
    }

    fun removeParticipant(participantId: String) {
        currentParticipants = currentParticipants.filterNot { it.participantId == participantId && !it.isAppUser }
        recalculateShares()
    }

    fun updateCustomShare(participantId: String, amountStr: String) {
        if (!isCustom) {
            isCustom = true
        }
        
        // Try to parse using AmountParser, default to 0 if invalid
        val parseResult = com.chirag.arthix.util.AmountParser.parse(amountStr)
        val parsedPaise = if (parseResult is com.chirag.arthix.util.AmountParseResult.Success) {
            parseResult.amountPaise
        } else {
            0L
        }

        currentParticipants = currentParticipants.map { p ->
            if (p.participantId == participantId) {
                p.copy(
                    customOverrideString = amountStr,
                    customOverridePaise = parsedPaise,
                    sharePaise = parsedPaise
                )
            } else {
                p
            }
        }
        recalculateShares()
    }

    private fun recalculateShares() {
        if (currentParticipants.isEmpty()) return

        var remainder = 0L
        if (isCustom) {
            val sum = currentParticipants.sumOf { it.customOverridePaise ?: 0L }
            remainder = totalAmount - sum
            currentParticipants = currentParticipants.map { p ->
                p.copy(sharePaise = p.customOverridePaise ?: 0L)
            }
        } else {
            val mode = SplitMode.Even
            val pIds = currentParticipants.map { it.participantId }
            val shares = computeSplitShares(totalAmount, pIds, mode)
            val shareMap = shares.associate { it.participantId to it.sharePaise }
            
            currentParticipants = currentParticipants.map { p ->
                p.copy(sharePaise = shareMap[p.participantId] ?: p.sharePaise)
            }
            remainder = 0L
        }

        _state.value = SplitEditState.Active(
            transactionId = currentTxnId,
            totalAmountPaise = totalAmount,
            participants = currentParticipants,
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
                    previousSharePaise = null,
                    isPaid = it.isPaid
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
