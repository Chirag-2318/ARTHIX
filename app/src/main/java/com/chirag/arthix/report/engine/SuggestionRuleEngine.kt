package com.chirag.arthix.report.engine

import com.chirag.arthix.report.model.ComputedSuggestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic suggestion rule engine (PRD §3.e).
 *
 * Emits structured suggestions referencing actual logged categories and amounts.
 * Zero free text is generated here — produces a verified [ComputedSuggestion] data object.
 */
@Singleton
class SuggestionRuleEngine @Inject constructor() {

    companion object {
        const val DEFAULT_TARGET_REDUCTION_PCT = 20
    }

    /**
     * Compute at least one actionable cut-down suggestion based on current category spend
     * compared against previous period baseline.
     */
    fun generateSuggestion(
        currentCategorySums: Map<String, Long>,
        previousCategorySums: Map<String, Long>,
    ): ComputedSuggestion? {
        if (currentCategorySums.isEmpty()) return null

        // Exclude pending/uncategorized buckets from category suggestions (EC-44)
        val validCurrent = currentCategorySums.filter { (cat, amount) ->
            cat.isNotBlank() && cat.lowercase() != "uncategorized" && amount > 0
        }

        if (validCurrent.isEmpty()) return null

        // Strategy 1: Find category with highest spend increase over baseline
        var bestCandidate: String? = null
        var highestDeltaPaise = Long.MIN_VALUE
        var highestPercentage = 0

        for ((category, currentAmount) in validCurrent) {
            val prevAmount = previousCategorySums[category] ?: 0L
            val delta = currentAmount - prevAmount

            val pct = if (prevAmount > 0) {
                ((currentAmount - prevAmount).toDouble() / prevAmount * 100).toInt()
            } else {
                0
            }

            if (delta > highestDeltaPaise) {
                highestDeltaPaise = delta
                bestCandidate = category
                highestPercentage = pct
            }
        }

        // Strategy 2: If no category is above baseline, pick the single largest spend category
        val targetCategory = bestCandidate ?: validCurrent.maxByOrNull { it.value }?.key ?: return null
        val currentSpend = validCurrent[targetCategory] ?: 0L
        val baselineSpend = previousCategorySums[targetCategory] ?: 0L

        val reductionPct = DEFAULT_TARGET_REDUCTION_PCT
        val projectedSavings = (currentSpend * (reductionPct / 100.0)).toLong()

        val pctAboveBaseline = if (baselineSpend > 0) {
            (((currentSpend - baselineSpend).toDouble() / baselineSpend) * 100).toInt()
        } else {
            0
        }

        return ComputedSuggestion(
            category = targetCategory,
            currentSpendPaise = currentSpend,
            baselineSpendPaise = baselineSpend,
            percentageAboveBaseline = pctAboveBaseline,
            targetReductionPercentage = reductionPct,
            projectedSavingsPaise = projectedSavings,
        )
    }
}
