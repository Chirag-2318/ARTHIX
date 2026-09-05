package com.chirag.arthix.report

import android.util.Log
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.model.JsonConverters
import com.chirag.arthix.data.repository.ReportRepository
import com.chirag.arthix.report.engine.GroundingValidator
import com.chirag.arthix.report.engine.ReportComputationEngine
import com.chirag.arthix.report.engine.ValidationResult
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.GroundingWhitelist
import com.chirag.arthix.report.model.ReportPeriod
import com.chirag.arthix.report.model.ReportPeriodType
import com.chirag.arthix.report.phrasing.OnDeviceMediaPipeEngine
import com.chirag.arthix.report.phrasing.ReportPhrasingEngine
import com.chirag.arthix.report.phrasing.TemplatePhrasingEngine
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Report generation orchestrator (PRD §8, FR-7).
 *
 * Pipeline:
 * 1. Compute verified metrics via [ReportComputationEngine] (100% deterministic paise math).
 * 2. Construct [GroundingWhitelist] from computed figures.
 * 3. Phrase insight sentences via [ReportPhrasingEngine].
 * 4. Verify output with [GroundingValidator] (fallback to template on failure).
 * 5. Persist [ReportEntity] in [ReportRepository] for historical records and UI display.
 */
@Singleton
class ReportGenerator @Inject constructor(
    private val computationEngine: ReportComputationEngine,
    private val phrasingEngine: OnDeviceMediaPipeEngine,
    private val templateEngine: TemplatePhrasingEngine,
    private val validator: GroundingValidator,
    private val reportRepository: ReportRepository,
    private val gson: Gson,
) {

    companion object {
        private const val TAG = "ReportGenerator"
    }

    /**
     * Generate a report for [period], validate grounding, and persist to Room database.
     */
    suspend fun generateAndSaveReport(
        periodType: ReportPeriodType = ReportPeriodType.WEEKLY,
    ): ReportEntity {
        val period = when (periodType) {
            ReportPeriodType.WEEKLY -> ReportPeriod.currentWeek()
            ReportPeriodType.MONTHLY -> ReportPeriod.currentMonth()
            ReportPeriodType.YEARLY -> ReportPeriod.currentYear()
        }
        
        Log.i(TAG, "Starting report generation for period: ${period.label}")

        // 1. Deterministic calculation
        val computedData = computationEngine.compute(period)

        // 2. Build whitelist
        val whitelist = GroundingWhitelist.fromComputedData(computedData)

        // 3. Phrasing
        var phrasedSentences = phrasingEngine.phraseReport(computedData, whitelist)

        // 4. Double-check validation safeguard
        val allText = phrasedSentences.joinToString(" ")
        val validation = validator.validate(allText, whitelist)
        if (validation !is ValidationResult.Valid) {
            Log.w(TAG, "Post-phrasing validation check failed -> switching to deterministic template sentences")
            phrasedSentences = templateEngine.phraseReport(computedData, whitelist)
        }

        // 5. Serialize and persist
        val report = ReportEntity(
            periodStart = period.startMs,
            periodEnd = period.endMs,
            categoryBreakdownJson = gson.toJson(computedData.categoryBreakdown),
            netFlowPaise = computedData.netFlowPaise,
            suggestionsJson = gson.toJson(phrasedSentences),
            projectedTotalPaise = computedData.projectedTotalPaise,
            projectedSavingsPaise = computedData.projectedSavingsPaise,
            uncategorizedTotalPaise = computedData.uncategorizedTotalPaise,
            generatedAt = System.currentTimeMillis(),
        )

        val id = reportRepository.save(report)
        Log.i(TAG, "Report #$id generated and saved successfully (netFlow=₹${report.netFlowPaise / 100})")

        return report.copy(id = id)
    }

    /**
     * Compute report data without persisting (useful for test assertions and real-time previews).
     */
    suspend fun computeOnly(periodType: ReportPeriodType = ReportPeriodType.WEEKLY): ComputedReportData {
        val period = when (periodType) {
            ReportPeriodType.WEEKLY -> ReportPeriod.currentWeek()
            ReportPeriodType.MONTHLY -> ReportPeriod.currentMonth()
            ReportPeriodType.YEARLY -> ReportPeriod.currentYear()
        }
        return computationEngine.compute(period)
    }

    suspend fun generateReportForExport(periodType: ReportPeriodType = ReportPeriodType.WEEKLY): Pair<ComputedReportData, List<String>> {
        val period = when (periodType) {
            ReportPeriodType.WEEKLY -> ReportPeriod.currentWeek()
            ReportPeriodType.MONTHLY -> ReportPeriod.currentMonth()
            ReportPeriodType.YEARLY -> ReportPeriod.currentYear()
        }
        val computedData = computationEngine.compute(period)
        val whitelist = GroundingWhitelist.fromComputedData(computedData)
        
        var phrasedSentences = phrasingEngine.phraseReport(computedData, whitelist)
        val allText = phrasedSentences.joinToString(" ")
        val validation = validator.validate(allText, whitelist)
        if (validation !is ValidationResult.Valid) {
            phrasedSentences = templateEngine.phraseReport(computedData, whitelist)
        }
        return Pair(computedData, phrasedSentences)
    }
}
