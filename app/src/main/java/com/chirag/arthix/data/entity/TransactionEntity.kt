package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus

/**
 * Core transaction record.
 *
 * Foreign keys to [PendingCaptureEntity] and [PendingNotificationEntity] are
 * intentionally soft (plain String? columns, not Room @ForeignKey) because
 * pending rows may be cleaned up by housekeeping while transaction rows must
 * survive indefinitely. A hard FK with CASCADE/RESTRICT would either
 * orphan-delete history or block legitimate cleanup — neither is correct.
 *
 * [amountPaise] is always an integer Long (EC-46: never Float/Double).
 * Nullable only while [status] == [TransactionStatus.AWAITING_AMOUNT].
 *
 * [timestamp] is epoch millis (wall clock) — the transaction's *effective*
 * time for report bucketing/display. For reconciliation matching, the
 * monotonic time on [PendingCaptureEntity]/[PendingNotificationEntity] is
 * used instead (per techstack §1.1).
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["timestamp", "category"])  // report range+breakdown queries
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val amountPaise: Long?,
    val payee: String?,
    val category: String?,

    val timestamp: Long,

    val direction: Direction,
    val source: CaptureSource,
    val status: TransactionStatus,

    val sourceCaptureId: String?,       // soft FK -> PendingCapture.id (EC-23)
    val sourceNotificationId: String?,  // soft FK -> PendingNotification.id (EC-23)

    val confidenceFlag: ConfidenceFlag,

    val createdAt: Long                 // epoch millis, row-insert time
)
