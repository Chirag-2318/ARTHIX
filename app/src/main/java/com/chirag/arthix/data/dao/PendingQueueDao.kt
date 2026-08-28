package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity

@Dao
interface PendingQueueDao {

    @Insert
    suspend fun insertCapture(c: PendingCaptureEntity)

    @Insert
    suspend fun insertNotification(n: PendingNotificationEntity)

    @Update
    suspend fun updateCapture(c: PendingCaptureEntity)

    @Update
    suspend fun updateNotification(n: PendingNotificationEntity)

    /**
     * Phase 2's core reconciliation query — nearest-neighbor search space.
     * Uses the (matched, active, timestampMonotonic) composite index.
     */
    @Query("""
        SELECT * FROM pending_captures
        WHERE matched = 0 AND active = 1
              AND timestampMonotonic BETWEEN :windowStart AND :windowEnd
        ORDER BY timestampMonotonic ASC
    """)
    suspend fun getUnmatchedCapturesInWindow(
        windowStart: Long,
        windowEnd: Long
    ): List<PendingCaptureEntity>

    /**
     * Mirrored query for unmatched notifications within a reconciliation window.
     */
    @Query("""
        SELECT * FROM pending_notifications
        WHERE matched = 0 AND active = 1
              AND timestampMonotonic BETWEEN :windowStart AND :windowEnd
        ORDER BY timestampMonotonic ASC
    """)
    suspend fun getUnmatchedNotificationsInWindow(
        windowStart: Long,
        windowEnd: Long
    ): List<PendingNotificationEntity>

    /**
     * Phase 2's per-capture independent timeout sweep — finds captures that
     * are still active and unmatched but older than the cutoff.
     */
    @Query("""
        SELECT * FROM pending_captures
        WHERE active = 1 AND matched = 0 AND timestampMonotonic < :cutoff
    """)
    suspend fun getExpiredCaptures(cutoff: Long): List<PendingCaptureEntity>

    /**
     * Housekeeping: remove stale inactive captures older than a given epoch.
     * Called from WorkManager by Phase 1, not scheduled by Phase 0.
     */
    @Query("DELETE FROM pending_captures WHERE active = 0 AND createdAt < :olderThan")
    suspend fun deleteStaleInactiveCaptures(olderThan: Long)

    /**
     * Housekeeping: remove stale inactive notifications older than a given epoch.
     */
    @Query("DELETE FROM pending_notifications WHERE active = 0 AND createdAt < :olderThan")
    suspend fun deleteStaleInactiveNotifications(olderThan: Long)
}
