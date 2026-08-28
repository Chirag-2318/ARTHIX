package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity

@Dao
interface SplitDao {

    @Insert
    suspend fun insertSplit(s: SplitRecordEntity): Long

    @Insert
    suspend fun insertParticipants(p: List<SplitParticipantEntity>)

    @Update
    suspend fun updateSplit(s: SplitRecordEntity)

    @Query("SELECT * FROM split_records WHERE transactionId = :txnId")
    suspend fun getSplitsForTransaction(txnId: Long): List<SplitRecordEntity>

    @Query("SELECT * FROM split_participants WHERE splitRecordId = :splitId")
    suspend fun getParticipants(splitId: Long): List<SplitParticipantEntity>
}
