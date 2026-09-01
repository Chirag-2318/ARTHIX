package com.chirag.arthix.ui.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.ReportRepository
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.report.ReportGenerator
import com.chirag.arthix.report.model.CategorySavingsOpportunity
import com.chirag.arthix.report.model.ReportPeriod
import com.chirag.arthix.ui.screen.report.ReportUiModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class InsightsUiState(
    val thisWeekSpendPaise: Long = 0L,
    val lastWeekSpendPaise: Long = 0L,
    val thisMonthSpendPaise: Long = 0L,
    val lastMonthSpendPaise: Long = 0L,
    val dailyAveragePaise: Long = 0L,
    val moMPercentage: Float = 0f,
    val categoryBreakdown: List<CategorySum> = emptyList(),
    val report: ReportUiModel? = null,
    val isReportLoading: Boolean = false,
    val savingsOpportunities: List<CategorySavingsOpportunity> = emptyList(),
    val discretionarySpendPaise: Long = 0L,
    val essentialSpendPaise: Long = 0L,
    val discretionaryPercentage: Int = 0,
    val recentTransactions: List<com.chirag.arthix.data.entity.TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitRepository: SplitRepository,
    private val reportGenerator: ReportGenerator,
    private val reportRepository: ReportRepository,
    private val gson: Gson,
) : ViewModel() {

    private val isReportLoadingFlow = MutableStateFlow(false)

    init {
        refreshReport()
    }

    fun refreshReport() {
        viewModelScope.launch {
            try {
                isReportLoadingFlow.value = true
                reportGenerator.generateAndSaveReport(ReportPeriod.currentWeek())
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                isReportLoadingFlow.value = false
            }
        }
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        transactionRepository.observeHistory(),
        reportRepository.observeAll(),
        isReportLoadingFlow
    ) { transactions, reports, isReportLoading ->
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

        val latestReport = reports.firstOrNull()?.let { mapToUiModel(it) }

        // Discretionary calculation on current month
        val discretionaryCats = setOf("food", "dining", "shopping", "entertainment", "lifestyle", "travel", "cafe", "other")
        val discretionary = breakdown.filter { cs -> discretionaryCats.any { (cs.category ?: "").lowercase().contains(it) } }
            .sumOf { it.total }
        val essential = (thisMonth - discretionary).coerceAtLeast(0L)
        val discretionaryPct = if (thisMonth > 0) ((discretionary.toDouble() / thisMonth) * 100).toInt() else 0

        // Compute savings opportunities
        val savingsOpps = breakdown.take(3).map { cs ->
            val isDisc = discretionaryCats.any { (cs.category ?: "").lowercase().contains(it) }
            val cutPct = if (isDisc) 25 else 15
            val weeklySav = (cs.total / 4 * (cutPct / 100.0)).toLong()
            CategorySavingsOpportunity(
                category = cs.category ?: "other",
                spendPaise = cs.total,
                targetReductionPct = cutPct,
                weeklySavingsPaise = weeklySav,
                monthlySavingsPaise = weeklySav * 4,
            )
        }

        val recentTxns = transactions
            .filter { it.status != TransactionStatus.DISCARDED }
            .sortedByDescending { it.timestamp }
            .take(10)

        InsightsUiState(
            thisWeekSpendPaise = thisWeek,
            lastWeekSpendPaise = lastWeek,
            thisMonthSpendPaise = thisMonth,
            lastMonthSpendPaise = lastMonth,
            dailyAveragePaise = dailyAverage,
            moMPercentage = moM,
            categoryBreakdown = breakdown,
            report = latestReport,
            isReportLoading = isReportLoading,
            savingsOpportunities = savingsOpps,
            discretionarySpendPaise = discretionary,
            essentialSpendPaise = essential,
            discretionaryPercentage = discretionaryPct,
            recentTransactions = recentTxns,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = InsightsUiState(),
    )

    private fun mapToUiModel(entity: ReportEntity): ReportUiModel {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val label = "${dateFormat.format(Date(entity.periodStart))} – ${dateFormat.format(Date(entity.periodEnd))}"

        val mapType = object : TypeToken<Map<String, Long>>() {}.type
        val categoryBreakdown: Map<String, Long> = try {
            gson.fromJson(entity.categoryBreakdownJson, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val listType = object : TypeToken<List<String>>() {}.type
        val suggestions: List<String> = try {
            gson.fromJson(entity.suggestionsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return ReportUiModel(
            periodLabel = label,
            categoryBreakdown = categoryBreakdown,
            netFlowPaise = entity.netFlowPaise,
            suggestions = suggestions,
            projectedTotalPaise = entity.projectedTotalPaise,
            projectedSavingsPaise = entity.projectedSavingsPaise,
            uncategorizedTotalPaise = entity.uncategorizedTotalPaise,
            noPriorData = entity.netFlowPaise == 0L || suggestions.any { it.contains("first week", ignoreCase = true) },
        )
    }
}

