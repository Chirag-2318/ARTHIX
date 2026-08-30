package com.chirag.arthix.report.engine

import com.chirag.arthix.report.model.ReportPeriod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anti-naive extrapolation projection algorithm (PRD §3.d, EC-43).
 *
 * ## The Problem (EC-43 Critical)
 * A naive formula `(spend_so_far / time_elapsed) * total_period` explodes on thin data
 * (e.g. 2 transactions of ₹500 in the first 2 hours on Day 1 extrapolates to ₹84,000/week!).
 *
 * ## The Solution (Weighted Blending)
 * We anchor to the previous period baseline (or sensible historical daily rate), blending
 * with the current period rate weighted by the proportion of time elapsed:
 *
 * `weight_current = elapsedDays / totalDays`
 * `weight_baseline = 1.0 - weight_current`
 * `blended_daily_rate = (weight_current * current_daily) + (weight_baseline * baseline_daily)`
 * `projected_total = blended_daily_rate * totalDays`
 *
 * On Day 1 of 7: weight_current = 1/7 (~14%), weight_baseline = 6/7 (~86%).
 * This anchors securely to normal spend and cannot produce runaway extrapolations.
 */
@Singleton
class ProjectionAnchor @Inject constructor() {

    companion object {
        /** Default baseline daily spend (~₹500/day) if zero prior history exists. */
        const val DEFAULT_DAILY_BASELINE_PAISE = 50_000L
    }

    /**
     * Compute projected total spend for [period] given [currentSpendPaise] and [previousPeriodSpendPaise].
     */
    fun computeProjectedTotal(
        currentSpendPaise: Long,
        previousPeriodSpendPaise: Long,
        period: ReportPeriod,
    ): Long {
        val totalDays = period.totalDaysInPeriod.coerceAtLeast(1)
        val elapsedDays = period.elapsedDaysInPeriod.coerceIn(1, totalDays)

        val currentDailyRate = currentSpendPaise.toDouble() / elapsedDays

        val baselineDailyRate = if (previousPeriodSpendPaise > 0) {
            previousPeriodSpendPaise.toDouble() / totalDays
        } else {
            DEFAULT_DAILY_BASELINE_PAISE.toDouble()
        }

        val weightCurrent = elapsedDays.toDouble() / totalDays
        val weightBaseline = 1.0 - weightCurrent

        val blendedDailyRate = (weightCurrent * currentDailyRate) + (weightBaseline * baselineDailyRate)
        val projectedTotal = (blendedDailyRate * totalDays).toLong()

        // Projection cannot be less than what has already been spent
        return projectedTotal.coerceAtLeast(currentSpendPaise)
    }
}
