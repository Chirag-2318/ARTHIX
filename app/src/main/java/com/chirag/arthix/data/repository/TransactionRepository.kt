package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for transaction CRUD and queries.
 *
 * FROZEN as of Phase 0 — method signatures must not change without
 * renegotiating with Chirag. Add new methods if genuinely needed;
 * do not change existing signatures.
 *
 * Implementations are written by whichever phase first needs them.
 * No phase should call a DAO directly from ViewModel/UI-facing code;
 * all access goes through this interface.
 */
interface TransactionRepository {

    /** Insert a new transaction. Returns the auto-generated row ID. */
    suspend fun commit(txn: TransactionEntity): Long

    /** Update an existing transaction (matched by [TransactionEntity.id]). */
    suspend fun update(txn: TransactionEntity)

    /**
     * Discard a transaction — sets status to DISCARDED rather than deleting.
     * The row survives for audit/history, but is excluded from report queries.
     */
    suspend fun discard(id: Long)

    suspend fun getById(id: Long): TransactionEntity?

    /** Reactive stream of all transactions, newest first. */
    fun observeHistory(): Flow<List<TransactionEntity>>

    /** All non-discarded transactions within a date range (for report generation). */
    suspend fun getInRange(start: Long, end: Long): List<TransactionEntity>

    /** Category-grouped sums within a date range (for report breakdown). */
    suspend fun getCategorySums(start: Long, end: Long): List<CategorySum>

    /** Sum of amounts for pending/unlabeled transactions (EC-44). */
    suspend fun getUncategorizedTotal(start: Long, end: Long): Long

    /**
     * Returns true if at least one AWAITING_AMOUNT or AWAITING_CATEGORY transaction
     * exists. Used by [IdleDetector] to skip the voice follow-up when there's nothing
     * to resolve (Phase 4 Step 2).
     */
    suspend fun hasPendingVoiceRecords(): Boolean

    /**
     * Returns pending transactions for voice follow-up (AWAITING_AMOUNT or AWAITING_CATEGORY).
     */
    suspend fun getPendingVoiceRecords(limit: Int): List<TransactionEntity>
}
