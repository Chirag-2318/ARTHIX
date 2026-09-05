package com.chirag.arthix.data.model

/**
 * Strategy used by the on-device AI Goal Planner to achieve savings.
 */
enum class GoalPlanType {
    /**
     * Target a specific high-spend discretionary category (e.g. Food, Shopping).
     */
    CATEGORY_REDUCTION,

    /**
     * Flat weekly set-aside amount when spending history is thin or diversified.
     */
    FLAT_SAVINGS
}

/**
 * Lifecycle status of a savings goal.
 */
enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED
}
