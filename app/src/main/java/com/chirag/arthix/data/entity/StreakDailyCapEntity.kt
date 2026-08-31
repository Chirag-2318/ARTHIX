package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_daily_caps",
    foreignKeys = [
        ForeignKey(
            entity = BudgetStreakEntity::class,
            parentColumns = ["id"],
            childColumns = ["streakId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("streakId")
    ]
)
data class StreakDailyCapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val streakId: Long,
    val dayIndex: Int, // 1-based day within the streak period
    val capPaise: Long,
    val compensationAdjustmentPaise: Long = 0
)
