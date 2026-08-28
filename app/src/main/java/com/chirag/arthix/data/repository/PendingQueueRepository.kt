package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity

/**
 * Repository contract for the reconciliation pending queues.
 *
 * FROZEN as of Phase 0. [markMatched] and [markExpired] deliberately live
 * here (not on the DAO) — they encapsulate the multi-step operation the
 * reconciliation engine (Phase 1) needs, wrapped in a single @Transaction
 * method once implemented.
 */
interface PendingQueueRepository {

    suspend fun addCapture(capture: PendingCaptureEntity)

    suspend fun addNotification(notification: PendingNotificationEntity)

    /** Reconciliation window query: active, unmatched captures within a time range. */
    suspend fun getUnmatchedCapturesInWindow(
        start: Long,
        end: Long
    ): List<PendingCaptureEntity>

    /** Reconciliation window query: active, unmatched notifications within a time range. */
    suspend fun getUnmatchedNotificationsInWindow(
        start: Long,
        end: Long
    ): List<PendingNotificationEntity>

    /** Timeout sweep: active, unmatched captures older than the cutoff. */
    suspend fun getExpiredCaptures(cutoff: Long): List<PendingCaptureEntity>

    /**
     * Atomic match: flip matched/active on both the capture and notification,
     * and bind the resulting transaction record — all in a single @Transaction.
     */
    suspend fun markMatched(captureId: String, notificationId: String)

    /**
     * Timeout expiry: mark a capture as inactive (leaves the active matching pool)
     * and route the transaction to AWAITING_AMOUNT (EC-17).
     */
    suspend fun markExpired(captureId: String)

    /** Housekeeping: clean up stale inactive rows older than [olderThan] epoch millis. */
    suspend fun runHousekeeping(olderThan: Long)
}
