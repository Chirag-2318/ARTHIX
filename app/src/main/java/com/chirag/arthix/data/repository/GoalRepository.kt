package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeAll(): Flow<List<GoalEntity>>
    fun observeActive(): Flow<List<GoalEntity>>
    suspend fun getById(id: Long): GoalEntity?
    suspend fun createGoal(goal: GoalEntity): Long
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(id: Long)
    suspend fun addSavings(id: Long, amountPaise: Long)
    suspend fun markCompleted(id: Long)
}
