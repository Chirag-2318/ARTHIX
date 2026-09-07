package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chirag.arthix.data.model.MoneyLogCategory
import com.chirag.arthix.data.model.MoneyLogDateMode

@Entity(tableName = "money_log")
data class MoneyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: MoneyLogCategory,
    val customCategoryLabel: String?, // For when category == OTHER
    val description: String,
    val amount: Double,
    val dateMode: MoneyLogDateMode,
    val exactDateMillis: Long?,
    val rangeStartMillis: Long?,
    val rangeEndMillis: Long?,
    val approximateMonthYear: String?,
    val note: String?,
    val createdAt: Long
)
