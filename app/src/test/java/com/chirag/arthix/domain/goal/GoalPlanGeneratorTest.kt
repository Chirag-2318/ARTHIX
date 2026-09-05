package com.chirag.arthix.domain.goal

import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.GoalPlanType
import com.chirag.arthix.data.model.GoalStatus
import com.chirag.arthix.data.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoalPlanGeneratorTest {

    private lateinit var generator: GoalPlanGenerator

    @Before
    fun setUp() {
        generator = GoalPlanGenerator()
    }

    private fun testTxn(
        id: Long,
        amountPaise: Long,
        payee: String,
        category: String,
        timestamp: Long,
        direction: Direction = Direction.OUTFLOW,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            payee = payee,
            category = category,
            timestamp = timestamp,
            direction = direction,
            source = CaptureSource.MANUAL,
            status = status,
            sourceCaptureId = null,
            sourceNotificationId = null,
            confidenceFlag = ConfidenceFlag.CLEAN,
            createdAt = timestamp
        )
    }

    @Test
    fun generatePlan_withRichFoodSpending_suggestsCategoryReduction() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        // 8 transactions of ₹600 each on Food over the last 14 days
        val txns = (1..8).map { i ->
            testTxn(
                id = i.toLong(),
                amountPaise = 600_00L, // ₹600
                payee = "Zomato",
                category = "Food",
                timestamp = now - (i * oneDay)
            )
        }

        val plan = generator.generatePlan(
            targetAmountPaise = 2000_00L, // ₹2,000 goal
            transactions = txns
        )

        assertEquals(GoalPlanType.CATEGORY_REDUCTION, plan.planType)
        assertEquals("Food", plan.targetCategory)
        assertTrue(plan.weeklyTargetSavingsPaise >= 150_00L)
        assertTrue(plan.estimatedDaysToTarget in 7..120)
        assertTrue(plan.recommendationHeadline.contains("Food"))
    }

    @Test
    fun generatePlan_withThinHistory_fallsBackToFlatSavings() {
        val txns = listOf(
            testTxn(
                id = 1L,
                amountPaise = 100_00L,
                payee = "Chai",
                category = "Food",
                timestamp = System.currentTimeMillis()
            )
        )

        val plan = generator.generatePlan(
            targetAmountPaise = 2000_00L, // ₹2,000 goal
            transactions = txns
        )

        assertEquals(GoalPlanType.FLAT_SAVINGS, plan.planType)
        assertNull(plan.targetCategory)
        assertEquals(250_00L, plan.weeklyTargetSavingsPaise) // ₹2,000 / 8 weeks = ₹250/wk
        assertEquals(56, plan.estimatedDaysToTarget) // 8 * 7 = 56 days
        assertTrue(plan.recommendationHeadline.contains("Save"))
    }

    @Test
    fun createEntity_setsCorrectInitialAttributes() {
        val plan = GeneratedGoalPlan(
            planType = GoalPlanType.CATEGORY_REDUCTION,
            targetCategory = "Shopping",
            weeklyTargetSavingsPaise = 300_00L,
            baselineWeeklySpendPaise = 1500_00L,
            estimatedDaysToTarget = 35,
            recommendationHeadline = "Cut Shopping by ₹300/week",
            recommendationDetail = "Detail text"
        )

        val entity = generator.createEntity(
            title = "Noise Cancelling Headphones",
            targetAmountPaise = 1500_00L,
            plan = plan,
            initialSavedPaise = 300_00L
        )

        assertEquals("Noise Cancelling Headphones", entity.title)
        assertEquals(1500_00L, entity.targetAmountPaise)
        assertEquals(300_00L, entity.savedAmountPaise)
        assertEquals(0.2f, entity.progressFraction, 0.001f)
        assertEquals(20, entity.progressPercent)
        assertFalse(entity.isCompleted)
        assertEquals(GoalStatus.ACTIVE, entity.status)
        assertEquals("Shopping", entity.targetCategory)
    }
}
