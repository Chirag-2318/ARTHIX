package com.chirag.arthix.report.engine

import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.ReportPeriod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic financial computation engine (PRD §3, EC-44, EC-45, EC-46).
 *
 * Implements 100% Kotlin integer paise arithmetic for category sums, net flow,
 * pending inclusion, zero-baseline branching, and projection anchoring.
 */
@Singleton
class ReportComputationEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val projectionAnchor: ProjectionAnchor,
    private val suggestionRuleEngine: SuggestionRuleEngine,
) {

    /**
     * Compute full deterministic metrics for [period].
     */
    suspend fun compute(period: ReportPeriod): ComputedReportData {
        // Query non-discarded transactions for current period
        val currentTxns = transactionDao.getInRange(period.startMs, period.endMs)
        val prevTxns = transactionDao.getInRange(period.prevStartMs, period.prevEndMs)

        // Sum inflows and outflows
        var totalInflow = 0L
        var totalOutflow = 0L

        val categoryMap = mutableMapOf<String, Long>()

        for (txn in currentTxns) {
            val amount = txn.amountPaise ?: continue
            when (txn.direction) {
                Direction.INFLOW -> totalInflow += amount
                Direction.OUTFLOW -> {
                    totalOutflow += amount
                    val cat = txn.category ?: "other"
                    categoryMap[cat] = (categoryMap[cat] ?: 0L) + amount
                }
            }
        }

        // EC-44: Fetch pending/unlabeled amount and ensure it is included in total spend
        val uncategorizedTotal = transactionDao.getUncategorizedTotal(period.startMs, period.endMs)
        totalOutflow += uncategorizedTotal

        val netFlow = totalInflow - totalOutflow

        // Previous period category breakdown and total outflow for baseline
        val prevCategoryMap = mutableMapOf<String, Long>()
        var prevTotalOutflow = 0L

        for (txn in prevTxns) {
            val amount = txn.amountPaise ?: continue
            if (txn.direction == Direction.OUTFLOW) {
                prevTotalOutflow += amount
                val cat = txn.category ?: "other"
                prevCategoryMap[cat] = (prevCategoryMap[cat] ?: 0L) + amount
            }
        }

        // EC-45: Zero-baseline detection
        val noPriorData = prevTxns.isEmpty() || prevTotalOutflow == 0L

        // EC-43: Projection anchoring
        val projectedTotal = projectionAnchor.computeProjectedTotal(
            currentSpendPaise = totalOutflow,
            previousPeriodSpendPaise = prevTotalOutflow,
            period = period,
        )

        // Suggestion generation
        val suggestion = suggestionRuleEngine.generateSuggestion(
            currentCategorySums = categoryMap,
            previousCategorySums = prevCategoryMap,
        )

        val projectedSavings = suggestion?.projectedSavingsPaise ?: 0L

        return ComputedReportData(
            period = period,
            categoryBreakdown = categoryMap,
            totalInflowPaise = totalInflow,
            totalOutflowPaise = totalOutflow,
            netFlowPaise = netFlow,
            uncategorizedTotalPaise = uncategorizedTotal,
            projectedTotalPaise = projectedTotal,
            projectedSavingsPaise = projectedSavings,
            noPriorData = noPriorData,
            suggestion = suggestion,
        )
    }
}
