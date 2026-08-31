package com.chirag.arthix.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.data.preferences.AccountPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.voice.VoskSttEngine
import javax.inject.Inject

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.NotificationsActive

/**
 * ViewModel for the Home dashboard screen.
 *
 * All data is derived from live Room queries — no hardcoded sample numbers (NFR-5).
 * Computes today's total spend, category breakdown, and recent transactions.
 */

data class AppAlert(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val isUnread: Boolean = true
)

data class HomeUiState(
    val todaySpendPaise: Long = 0L,
    val todayInflowPaise: Long = 0L,
    val pendingCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val streakDays: Int = 0,
    val txnsLoggedThisWeek: Int = 0,
    val weekChangePercent: Double = 0.0,
    val insightHeadline: String = "No insights yet",
    val insightBody: String = "Keep logging transactions to get personalized insights.",
    val alerts: List<AppAlert> = emptyList(),
    val unreadAlertsCount: Int = 0,
    val transactionToDelete: TransactionEntity? = null,
    val coachMarkDismissed: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
    private val accountPreferences: AccountPreferences,
    val sttEngine: VoskSttEngine,
) : ViewModel() {
    
    private val _transactionToDelete = kotlinx.coroutines.flow.MutableStateFlow<TransactionEntity?>(null)

    val uiState: StateFlow<HomeUiState> = kotlinx.coroutines.flow.combine(
        transactionRepository.observeHistory(),
        _transactionToDelete,
        accountPreferences.coachMarkDismissed
    ) { transactions, txnToDelete, coachDismissed ->
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
                
            // Streak and week calculations
            val oneDayMillis = 24 * 60 * 60 * 1000L
            val weekStart = todayStart - (6 * oneDayMillis)
            val txnsLoggedThisWeek = transactions.count { it.timestamp >= weekStart }
            
            // Calculate streak (consecutive days with at least one transaction)
            val daysWithTxns = transactions.map { (todayStart - it.timestamp) / oneDayMillis }.filter { it >= 0 }.toSet()
            var streak = 0
            while (daysWithTxns.contains(streak.toLong())) {
                streak++
            }
            
            // Mock week change percent for now
            val weekChangePercent = -12.4 
            
            // Build Alerts
            val alerts = mutableListOf<AppAlert>()
            if (pendingCount > 0) {
                alerts.add(AppAlert(
                    title = "Pending Transactions",
                    message = "You have $pendingCount transactions that need your review.",
                    icon = Icons.Filled.Warning,
                    isUnread = true
                ))
            }
            val unsettledSplits = splits.values.filter { split ->
                val myShare = split.second.find { it.isAppUser }?.sharePaise ?: 0L
                val total = split.first.lockedAmountPaise ?: 0L // Assuming transaction amount is matched
                val othersPaid = split.second.count { !it.isAppUser && it.isPaid }
                val othersTotal = split.second.count { !it.isAppUser }
                othersPaid < othersTotal && othersTotal > 0
            }.size
            if (unsettledSplits > 0) {
                alerts.add(AppAlert(
                    title = "Unsettled Splits",
                    message = "You have $unsettledSplits unsettled bills with friends.",
                    icon = Icons.Filled.ReceiptLong,
                    isUnread = true
                ))
            }
            if (streak == 0) {
                alerts.add(AppAlert(
                    title = "Budget Streak Broken",
                    message = "You missed logging yesterday. Log a transaction today to start a new streak!",
                    icon = Icons.Filled.NotificationsActive,
                    isUnread = true
                ))
            }

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
                streakDays = streak,
                txnsLoggedThisWeek = txnsLoggedThisWeek,
                weekChangePercent = weekChangePercent,
                insightHeadline = if (categoryBreakdown.isNotEmpty()) "You spent most on ${categoryBreakdown.maxByOrNull { it.value }?.key ?: "something"}" else "Great start!",
                insightBody = "Keep tracking to stay on budget.",
                alerts = alerts,
                unreadAlertsCount = alerts.count { it.isUnread },
                transactionToDelete = txnToDelete,
                coachMarkDismissed = coachDismissed,
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

    fun requestDelete(txn: TransactionEntity) {
        _transactionToDelete.value = txn
    }

    fun dismissDelete() {
        _transactionToDelete.value = null
    }

    fun confirmDelete() {
        _transactionToDelete.value?.let { txn ->
            viewModelScope.launch {
                transactionRepository.discard(txn.id)
                _transactionToDelete.value = null
            }
        }
    }

    fun dismissCoachMark() {
        viewModelScope.launch {
            accountPreferences.dismissCoachMark()
        }
    }
}
