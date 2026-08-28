package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A pending shake-capture event awaiting reconciliation with a notification.
 *
 * Room-persisted (not held in memory, EC-51) so a process death
 * mid-reconciliation does not silently lose a pending shake.
 *
 * [timestampMonotonic] is [SystemClock.elapsedRealtime()] at shake detection —
 * monotonic clock for all matching math, never wall clock (EC-19).
 *
 * [active] becomes false once matched OR once timed out and routed to
 * AWAITING_AMOUNT (EC-17: must leave the active matching pool at the exact
 * instant it moves to awaiting_*).
 */
@Entity(
    tableName = "pending_captures",
    indices = [
        Index(value = ["timestampMonotonic"]),
        Index(value = ["matched"]),
        Index(value = ["active"]),
        Index(value = ["matched", "active", "timestampMonotonic"])  // reconciliation hot query
    ]
)
data class PendingCaptureEntity(
    @PrimaryKey val id: String,                   // UUID, generated at shake-detection time

    val timestampMonotonic: Long,                 // SystemClock.elapsedRealtime()
    val matched: Boolean = false,
    val active: Boolean = true,

    val category: String?,                        // set if user tapped a chip before notification

    val createdAt: Long                           // epoch millis, for debugging/ordering only
)
