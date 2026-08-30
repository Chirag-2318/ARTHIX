package com.chirag.arthix.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.chirag.arthix.data.repository.SplitRepository
import javax.inject.Inject

/**
 * ViewModel for the Home dashboard screen.
 *
 * All data is derived from live Room queries — no hardcoded sample numbers (NFR-5).
 * Computes today's total spend, category breakdown, and recent transactions.
 */

data class HomeUiState(
    val todaySpendPaise: Long = 0L,
    val todayInflowPaise: Long = 0L,
    val pendingCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = transactionRepository.observeHistory()
        .map { transactions: List<TransactionEntity> ->
            val todayStart = todayStartMillis()

            val splits = splitRepository.getAllSplits().associateBy { it.first.transactionId }
            
            val todayTxns = transactions.filter { txn -> txn.timestamp >= todayStart }
            
            val todayOutflows = todayTxns.filter { txn ->
                txn.direction == Direction.OUTFLOW && txn.status != TransactionStatus.DISCARDED
            }.map { txn ->
                val splitData = splits[txn.id]
                if (splitData != null && txn.amountPaise != null) {
                    val (_, participants) = splitData
                    val othersTotal = participants.filter { !it.isAppUser }.sumOf { it.sharePaise }
                    val adjustedPaise = (txn.amountPaise) - othersTotal
                    txn.copy(amountPaise = adjustedPaise.coerceAtLeast(0L))
                } else {
                    txn
                }
            }
            
            val todayInflows = todayTxns.filter { txn ->
                txn.direction == Direction.INFLOW && txn.status != TransactionStatus.DISCARDED
            }

            val pendingCount = transactions.count { txn ->
                txn.status in listOf(
                    TransactionStatus.AWAITING_MATCH,
                    TransactionStatus.AWAITING_CATEGORY,
                    TransactionStatus.AWAITING_AMOUNT,
                )
            }

            val categoryBreakdown = todayOutflows
                .filter { txn -> txn.category != null && txn.amountPaise != null }
                .groupBy { txn -> txn.category!! }
                .mapValues { (_, txns) -> txns.sumOf { txn -> txn.amountPaise ?: 0L } }

            HomeUiState(
                todaySpendPaise = todayOutflows.sumOf { txn -> txn.amountPaise ?: 0L },
                todayInflowPaise = todayInflows.sumOf { txn -> txn.amountPaise ?: 0L },
                pendingCount = pendingCount,
                recentTransactions = transactions
                    .filter { txn -> txn.status != TransactionStatus.DISCARDED }
                    .sortedByDescending { txn -> txn.timestamp }
                    .take(5),
                categoryBreakdown = categoryBreakdown,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState(),
        )

    private fun todayStartMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
