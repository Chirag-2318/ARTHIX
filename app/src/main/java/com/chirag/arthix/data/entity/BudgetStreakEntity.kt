package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_streaks")
data class BudgetStreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val label: String?,
    val monthlyAmountPaise: Long,
    val daysInPeriod: Int,
    val startDate: Long,
    val distributionMode: String,
    val active: Boolean = true,
    val createdAt: Long
)
