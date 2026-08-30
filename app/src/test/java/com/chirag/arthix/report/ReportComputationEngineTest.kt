package com.chirag.arthix.report

import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.report.engine.ProjectionAnchor
import com.chirag.arthix.report.engine.ReportComputationEngine
import com.chirag.arthix.report.engine.SuggestionRuleEngine
import com.chirag.arthix.report.model.ReportPeriod
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ReportComputationEngineTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var engine: ReportComputationEngine
    private val projectionAnchor = ProjectionAnchor()
    private val suggestionRuleEngine = SuggestionRuleEngine()

    @Before
    fun setup() {
        transactionDao = mock(TransactionDao::class.java)
        engine = ReportComputationEngine(
            transactionDao = transactionDao,
            projectionAnchor = projectionAnchor,
            suggestionRuleEngine = suggestionRuleEngine,
        )
    }

    @Test
    fun compute_datasetA_correctCategorySumsAndNetFlow() = runTest {
        // Dataset A: Hand-computed benchmark dataset
        // Period: 1000L to 8000L
        val period = ReportPeriod.custom(
            startMs = 1000L,
            endMs = 8000L,
            prevStartMs = 0L,
            prevEndMs = 1000L,
            label = "Test Week A",
            elapsedDays = 7,
            totalDays = 7,
        )

        val currentTxns = listOf(
            createTxn(id = 1, amount = 50_000L, category = "food", direction = Direction.OUTFLOW),       // ₹500 food
            createTxn(id = 2, amount = 30_000L, category = "food", direction = Direction.OUTFLOW),       // ₹300 food
            createTxn(id = 3, amount = 120_000L, category = "travel", direction = Direction.OUTFLOW),   // ₹1,200 travel
            createTxn(id = 4, amount = 200_000L, category = "income", direction = Direction.INFLOW),    // ₹2,000 inflow
        )

        val prevTxns = listOf(
            createTxn(id = 5, amount = 40_000L, category = "food", direction = Direction.OUTFLOW),       // ₹400 food
            createTxn(id = 6, amount = 60_000L, category = "travel", direction = Direction.OUTFLOW),     // ₹600 travel
        )

        `when`(transactionDao.getInRange(period.startMs, period.endMs)).thenReturn(currentTxns)
        `when`(transactionDao.getInRange(period.prevStartMs, period.prevEndMs)).thenReturn(prevTxns)
        `when`(transactionDao.getUncategorizedTotal(period.startMs, period.endMs)).thenReturn(0L)

        val report = engine.compute(period)

        // Hand-verified assertions:
        // Food: 50,000 + 30,000 = 80,000 paise (₹800)
        // Travel: 120,000 paise (₹1,200)
        // Total Outflow: 200,000 paise (₹2,000)
        // Total Inflow: 200,000 paise (₹2,000)
        // Net Flow: 0 paise (₹0)
        assertThat(report.categoryBreakdown["food"]).isEqualTo(80_000L)
        assertThat(report.categoryBreakdown["travel"]).isEqualTo(120_000L)
        assertThat(report.totalOutflowPaise).isEqualTo(200_000L)
        assertThat(report.totalInflowPaise).isEqualTo(200_000L)
        assertThat(report.netFlowPaise).isEqualTo(0L)
        assertThat(report.noPriorData).isFalse()
    }

    @Test
    fun compute_datasetB_withUncategorized_includedInTotalSpend() = runTest {
        // Dataset B with EC-44 pending uncategorized amounts
        val period = ReportPeriod.custom(
            startMs = 1000L,
            endMs = 8000L,
            prevStartMs = 0L,
            prevEndMs = 1000L,
            label = "Test Week B",
            elapsedDays = 7,
            totalDays = 7,
        )

        val currentTxns = listOf(
            createTxn(id = 1, amount = 150_000L, category = "shopping", direction = Direction.OUTFLOW), // ₹1,500
        )

        `when`(transactionDao.getInRange(period.startMs, period.endMs)).thenReturn(currentTxns)
        `when`(transactionDao.getInRange(period.prevStartMs, period.prevEndMs)).thenReturn(emptyList())
        // EC-44: ₹340 (34,000 paise) pending in queue
        `when`(transactionDao.getUncategorizedTotal(period.startMs, period.endMs)).thenReturn(34_000L)

        val report = engine.compute(period)

        // Hand-verified:
        // shopping = ₹1,500 (150,000)
        // uncategorized = ₹340 (34,000)
        // Total outflow MUST include uncategorized = ₹1,840 (184,000 paise)
        assertThat(report.categoryBreakdown["shopping"]).isEqualTo(150_000L)
        assertThat(report.uncategorizedTotalPaise).isEqualTo(34_000L)
        assertThat(report.totalOutflowPaise).isEqualTo(184_000L)
        assertThat(report.noPriorData).isTrue() // EC-45 zero baseline
    }

    private fun createTxn(
        id: Long,
        amount: Long,
        category: String,
        direction: Direction,
    ) = TransactionEntity(
        id = id,
        amountPaise = amount,
        payee = "Vendor $id",
        category = category,
        timestamp = 2000L,
        direction = direction,
        source = CaptureSource.MANUAL,
        status = TransactionStatus.CONFIRMED,
        sourceCaptureId = null,
        sourceNotificationId = null,
        confidenceFlag = ConfidenceFlag.CLEAN,
        createdAt = 2000L,
    )
}
