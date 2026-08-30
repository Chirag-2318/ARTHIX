package com.chirag.arthix.report

import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.repository.ReportRepository
import com.chirag.arthix.report.engine.GroundingValidator
import com.chirag.arthix.report.engine.ReportComputationEngine
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.ComputedSuggestion
import com.chirag.arthix.report.model.ReportPeriod
import com.chirag.arthix.report.phrasing.OnDeviceMediaPipeEngine
import com.chirag.arthix.report.phrasing.TemplatePhrasingEngine
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ReportGeneratorTest {

    private lateinit var computationEngine: ReportComputationEngine
    private lateinit var templateEngine: TemplatePhrasingEngine
    private lateinit var phrasingEngine: OnDeviceMediaPipeEngine
    private lateinit var validator: GroundingValidator
    private lateinit var fakeReportRepository: FakeReportRepository
    private val gson = Gson()

    private lateinit var generator: ReportGenerator

    @Before
    fun setup() {
        computationEngine = mock(ReportComputationEngine::class.java)
        validator = GroundingValidator()
        templateEngine = TemplatePhrasingEngine()
        phrasingEngine = OnDeviceMediaPipeEngine(templateEngine, validator)
        fakeReportRepository = FakeReportRepository()

        generator = ReportGenerator(
            computationEngine = computationEngine,
            phrasingEngine = phrasingEngine,
            templateEngine = templateEngine,
            validator = validator,
            reportRepository = fakeReportRepository,
            gson = gson,
        )
    }

    @Test
    fun generateAndSaveReport_validComputation_savesAndReturnsReport() = runTest {
        val period = ReportPeriod.currentWeek()
        val computedData = ComputedReportData(
            period = period,
            categoryBreakdown = mapOf("food" to 400_000L),
            totalInflowPaise = 100_000L,
            totalOutflowPaise = 400_000L,
            netFlowPaise = -300_000L,
            uncategorizedTotalPaise = 0L,
            projectedTotalPaise = 400_000L,
            projectedSavingsPaise = 80_000L,
            noPriorData = false,
            suggestion = ComputedSuggestion(
                category = "food",
                currentSpendPaise = 400_000L,
                baselineSpendPaise = 300_000L,
                percentageAboveBaseline = 33,
                targetReductionPercentage = 20,
                projectedSavingsPaise = 80_000L,
            )
        )

        `when`(computationEngine.compute(period)).thenReturn(computedData)

        val report = generator.generateAndSaveReport(period)

        assertThat(report.id).isEqualTo(42L)
        assertThat(report.netFlowPaise).isEqualTo(-300_000L)
        assertThat(report.projectedSavingsPaise).isEqualTo(80_000L)

        assertThat(fakeReportRepository.savedReports).hasSize(1)
        val saved = fakeReportRepository.savedReports.first()
        assertThat(saved.netFlowPaise).isEqualTo(-300_000L)
        assertThat(saved.suggestionsJson).contains("Food")
    }

    private class FakeReportRepository : ReportRepository {
        val savedReports = mutableListOf<ReportEntity>()

        override suspend fun save(report: ReportEntity): Long {
            savedReports.add(report)
            return 42L
        }

        override suspend fun getForPeriod(start: Long, end: Long): ReportEntity? = null

        override fun observeAll(): Flow<List<ReportEntity>> = flowOf(savedReports)
    }
}
