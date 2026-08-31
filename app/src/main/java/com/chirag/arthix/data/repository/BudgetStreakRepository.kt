package com.chirag.arthix.data.repository

import com.chirag.arthix.data.dao.BudgetStreakDao
import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.entity.StreakDailyCapEntity
import com.chirag.arthix.data.model.DayStatus
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.StreakDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.TimeZone
import kotlin.math.max

interface BudgetStreakRepository {
    
    suspend fun createStreak(
        streak: BudgetStreakEntity,
        caps: List<StreakDailyCapEntity>
    )

    fun getActiveStreaks(): Flow<List<BudgetStreakEntity>>

    suspend fun getStreakById(streakId: Long): BudgetStreakEntity?
    
    fun getStreakDays(streakId: Long): Flow<List<StreakDay>>
    
    suspend fun deactivateStreak(streakId: Long)
}

@Singleton
class BudgetStreakRepositoryImpl @Inject constructor(
    private val budgetStreakDao: BudgetStreakDao,
    private val transactionDao: TransactionDao
) : BudgetStreakRepository {

    override suspend fun createStreak(streak: BudgetStreakEntity, caps: List<StreakDailyCapEntity>) {
        val streakId = budgetStreakDao.insertStreak(streak)
        val capsWithId = caps.map { it.copy(streakId = streakId) }
        budgetStreakDao.insertCaps(capsWithId)
    }

    override fun getActiveStreaks(): Flow<List<BudgetStreakEntity>> {
        return budgetStreakDao.observeActiveStreaks()
    }

    override suspend fun getStreakById(streakId: Long): BudgetStreakEntity? {
        return budgetStreakDao.getStreakById(streakId)
    }

    override suspend fun deactivateStreak(streakId: Long) {
        budgetStreakDao.deactivateStreak(streakId)
    }

    override fun getStreakDays(streakId: Long): Flow<List<StreakDay>> = flow {
        val streak = budgetStreakDao.getStreakById(streakId) ?: return@flow
        
        // Use the DAO to observe transactions for the category.
        emitAll(transactionDao.observeByCategory(streak.category).map { allTxns ->
            val caps = budgetStreakDao.getCapsForStreak(streakId).associateBy { it.dayIndex }
            
            // Calculate today's epoch day using the local timezone
            val todayEpochDay = getLocalEpochDay(System.currentTimeMillis())
            
            // Pre-calculate daily spent totals
            val dailySpent = mutableMapOf<Int, Long>()
            for (txn in allTxns) {
                // Only count confirmed outflows
                if (txn.direction == Direction.OUTFLOW) {
                    val txnDay = getLocalEpochDay(txn.timestamp)
                    val dayIndex = (txnDay - streak.startDate + 1).toInt()
                    if (dayIndex in 1..streak.daysInPeriod) {
                        dailySpent[dayIndex] = (dailySpent[dayIndex] ?: 0L) + (txn.amountPaise ?: 0L)
                    }
                }
            }
            
            val streakDays = mutableListOf<StreakDay>()
            
            for (i in 1..streak.daysInPeriod) {
                val capEntity = caps[i] ?: StreakDailyCapEntity(streakId = streakId, dayIndex = i, capPaise = 0)
                val spent = dailySpent[i] ?: 0L
                val dateOfDay = streak.startDate + i - 1
                
                // The effective cap includes any trimming from previous overages
                val effectiveCap = max(0L, capEntity.capPaise - capEntity.compensationAdjustmentPaise)
                
                val status = when {
                    dateOfDay > todayEpochDay -> DayStatus.FUTURE
                    dateOfDay == todayEpochDay && spent == 0L -> DayStatus.TODAY_EMPTY
                    spent <= effectiveCap -> DayStatus.HELD
                    else -> DayStatus.OVER
                }
                
                streakDays.add(StreakDay(
                    dayOfMonth = i,
                    status = status,
                    spent = spent,
                    cap = effectiveCap
                ))
            }
            
            // Compensation pass: If a day is OVER, it spreads its overage to the future.
            val newCompensations = LongArray(streak.daysInPeriod + 1) { 0L }
            
            for (i in 1..streak.daysInPeriod) {
                val baseCap = caps[i]?.capPaise ?: 0L
                val spent = dailySpent[i] ?: 0L
                val effectiveCap = max(0L, baseCap - newCompensations[i])
                
                if (spent > effectiveCap) {
                    // It's OVER. Calculate overage.
                    var overage = spent - effectiveCap
                    
                    // Spread this overage across subsequent days
                    var j = i + 1
                    while (overage > 0 && j <= streak.daysInPeriod) {
                        val futureBaseCap = caps[j]?.capPaise ?: 0L
                        val futureSpent = dailySpent[j] ?: 0L
                        val futureAvailable = max(0L, futureBaseCap - futureSpent - newCompensations[j])
                        
                        if (futureAvailable > 0) {
                            val take = minOf(overage, futureAvailable)
                            newCompensations[j] += take
                            overage -= take
                        }
                        j++
                    }
                }
            }
            
            for (i in 1..streak.daysInPeriod) {
                val oldComp = caps[i]?.compensationAdjustmentPaise ?: 0L
                val newComp = newCompensations[i]
                if (oldComp != newComp) {
                    budgetStreakDao.updateCompensationAdjustment(streakId, i, newComp)
                }
            }
            
            var runningBalance = 0L
            for (i in 1..streak.daysInPeriod) {
                val day = streakDays[i-1]
                if (day.status != DayStatus.FUTURE && day.status != DayStatus.TODAY_EMPTY) {
                    runningBalance += (caps[i]?.capPaise ?: 0L) - day.spent
                    
                    if (day.status == DayStatus.OVER && runningBalance >= 0) {
                        streakDays[i-1] = day.copy(status = DayStatus.COMPENSATED)
                    }
                }
            }
            
            for (i in 1..streak.daysInPeriod) {
                val day = streakDays[i-1]
                val effectiveCap = max(0L, (caps[i]?.capPaise ?: 0L) - newCompensations[i])
                streakDays[i-1] = day.copy(cap = effectiveCap)
            }

            streakDays
        })
    }
    
    private fun getLocalEpochDay(millis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(millis)
        return (millis + offset) / 86400000L
    }
}
