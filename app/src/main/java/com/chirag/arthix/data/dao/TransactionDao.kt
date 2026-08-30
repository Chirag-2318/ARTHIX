package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.ConfidenceFlag
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

    // ── Phase 2 reconciliation additions ───────────────────────────────

    /** Find a transaction by its source capture ID (for timeout/discard status updates). */
    @Query("SELECT * FROM transactions WHERE sourceCaptureId = :captureId LIMIT 1")
    suspend fun findBySourceCaptureId(captureId: String): TransactionEntity?

    /** Atomically update transaction status by source capture ID (PRD §7.6/§7.7). */
    @Query("UPDATE transactions SET status = :newStatus WHERE sourceCaptureId = :captureId")
    suspend fun updateStatusBySourceCaptureId(captureId: String, newStatus: TransactionStatus)

    /**
     * Recent outflow transactions for dedup checking (PRD §6) and refund netting (PRD §5.3).
     * Returns confirmed outflows within a time window.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE direction = 'OUTFLOW'
              AND status != 'DISCARDED'
              AND timestamp >= :minTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentOutflows(minTimestamp: Long): List<TransactionEntity>

    /**
     * Refund netting: find a recent outflow matching amount + payee (PRD §5.3).
     * Caller applies payee similarity check in-memory.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE direction = 'OUTFLOW'
              AND status != 'DISCARDED'
              AND amountPaise = :amountPaise
              AND timestamp >= :minTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun findRecentOutflowByAmount(amountPaise: Long, minTimestamp: Long): List<TransactionEntity>

    /** Phase 3: update status by row ID (for discard and edit-save transitions). */
    @Query("UPDATE transactions SET status = :newStatus WHERE id = :id")
    suspend fun updateStatusById(id: Long, newStatus: TransactionStatus)

    /** Phase 3: atomic update of status + confidence flag on manual edit save. */
    @Query("UPDATE transactions SET status = :newStatus, confidenceFlag = :flag WHERE id = :id")
    suspend fun updateStatusAndFlag(id: Long, newStatus: TransactionStatus, flag: ConfidenceFlag)

    /**
     * Phase 4 Step 2 (IdleDetector): returns the count of transactions that
     * the voice follow-up can resolve — AWAITING_AMOUNT or AWAITING_CATEGORY.
     */
    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE status IN ('AWAITING_AMOUNT', 'AWAITING_CATEGORY')
    """)
    suspend fun countPendingVoiceRecords(): Int

    /**
     * Phase 4 Step 2 (VoiceFollowUpSession): returns pending transactions for voice follow-up
     */
    @Query("""
        SELECT * FROM transactions
        WHERE status IN ('AWAITING_AMOUNT', 'AWAITING_CATEGORY')
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getPendingVoiceRecords(limit: Int): List<TransactionEntity>
}
