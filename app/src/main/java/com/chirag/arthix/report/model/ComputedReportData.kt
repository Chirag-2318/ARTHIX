package com.chirag.arthix.report.model

/**
 * Category-specific savings opportunity computed deterministically.
 */
data class CategorySavingsOpportunity(
    val category: String,
    val spendPaise: Long,
    val targetReductionPct: Int,
    val weeklySavingsPaise: Long,
    val monthlySavingsPaise: Long,
)

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
    val additionalOpportunities: List<CategorySavingsOpportunity> = emptyList(),
)

/**
 * Trend representation for a specific category.
 */
data class CategoryTrend(
    val category: String,
    val amountChangedPaise: Long, // Positive means increase, negative means decrease
    val percentageChange: Int, // Positive means increase, negative means decrease
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
    val discretionarySpendPaise: Long = 0L,
    val essentialSpendPaise: Long = 0L,
    val discretionaryPercentage: Int = 0,
    val dailyAveragePaise: Long = 0L,
    val prevTotalInflowPaise: Long = 0L,
    val prevTotalOutflowPaise: Long = 0L,
    val prevNetFlowPaise: Long = 0L,
    val trendingCategories: List<CategoryTrend> = emptyList(),
)

