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

    // ── Phase 2 reconciliation additions ───────────────────────────────

    /** Find a single capture by its correlation ID. */
    @Query("SELECT * FROM pending_captures WHERE id = :id")
    suspend fun findCaptureById(id: String): PendingCaptureEntity?

    /** Find a single notification by its ID. */
    @Query("SELECT * FROM pending_notifications WHERE id = :id")
    suspend fun findNotificationById(id: String): PendingNotificationEntity?

    /** Atomically mark a capture as matched and inactive (PRD §9). */
    @Query("UPDATE pending_captures SET matched = 1, active = 0 WHERE id = :id")
    suspend fun markCaptureMatched(id: String)

    /** Atomically mark a notification as matched and inactive (PRD §9). */
    @Query("UPDATE pending_notifications SET matched = 1, active = 0 WHERE id = :id")
    suspend fun markNotificationMatched(id: String)

    /** Deactivate a capture (for timeout/discard — PRD §7.6/§7.7). */
    @Query("UPDATE pending_captures SET active = 0 WHERE id = :id")
    suspend fun deactivateCapture(id: String)

    /** Deactivate a notification (for timeout — PRD §7.6). */
    @Query("UPDATE pending_notifications SET active = 0 WHERE id = :id")
    suspend fun deactivateNotification(id: String)

    /** Delete a capture by ID (for cancellation signal handling). */
    @Query("DELETE FROM pending_captures WHERE id = :id")
    suspend fun deleteCaptureById(id: String)

    /**
     * Dedup lookup: recent active notifications within the dedup window.
     * Used to check for bank+app double-notification (PRD §6).
     */
    @Query("""
        SELECT * FROM pending_notifications
        WHERE active = 1 AND timestampMonotonic >= :minTimestamp
        ORDER BY timestampMonotonic DESC
    """)
    suspend fun getRecentActiveNotifications(minTimestamp: Long): List<PendingNotificationEntity>

    /**
     * Symmetric timeout query for notifications (matches getExpiredCaptures).
     */
    @Query("""
        SELECT * FROM pending_notifications
        WHERE active = 1 AND matched = 0 AND timestampMonotonic < :cutoff
    """)
    suspend fun getExpiredNotifications(cutoff: Long): List<PendingNotificationEntity>
}
