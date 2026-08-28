package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity

/**
 * Repository contract for bill-split operations.
 *
 * FROZEN as of Phase 0. [createSplit] wraps the insert of both a
 * [SplitRecordEntity] and its [SplitParticipantEntity] rows in a single
 * @Transaction for atomicity (PRD §5).
 */
interface SplitRepository {

    /**
     * Atomically insert a split record and all its participants.
     * The [SplitParticipantEntity.splitRecordId] fields are set by the
     * implementation using the auto-generated split ID.
     */
    suspend fun createSplit(
        split: SplitRecordEntity,
        participants: List<SplitParticipantEntity>
    )

    /**
     * Get all splits for a transaction, each paired with its participants.
     */
    suspend fun getSplitsForTransaction(
        txnId: Long
    ): List<Pair<SplitRecordEntity, List<SplitParticipantEntity>>>
}
