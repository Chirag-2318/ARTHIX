package com.chirag.arthix.data.repository.impl

import com.chirag.arthix.data.dao.GoalDao
import com.chirag.arthix.data.entity.GoalEntity
import com.chirag.arthix.data.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()

    override fun observeActive(): Flow<List<GoalEntity>> = goalDao.observeActive()

    override suspend fun getById(id: Long): GoalEntity? = goalDao.getById(id)

    override suspend fun createGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    override suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    override suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)

    override suspend fun addSavings(id: Long, amountPaise: Long) = goalDao.addSavings(id, amountPaise)

    override suspend fun markCompleted(id: Long) = goalDao.markCompleted(id)
}
