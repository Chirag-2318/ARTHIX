package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.entity.StreakDailyCapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetStreakDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStreak(streak: BudgetStreakEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCaps(caps: List<StreakDailyCapEntity>)

    @Query("SELECT * FROM budget_streaks WHERE active = 1 ORDER BY createdAt DESC")
    fun observeActiveStreaks(): Flow<List<BudgetStreakEntity>>

    @Query("SELECT * FROM budget_streaks WHERE id = :streakId")
    suspend fun getStreakById(streakId: Long): BudgetStreakEntity?

    @Query("SELECT * FROM streak_daily_caps WHERE streakId = :streakId ORDER BY dayIndex ASC")
    suspend fun getCapsForStreak(streakId: Long): List<StreakDailyCapEntity>

    @Query("UPDATE budget_streaks SET active = 0 WHERE id = :streakId")
    suspend fun deactivateStreak(streakId: Long)

    @Query("UPDATE streak_daily_caps SET compensationAdjustmentPaise = :adjustment WHERE streakId = :streakId AND dayIndex = :dayIndex")
    suspend fun updateCompensationAdjustment(streakId: Long, dayIndex: Int, adjustment: Long)
    
    @Query("UPDATE streak_daily_caps SET compensationAdjustmentPaise = 0 WHERE streakId = :streakId AND dayIndex > :startDayIndex")
    suspend fun clearCompensationAdjustmentsAfter(streakId: Long, startDayIndex: Int)
}
