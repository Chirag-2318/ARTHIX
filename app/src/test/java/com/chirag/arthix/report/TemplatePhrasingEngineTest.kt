package com.chirag.arthix.report

import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.ComputedSuggestion
import com.chirag.arthix.report.model.GroundingWhitelist
import com.chirag.arthix.report.model.ReportPeriod
import com.chirag.arthix.report.phrasing.TemplatePhrasingEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TemplatePhrasingEngineTest {

    private lateinit var engine: TemplatePhrasingEngine

    @Before
    fun setup() {
        engine = TemplatePhrasingEngine()
    }

    @Test
    fun phraseReport_standardSpendWithSuggestion_producesAccurateSentences() = runTest {
        val data = ComputedReportData(
            period = ReportPeriod.currentWeek(),
            categoryBreakdown = mapOf("food" to 300_000L, "shopping" to 150_000L),
            totalInflowPaise = 0L,
            totalOutflowPaise = 450_000L,
            netFlowPaise = -450_000L,
            uncategorizedTotalPaise = 0L,
            projectedTotalPaise = 450_000L,
            projectedSavingsPaise = 60_000L,
            noPriorData = false,
            suggestion = ComputedSuggestion(
                category = "food",
                currentSpendPaise = 300_000L,
                baselineSpendPaise = 200_000L,
                percentageAboveBaseline = 50,
                targetReductionPercentage = 20,
                projectedSavingsPaise = 60_000L,
            )
        )
        val whitelist = GroundingWhitelist.fromComputedData(data)

        val sentences = engine.phraseReport(data, whitelist)

        assertThat(sentences).isNotEmpty()
        val allText = sentences.joinToString(" ")
        assertThat(allText).contains("₹4,500")
        assertThat(allText).contains("Food")
        assertThat(allText).contains("50%")
        assertThat(allText).contains("₹600")
    }

    @Test
    fun phraseReport_zeroPriorData_includesFirstWeekNotice() = runTest {
        val data = ComputedReportData(
            period = ReportPeriod.currentWeek(),
            categoryBreakdown = mapOf("food" to 100_000L),
            totalInflowPaise = 0L,
            totalOutflowPaise = 100_000L,
            netFlowPaise = -100_000L,
            uncategorizedTotalPaise = 0L,
            projectedTotalPaise = 100_000L,
            projectedSavingsPaise = 20_000L,
            noPriorData = true, // EC-45
            suggestion = null
        )
        val whitelist = GroundingWhitelist.fromComputedData(data)

        val sentences = engine.phraseReport(data, whitelist)

        val allText = sentences.joinToString(" ")
        assertThat(allText).contains("first week of tracking")
    }

    @Test
    fun phraseReport_uncategorizedIncluded_mentionsPendingAmount() = runTest {
        val data = ComputedReportData(
            period = ReportPeriod.currentWeek(),
            categoryBreakdown = mapOf("food" to 100_000L),
            totalInflowPaise = 0L,
            totalOutflowPaise = 134_000L,
            netFlowPaise = -134_000L,
            uncategorizedTotalPaise = 34_000L, // ₹340 pending (EC-44)
            projectedTotalPaise = 134_000L,
            projectedSavingsPaise = 0L,
            noPriorData = false,
            suggestion = null
        )
        val whitelist = GroundingWhitelist.fromComputedData(data)

        val sentences = engine.phraseReport(data, whitelist)

        val allText = sentences.joinToString(" ")
        assertThat(allText).contains("₹340 in pending")
    }
}
