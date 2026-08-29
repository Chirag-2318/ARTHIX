package com.chirag.arthix.data.repository.impl

import androidx.room.withTransaction
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.repository.SplitRepository
import javax.inject.Inject

/**
 * Room-DAO-backed implementation of [SplitRepository].
 *
 * [createSplit] uses Room's @Transaction wrapper to atomically insert
 * the parent SplitRecord and all child SplitParticipant rows, wiring
 * up the auto-generated splitRecordId.
 */
class SplitRepositoryImpl @Inject constructor(
    private val dao: SplitDao,
    private val database: ArthixDatabase,
) : SplitRepository {

    override suspend fun createSplit(
        split: SplitRecordEntity,
        participants: List<SplitParticipantEntity>
    ) {
        database.withTransaction {
            val splitId = dao.insertSplit(split)
            val wiredParticipants = participants.map { it.copy(splitRecordId = splitId) }
            dao.insertParticipants(wiredParticipants)
        }
    }

    override suspend fun getSplitsForTransaction(
        txnId: Long
    ): List<Pair<SplitRecordEntity, List<SplitParticipantEntity>>> {
        val splits = dao.getSplitsForTransaction(txnId)
        return splits.map { split ->
            val participants = dao.getParticipants(split.id)
            split to participants
        }
    }
}
