package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 ELSE 2 END, createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE goals SET savedAmountPaise = MIN(targetAmountPaise, savedAmountPaise + :amountPaise) WHERE id = :id")
    suspend fun addSavings(id: Long, amountPaise: Long)

    @Query("UPDATE goals SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: Long)
}
