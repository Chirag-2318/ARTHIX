package com.chirag.arthix.domain.goal

import com.chirag.arthix.data.entity.GoalEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.GoalPlanType
import com.chirag.arthix.data.model.GoalStatus
import com.chirag.arthix.data.model.TransactionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max

data class GeneratedGoalPlan(
    val planType: GoalPlanType,
    val targetCategory: String?,
    val weeklyTargetSavingsPaise: Long,
    val baselineWeeklySpendPaise: Long,
    val estimatedDaysToTarget: Int,
    val recommendationHeadline: String,
    val recommendationDetail: String,
)

/**
 * On-device rule-based AI Goal Plan Generator.
 *
 * Evaluates local transaction history to produce realistic, actionable
 * savings suggestions without cloud APIs or balance access.
 */
@Singleton
class GoalPlanGenerator @Inject constructor() {

    companion object {
        private val DISCRETIONARY_CATEGORIES = setOf(
            "food", "dining", "shopping", "travel", "entertainment",
            "cafe", "lifestyle", "groceries", "other"
        )
        private const val MIN_CATEGORY_CUT_WEEKLY_PAISE = 150_00L // ₹150 / week
        private const val DEFAULT_FLAT_WEEKS = 8
        private const val MAX_RECOMMENDED_WEEKS = 24
    }

    /**
     * Generate an on-device savings plan based on target amount and historical transactions.
     */
    fun generatePlan(
        targetAmountPaise: Long,
        transactions: List<TransactionEntity>,
    ): GeneratedGoalPlan {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)

        // Filter valid outflow transactions from the last 30 days
        val recentOutflows = transactions.filter { txn ->
            txn.direction == Direction.OUTFLOW &&
                txn.status != TransactionStatus.DISCARDED &&
                txn.timestamp >= thirtyDaysAgo &&
                txn.amountPaise != null &&
                txn.amountPaise > 0L
        }

        // Check if there is enough history (at least 3 transactions in 30 days)
        if (recentOutflows.size >= 3) {
            val daysSpan = max(7L, (now - recentOutflows.minOf { it.timestamp }) / (24 * 60 * 60 * 1000L))
            val weeksObserved = max(1.0, daysSpan / 7.0)

            // Group by category and compute weekly average spend
            val categoryWeeklySpend = recentOutflows
                .groupBy { (it.category ?: "other").lowercase() }
                .mapValues { (_, txns) ->
                    (txns.sumOf { it.amountPaise ?: 0L } / weeksObserved).toLong()
                }

            // Find best candidate discretionary category with highest weekly spend
            val bestDiscretionary = categoryWeeklySpend
                .filter { (cat, spend) ->
                    DISCRETIONARY_CATEGORIES.any { cat.contains(it) } && spend >= 500_00L // at least ₹500/wk
                }
                .maxByOrNull { it.value }

            if (bestDiscretionary != null) {
                val categoryName = bestDiscretionary.key.replaceFirstChar { it.uppercase() }
                val baselineWeekly = bestDiscretionary.value

                // Recommend a 20% cut (or 25% if weekly spend is high)
                val cutPercent = if (baselineWeekly >= 2000_00L) 0.25 else 0.20
                val weeklySavings = (baselineWeekly * cutPercent).toLong()

                if (weeklySavings >= MIN_CATEGORY_CUT_WEEKLY_PAISE) {
                    val weeksNeeded = max(1, ceil(targetAmountPaise.toDouble() / weeklySavings.toDouble()).toInt())

                    if (weeksNeeded <= MAX_RECOMMENDED_WEEKS) {
                        val estimatedDays = weeksNeeded * 7
                        val targetRupees = targetAmountPaise / 100
                        val weeklySavingsRupees = weeklySavings / 100
                        val baselineRupees = baselineWeekly / 100

                        return GeneratedGoalPlan(
                            planType = GoalPlanType.CATEGORY_REDUCTION,
                            targetCategory = categoryName,
                            weeklyTargetSavingsPaise = weeklySavings,
                            baselineWeeklySpendPaise = baselineWeekly,
                            estimatedDaysToTarget = estimatedDays,
                            recommendationHeadline = "Cut $categoryName by ₹$weeklySavingsRupees/week",
                            recommendationDetail = "You typically spend ~₹$baselineRupees/wk on $categoryName. Trimming this by ${(cutPercent * 100).toInt()}% reaches your ₹$targetRupees goal in ~$weeksNeeded weeks."
                        )
                    }
                }
            }
        }

        // Fallback to flat savings suggestion over 8 to 12 weeks
        val targetRupees = targetAmountPaise / 100
        val weeks = if (targetRupees > 10_000) 12 else DEFAULT_FLAT_WEEKS
        val weeklySavings = max(100_00L, targetAmountPaise / weeks)
        val estimatedDays = weeks * 7
        val weeklySavingsRupees = weeklySavings / 100

        return GeneratedGoalPlan(
            planType = GoalPlanType.FLAT_SAVINGS,
            targetCategory = null,
            weeklyTargetSavingsPaise = weeklySavings,
            baselineWeeklySpendPaise = 0L,
            estimatedDaysToTarget = estimatedDays,
            recommendationHeadline = "Save ₹$weeklySavingsRupees/week",
            recommendationDetail = "Setting aside a steady ₹$weeklySavingsRupees each week reaches your ₹$targetRupees goal in $weeks weeks (~$estimatedDays days)."
        )
    }

    /**
     * Converts a GeneratedGoalPlan into a ready-to-insert GoalEntity.
     */
    fun createEntity(
        title: String,
        targetAmountPaise: Long,
        plan: GeneratedGoalPlan,
        initialSavedPaise: Long = 0L,
    ): GoalEntity {
        return GoalEntity(
            title = title.trim(),
            targetAmountPaise = targetAmountPaise,
            savedAmountPaise = initialSavedPaise,
            planType = plan.planType,
            targetCategory = plan.targetCategory,
            weeklyTargetSavingsPaise = plan.weeklyTargetSavingsPaise,
            baselineWeeklySpendPaise = plan.baselineWeeklySpendPaise,
            estimatedDaysToTarget = plan.estimatedDaysToTarget,
            createdAt = System.currentTimeMillis(),
            status = if (initialSavedPaise >= targetAmountPaise) GoalStatus.COMPLETED else GoalStatus.ACTIVE,
            notes = plan.recommendationHeadline
        )
    }
}
