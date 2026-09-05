package com.chirag.arthix.report.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Encapsulates the time boundaries for report calculation (PRD §2, EC-50).
 *
 * Grounded in period-over-period semantics:
 * - Current period: [startMs, endMs]
 * - Previous period: [prevStartMs, prevEndMs] for baseline comparison
 */
enum class ReportPeriodType { WEEKLY, MONTHLY, YEARLY }

data class ReportPeriod(
    val startMs: Long,
    val endMs: Long,
    val prevStartMs: Long,
    val prevEndMs: Long,
    val label: String,
    val elapsedDaysInPeriod: Int = 7,
    val totalDaysInPeriod: Int = 7,
) {
    companion object {
        private const val DAY_MS = 86_400_000L
        private const val WEEK_MS = 7 * DAY_MS

        /**
         * Create a standard 7-day rolling period ending at [nowMs].
         */
        fun currentWeek(nowMs: Long = System.currentTimeMillis()): ReportPeriod {
            val end = nowMs
            val start = nowMs - WEEK_MS
            val prevEnd = start
            val prevStart = start - WEEK_MS

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val label = "${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}"

            return ReportPeriod(
                startMs = start,
                endMs = end,
                prevStartMs = prevStart,
                prevEndMs = prevEnd,
                label = label,
                elapsedDaysInPeriod = 7,
                totalDaysInPeriod = 7,
            )
        }

        /**
         * Create a standard rolling 30-day period ending at [nowMs].
         */
        fun currentMonth(nowMs: Long = System.currentTimeMillis()): ReportPeriod {
            val end = nowMs
            val start = nowMs - (30 * DAY_MS)
            val prevEnd = start
            val prevStart = start - (30 * DAY_MS)

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val label = "${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}"

            return ReportPeriod(
                startMs = start,
                endMs = end,
                prevStartMs = prevStart,
                prevEndMs = prevEnd,
                label = label,
                elapsedDaysInPeriod = 30,
                totalDaysInPeriod = 30,
            )
        }

        /**
         * Create a standard rolling 365-day period ending at [nowMs].
         */
        fun currentYear(nowMs: Long = System.currentTimeMillis()): ReportPeriod {
            val end = nowMs
            val start = nowMs - (365 * DAY_MS)
            val prevEnd = start
            val prevStart = start - (365 * DAY_MS)

            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val label = "${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}"

            return ReportPeriod(
                startMs = start,
                endMs = end,
                prevStartMs = prevStart,
                prevEndMs = prevEnd,
                label = label,
                elapsedDaysInPeriod = 365,
                totalDaysInPeriod = 365,
            )
        }

        /**
         * Create a custom period with explicit day progression (for thin-data projection testing).
         */
        fun custom(
            startMs: Long,
            endMs: Long,
            prevStartMs: Long,
            prevEndMs: Long,
            label: String,
            elapsedDays: Int = 7,
            totalDays: Int = 7,
        ): ReportPeriod = ReportPeriod(
            startMs = startMs,
            endMs = endMs,
            prevStartMs = prevStartMs,
            prevEndMs = prevEndMs,
            label = label,
            elapsedDaysInPeriod = elapsedDays.coerceAtLeast(1),
            totalDaysInPeriod = totalDays.coerceAtLeast(1),
        )
    }
}
