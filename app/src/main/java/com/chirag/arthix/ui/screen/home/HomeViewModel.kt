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
import com.chirag.arthix.voice.WhisperSttEngine
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
    val userName: String = "User",
    val profileAvatar: String? = null,
    val todaySpendPaise: Long = 0L,
    val todayInflowPaise: Long = 0L,
    val pendingCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val splitParticipantCounts: Map<Long, Int> = emptyMap(),
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
    val discardedCount: Int = 0,
    val dailySpendData: List<Pair<String, Long>> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
    private val accountPreferences: AccountPreferences,
    val sttEngine: WhisperSttEngine,
) : ViewModel() {
    
    private val _transactionToDelete = kotlinx.coroutines.flow.MutableStateFlow<TransactionEntity?>(null)

    val uiState: StateFlow<HomeUiState> = kotlinx.coroutines.flow.combine(
        transactionRepository.observeHistory(),
        _transactionToDelete,
        accountPreferences.coachMarkDismissed,
        accountPreferences.displayName,
        accountPreferences.profileAvatar,
    ) { transactions, txnToDelete, coachDismissed, displayName, profileAvatar ->
        val todayStart = todayStartMillis()
        val splits = splitRepository.getAllSplits().associateBy { it.first.transactionId }
            
        // Pre-compute net transaction amounts (subtracting paid split shares for outflows)
        val netTransactions = transactions.map { txn ->
            if (txn.direction == Direction.OUTFLOW && txn.status != TransactionStatus.DISCARDED) {
                val splitData = splits[txn.id]
                if (splitData != null && txn.amountPaise != null) {
                    val (_, participants) = splitData
                    val othersPaidTotal = participants.filter { !it.isAppUser && it.isPaid }.sumOf { it.sharePaise }
                    val adjustedPaise = (txn.amountPaise) - othersPaidTotal
                    txn.copy(amountPaise = adjustedPaise.coerceAtLeast(0L))
                } else {
                    txn
                }
            } else {
                txn
            }
        }

        val todayTxns = netTransactions.filter { txn -> txn.timestamp >= todayStart }
            
        val todayOutflows = todayTxns.filter { txn ->
            txn.direction == Direction.OUTFLOW && txn.status != TransactionStatus.DISCARDED
        }
            
            val todayInflows = todayTxns.filter { txn ->
                txn.direction == Direction.INFLOW && txn.status != TransactionStatus.DISCARDED
            }

        val pendingCount = netTransactions.count { txn ->
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
        val txnsLoggedThisWeek = netTransactions.count { it.timestamp >= weekStart }
            
            // Calculate streak (consecutive days with at least one transaction)
        val daysWithTxns = netTransactions.map { (todayStart - it.timestamp) / oneDayMillis }.filter { it >= 0 }.toSet()
            var streak = 0
            while (daysWithTxns.contains(streak.toLong())) {
                streak++
            }
            
            // Calculate dynamic week change percent
            val lastWeekStart = weekStart - (7 * oneDayMillis)
        val thisWeekOutflow = netTransactions
            .filter { it.timestamp >= weekStart && it.direction == Direction.OUTFLOW && it.status != TransactionStatus.DISCARDED }
            .sumOf { it.amountPaise ?: 0L }
        val lastWeekOutflow = netTransactions
                .filter { it.timestamp in lastWeekStart until weekStart && it.direction == Direction.OUTFLOW && it.status != TransactionStatus.DISCARDED }
                .sumOf { it.amountPaise ?: 0L }

            val weekChangePercent = if (lastWeekOutflow > 0L) {
                ((thisWeekOutflow - lastWeekOutflow).toDouble() / lastWeekOutflow.toDouble()) * 100.0
            } else if (thisWeekOutflow > 0L) {
                100.0
            } else {
                0.0
            }
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

            // Discarded count
            val discardedCount = netTransactions.count { it.status == TransactionStatus.DISCARDED }

            // Daily spend for the last 7 days (compact chart)
            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = java.util.Calendar.getInstance()
            val todayDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // Sun=1, Mon=2...
            val dailySpendData = (0 until 7).map { daysAgo ->
                val dayStart = todayStart - (daysAgo * oneDayMillis)
                val dayEnd = dayStart + oneDayMillis
                val daySpend = netTransactions
                    .filter { it.timestamp in dayStart until dayEnd && it.direction == Direction.OUTFLOW && it.status != TransactionStatus.DISCARDED }
                    .sumOf { it.amountPaise ?: 0L }
                val dayIndex = ((todayDayOfWeek - 2 - daysAgo + 70) % 7) // Map to Mon=0..Sun=6
                dayLabels[dayIndex] to daySpend
            }.reversed()

            HomeUiState(
                userName = if (displayName.isNotBlank()) displayName else "User",
                profileAvatar = profileAvatar,
                todaySpendPaise = todayOutflows.sumOf { txn -> txn.amountPaise ?: 0L },
                todayInflowPaise = todayInflows.sumOf { txn -> txn.amountPaise ?: 0L },
                pendingCount = pendingCount,
                recentTransactions = netTransactions
                    .filter { txn -> txn.status != TransactionStatus.DISCARDED }
                    .sortedByDescending { txn -> txn.timestamp }
                    .take(5),
                splitParticipantCounts = splits.mapValues { it.value.second.size },
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
                discardedCount = discardedCount,
                dailySpendData = dailySpendData,
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
