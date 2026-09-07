package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.MoneyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoneyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MoneyLogEntity>)

    @Update
    suspend fun update(entry: MoneyLogEntity)

    @Delete
    suspend fun delete(entry: MoneyLogEntity)

    @Query("SELECT * FROM money_log ORDER BY createdAt DESC")
    fun getAllEntriesFlow(): Flow<List<MoneyLogEntity>>

    @Query("SELECT * FROM money_log ORDER BY createdAt DESC")
    suspend fun getAllEntries(): List<MoneyLogEntity>
    
    @Query("SELECT * FROM money_log WHERE id = :id")
    suspend fun getEntryById(id: Long): MoneyLogEntity?
}
