package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(txn: TransactionEntity): Long

    @Update
    suspend fun update(txn: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    /** Phase 3: history list — reactive stream ordered by newest first. */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllOrderedByTime(): Flow<List<TransactionEntity>>

    /** Phase 5: report input — all non-discarded transactions in a date range. */
    @Query("""
        SELECT * FROM transactions
        WHERE timestamp BETWEEN :start AND :end AND status != 'DISCARDED'
        ORDER BY timestamp ASC
    """)
    suspend fun getInRange(start: Long, end: Long): List<TransactionEntity>

    /**
     * Phase 5: grouped category sums for report generation.
     * Uses the (timestamp, category) composite index.
     */
    @Query("""
        SELECT category, SUM(amountPaise) as total
        FROM transactions
        WHERE timestamp BETWEEN :start AND :end
              AND status != 'DISCARDED' AND amountPaise IS NOT NULL
        GROUP BY category
    """)
    suspend fun getCategorySums(start: Long, end: Long): List<CategorySum>

    @Query("SELECT * FROM transactions WHERE status = :status")
    suspend fun getByStatus(status: TransactionStatus): List<TransactionEntity>

    /**
     * EC-44: pending/unlabeled amounts stay visible in the report total.
     * Sum of amounts for transactions still in a pending status within a period.
     */
    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE status IN ('AWAITING_MATCH','AWAITING_CATEGORY','AWAITING_AMOUNT')
              AND timestamp BETWEEN :start AND :end
    """)
    suspend fun getUncategorizedTotal(start: Long, end: Long): Long
}
