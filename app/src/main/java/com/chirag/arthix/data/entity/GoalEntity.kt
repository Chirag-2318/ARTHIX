package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chirag.arthix.data.model.GoalPlanType
import com.chirag.arthix.data.model.GoalStatus

/**
 * Persisted savings goal computed and tracked on-device.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Name of the goal or item (e.g. "Mechanical Keyboard", "Goa Trip", "Emergency Fund") */
    val title: String,

    /** Target amount in paise (e.g. 2,000 INR = 200,000 paise) */
    val targetAmountPaise: Long,

    /** Amount saved so far towards this goal in paise */
    val savedAmountPaise: Long = 0L,

    /** Strategy: category cut vs flat set-aside */
    val planType: GoalPlanType = GoalPlanType.FLAT_SAVINGS,

    /** The category to cut spending from if planType is CATEGORY_REDUCTION (e.g. "Food") */
    val targetCategory: String? = null,

    /** Recommended savings amount per week in paise */
    val weeklyTargetSavingsPaise: Long = 0L,

    /** Baseline weekly spending in targetCategory at the time goal was created */
    val baselineWeeklySpendPaise: Long = 0L,

    /** Estimated duration to achieve target at recommended pace (in days) */
    val estimatedDaysToTarget: Int = 30,

    /** Timestamp when goal was created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Current state: ACTIVE, COMPLETED, PAUSED */
    val status: GoalStatus = GoalStatus.ACTIVE,

    /** Optional user note */
    val notes: String? = null
) {
    val progressFraction: Float
        get() = if (targetAmountPaise > 0L) {
            (savedAmountPaise.toFloat() / targetAmountPaise.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val isCompleted: Boolean
        get() = status == GoalStatus.COMPLETED || savedAmountPaise >= targetAmountPaise
}
