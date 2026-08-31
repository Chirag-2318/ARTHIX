package com.chirag.arthix.ui.screen.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitListItemUiModel(
    val splitId: Long,
    val transactionId: Long,
    val merchantName: String,
    val totalAmountPaise: Long,
    val participantsCount: Int,
    val mySharePaise: Long,
    val netOwedPaise: Long,
    val paidParticipantsCount: Int
)

@HiltViewModel
class SplitListViewModel @Inject constructor(
    private val splitRepository: SplitRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<SplitListItemUiModel>?>(null)
    val uiState: StateFlow<List<SplitListItemUiModel>?> = _uiState

    fun loadSplits() {
        viewModelScope.launch {
            val splits = splitRepository.getAllSplits()
            val uiModels = splits.mapNotNull { (record, participants) ->
                val txn = transactionRepository.getById(record.transactionId) ?: return@mapNotNull null
                
                val myShare = participants.find { it.isAppUser }?.sharePaise ?: 0L
                val total = txn.amountPaise ?: 0L
                val netOwed = total - myShare

                SplitListItemUiModel(
                    splitId = record.id,
                    transactionId = record.transactionId,
                    merchantName = txn.payee ?: txn.category ?: "Unknown",
                    totalAmountPaise = total,
                    participantsCount = participants.size,
                    mySharePaise = myShare,
                    netOwedPaise = netOwed,
                    paidParticipantsCount = participants.count { it.isPaid }
                )
            }
            _uiState.value = uiModels.sortedByDescending { it.splitId }
        }
    }
}
