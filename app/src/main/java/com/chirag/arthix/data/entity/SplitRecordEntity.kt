package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chirag.arthix.data.model.AmountLock
import com.chirag.arthix.data.model.SplitConfirmedVia

/**
 * Parent record for a bill split on a transaction.
 *
 * The brief's single `share_per_person` field is under-specified for the
 * "editable" requirement (FR-6: default even split, editable per person).
 * Phase 0 splits this into a parent record plus a child table
 * ([SplitParticipantEntity]) so uneven, edited splits are representable
 * without a schema change later.
 *
 * [amountLock] defaults to [AmountLock.LIVE] (EC-40) — the split amount
 * tracks the parent transaction. When [AmountLock.LOCKED_AT_CREATION],
 * [lockedAmountPaise] captures the frozen value.
 */
@Entity(
    tableName = "split_records",
    indices = [Index(value = ["transactionId"])]
)
data class SplitRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,                    // soft FK -> TransactionEntity.id
    val confirmedVia: SplitConfirmedVia,
    val amountLock: AmountLock = AmountLock.LIVE,
    val lockedAmountPaise: Long?,               // only populated when amountLock == LOCKED_AT_CREATION
    val createdAt: Long,
    val recalculatedFlag: Boolean = false       // Phase 6 (EC-40) - true if a live recalculation has occurred
)
