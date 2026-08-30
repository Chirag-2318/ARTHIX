package com.chirag.arthix.report.model

/**
 * Structured deterministic recommendation produced by [SuggestionRuleEngine].
 */
data class ComputedSuggestion(
    val category: String,
    val currentSpendPaise: Long,
    val baselineSpendPaise: Long,
    val percentageAboveBaseline: Int,
    val targetReductionPercentage: Int,
    val projectedSavingsPaise: Long,
)

/**
 * Verified, deterministically computed metrics for a report period (PRD §3, EC-44, EC-45).
 *
 * Everything here is calculated purely in integer paise with 0% LLM involvement.
 */
data class ComputedReportData(
    val period: ReportPeriod,
    val categoryBreakdown: Map<String, Long>,
    val totalInflowPaise: Long,
    val totalOutflowPaise: Long,
    val netFlowPaise: Long,
    val uncategorizedTotalPaise: Long,
    val projectedTotalPaise: Long,
    val projectedSavingsPaise: Long,
    val noPriorData: Boolean,
    val suggestion: ComputedSuggestion?,
)
