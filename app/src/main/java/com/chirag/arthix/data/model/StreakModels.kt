package com.chirag.arthix.data.model

enum class DayStatus { HELD, OVER, COMPENSATED, FUTURE, TODAY_EMPTY }

data class StreakDay(
    val dayOfMonth: Int,
    val status: DayStatus,
    val spent: Long, // Note: using Long for Paise
    val cap: Long // Note: using Long for Paise
)
