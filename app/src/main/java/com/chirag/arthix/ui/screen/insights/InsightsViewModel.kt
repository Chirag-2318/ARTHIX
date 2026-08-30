package com.chirag.arthix.ui.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.CategorySum
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

data class InsightsUiState(
    val thisWeekSpendPaise: Long = 0L,
    val lastWeekSpendPaise: Long = 0L,
    val thisMonthSpendPaise: Long = 0L,
    val lastMonthSpendPaise: Long = 0L,
    val dailyAveragePaise: Long = 0L,
    val moMPercentage: Float = 0f,
    val categoryBreakdown: List<CategorySum> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = transactionRepository.observeHistory()
        .map { transactions ->
            val now = System.currentTimeMillis()
            val weekStart = now - 7 * 24 * 60 * 60 * 1000L
            val lastWeekStart = weekStart - 7 * 24 * 60 * 60 * 1000L

            val cal = java.util.Calendar.getInstance()
            
            // This month start
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis

            // Last month start & end
            cal.add(java.util.Calendar.MONTH, -1)
            val lastMonthStart = cal.timeInMillis
            cal.add(java.util.Calendar.MONTH, 1)
            cal.add(java.util.Calendar.MILLISECOND, -1)
            val lastMonthEnd = cal.timeInMillis

            val splits = splitRepository.getAllSplits().associateBy { it.first.transactionId }

            val activeOutflows = transactions.filter { txn ->
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

            val thisWeek = activeOutflows.filter { txn -> txn.timestamp >= weekStart }
                .sumOf { txn -> txn.amountPaise ?: 0L }

            val lastWeek = activeOutflows.filter { txn ->
                txn.timestamp in lastWeekStart until weekStart
            }.sumOf { txn -> txn.amountPaise ?: 0L }

            val thisMonth = activeOutflows.filter { txn -> txn.timestamp >= monthStart }
                .sumOf { txn -> txn.amountPaise ?: 0L }
                
            val lastMonth = activeOutflows.filter { txn -> 
                txn.timestamp in lastMonthStart..lastMonthEnd 
            }.sumOf { txn -> txn.amountPaise ?: 0L }

            val daysInCurrentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            val dailyAverage = if (daysInCurrentMonth > 0) thisMonth / daysInCurrentMonth else 0L

            val moM = if (lastMonth > 0) {
                ((thisMonth - lastMonth).toFloat() / lastMonth.toFloat()) * 100f
            } else {
                0f
            }

            val breakdown = activeOutflows
                .filter { txn -> txn.timestamp >= monthStart && txn.category != null && txn.amountPaise != null }
                .groupBy { txn -> txn.category!! }
                .map { (cat, txns) -> CategorySum(cat, txns.sumOf { txn -> txn.amountPaise ?: 0L }) }
                .sortedByDescending { cs -> cs.total }

            InsightsUiState(
                thisWeekSpendPaise = thisWeek,
                lastWeekSpendPaise = lastWeek,
                thisMonthSpendPaise = thisMonth,
                lastMonthSpendPaise = lastMonth,
                dailyAveragePaise = dailyAverage,
                moMPercentage = moM,
                categoryBreakdown = breakdown,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = InsightsUiState(),
        )
}
