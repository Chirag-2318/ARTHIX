package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chirag.arthix.data.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert
    suspend fun insert(r: ReportEntity): Long

    @Query("SELECT * FROM reports WHERE periodStart = :start AND periodEnd = :end LIMIT 1")
    suspend fun getForPeriod(start: Long, end: Long): ReportEntity?

    @Query("SELECT * FROM reports ORDER BY generatedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>
}
