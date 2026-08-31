package com.chirag.arthix.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.voice.VoskSttEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Chart bar data for the Account Statement chart.
 */
data class ChartBarData(
    val label: String,         // e.g. "Mon", "Tue" or "Jan", "Feb"
    val outflowPaise: Long,
    val inflowPaise: Long,
)

/**
 * Time filter options for the chart.
 */
enum class ChartTimeFilter(val label: String) {
    WEEK("W"),
    MONTH("M"),
    SIX_MONTHS("6M"),
    YEAR("Y"),
    ALL("All"),
}

/**
 * UI state for the transaction history screen.
 */
data class TransactionHistoryUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val chartBars: List<ChartBarData> = emptyList(),
    val chartMaxPaise: Long = 1L,
    val selectedFilter: ChartTimeFilter = ChartTimeFilter.WEEK,
    val totalOutflowPaise: Long = 0L,
    val totalInflowPaise: Long = 0L,
    val transactionToDelete: TransactionEntity? = null
)

/**
 * ViewModel for [TransactionHistoryScreen].
 *
 * Observes the full transaction history reactively via Room Flow.
 * Computes chart aggregation data for the spending overview.
 */
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    val sttEngine: VoskSttEngine,
) : ViewModel() {

    private val filterState = MutableStateFlow(ChartTimeFilter.WEEK)
    private val _transactionToDelete = MutableStateFlow<TransactionEntity?>(null)

    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        repository.observeHistory(),
        filterState,
        _transactionToDelete
    ) { transactions, filter, txnToDelete ->
        val chartBars = computeBars(transactions, filter)
        val maxVal = chartBars.maxOfOrNull { maxOf(it.outflowPaise, it.inflowPaise) } ?: 1L

        TransactionHistoryUiState(
            transactions = transactions,
            isLoading = false,
            chartBars = chartBars,
            chartMaxPaise = maxVal.coerceAtLeast(1L),
            selectedFilter = filter,
            totalOutflowPaise = transactions
                .filter { it.direction == Direction.OUTFLOW }
                .sumOf { it.amountPaise ?: 0L },
            totalInflowPaise = transactions
                .filter { it.direction == Direction.INFLOW }
                .sumOf { it.amountPaise ?: 0L },
            transactionToDelete = txnToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionHistoryUiState(),
    )

    fun setFilter(filter: ChartTimeFilter) {
        filterState.value = filter
    }

    private fun computeBars(transactions: List<TransactionEntity>, filter: ChartTimeFilter): List<ChartBarData> {
        return when (filter) {
            ChartTimeFilter.WEEK -> computeWeeklyBars(transactions)
            ChartTimeFilter.MONTH -> computeMonthlyBars(transactions)
            ChartTimeFilter.SIX_MONTHS -> computeSixMonthBars(transactions)
            ChartTimeFilter.YEAR -> computeYearlyBars(transactions)
            ChartTimeFilter.ALL -> computeAllTimeBars(transactions)
        }
    }

    private fun computeWeeklyBars(transactions: List<TransactionEntity>): List<ChartBarData> {
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val bars = mutableListOf<ChartBarData>()
        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val label = dayFormat.format(cal.time)

            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)

            val dayEnd = dayStart.clone() as Calendar
            dayEnd.add(Calendar.DAY_OF_YEAR, 1)

            val dayTxns = transactions.filter {
                it.timestamp >= dayStart.timeInMillis && it.timestamp < dayEnd.timeInMillis
            }

            bars.add(
                ChartBarData(
                    label = label.uppercase(),
                    outflowPaise = dayTxns.filter { it.direction == Direction.OUTFLOW }.sumOf { it.amountPaise ?: 0L },
                    inflowPaise = dayTxns.filter { it.direction == Direction.INFLOW }.sumOf { it.amountPaise ?: 0L },
                )
            )
        }
        return bars
    }

    private fun computeMonthlyBars(transactions: List<TransactionEntity>): List<ChartBarData> {
        // Last 4 weeks
        val cal = Calendar.getInstance()
        val bars = mutableListOf<ChartBarData>()
        for (i in 3 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.WEEK_OF_YEAR, -i)
            val label = "W${4 - i}"

            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = start.clone() as Calendar
            end.add(Calendar.WEEK_OF_YEAR, 1)

            val weekTxns = transactions.filter {
                it.timestamp >= start.timeInMillis && it.timestamp < end.timeInMillis
            }

            bars.add(
                ChartBarData(
                    label = label,
                    outflowPaise = weekTxns.filter { it.direction == Direction.OUTFLOW }.sumOf { it.amountPaise ?: 0L },
                    inflowPaise = weekTxns.filter { it.direction == Direction.INFLOW }.sumOf { it.amountPaise ?: 0L },
                )
            )
        }
        return bars
    }

    private fun computeSixMonthBars(transactions: List<TransactionEntity>): List<ChartBarData> {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM", Locale.getDefault())
        val bars = mutableListOf<ChartBarData>()
        for (i in 5 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -i)
            val label = format.format(cal.time)

            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = start.clone() as Calendar
            end.add(Calendar.MONTH, 1)

            val txns = transactions.filter {
                it.timestamp >= start.timeInMillis && it.timestamp < end.timeInMillis
            }

            bars.add(
                ChartBarData(
                    label = label.uppercase(),
                    outflowPaise = txns.filter { it.direction == Direction.OUTFLOW }.sumOf { it.amountPaise ?: 0L },
                    inflowPaise = txns.filter { it.direction == Direction.INFLOW }.sumOf { it.amountPaise ?: 0L },
                )
            )
        }
        return bars
    }

    private fun computeYearlyBars(transactions: List<TransactionEntity>): List<ChartBarData> {
        // Last 4 quarters or 6 bi-months (we will use 4 quarters to fit 4 bars nicely)
        val cal = Calendar.getInstance()
        val bars = mutableListOf<ChartBarData>()
        for (i in 3 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -(i * 3))
            val label = "Q${4 - i}"

            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = start.clone() as Calendar
            end.add(Calendar.MONTH, 3)

            val txns = transactions.filter {
                it.timestamp >= start.timeInMillis && it.timestamp < end.timeInMillis
            }

            bars.add(
                ChartBarData(
                    label = label,
                    outflowPaise = txns.filter { it.direction == Direction.OUTFLOW }.sumOf { it.amountPaise ?: 0L },
                    inflowPaise = txns.filter { it.direction == Direction.INFLOW }.sumOf { it.amountPaise ?: 0L },
                )
            )
        }
        return bars
    }

    private fun computeAllTimeBars(transactions: List<TransactionEntity>): List<ChartBarData> {
        if (transactions.isEmpty()) return emptyList()
        
        // Group by year
        val minTime = transactions.minOf { it.timestamp }
        val maxTime = transactions.maxOf { it.timestamp }
        
        val calMin = Calendar.getInstance().apply { timeInMillis = minTime }
        val minYear = calMin.get(Calendar.YEAR)
        
        val calMax = Calendar.getInstance().apply { timeInMillis = maxTime }
        val maxYear = calMax.get(Calendar.YEAR)
        
        val bars = mutableListOf<ChartBarData>()
        for (year in minYear..maxYear) {
            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = start.clone() as Calendar
            end.add(Calendar.YEAR, 1)
            
            val txns = transactions.filter {
                it.timestamp >= start.timeInMillis && it.timestamp < end.timeInMillis
            }

            bars.add(
                ChartBarData(
                    label = year.toString(),
                    outflowPaise = txns.filter { it.direction == Direction.OUTFLOW }.sumOf { it.amountPaise ?: 0L },
                    inflowPaise = txns.filter { it.direction == Direction.INFLOW }.sumOf { it.amountPaise ?: 0L },
                )
            )
        }
        return bars
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
                repository.discard(txn.id)
                _transactionToDelete.value = null
            }
        }
    }
}
