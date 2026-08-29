package com.chirag.arthix.data.repository.impl

import androidx.room.withTransaction
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.dao.PendingQueueDao
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.data.repository.PendingQueueRepository
import javax.inject.Inject

/**
 * Room-DAO-backed implementation of [PendingQueueRepository].
 *
 * [markMatched] and [markExpired] wrap multi-step operations in a single
 * Room @Transaction to maintain the correctness invariant (EC-18):
 * a capture and notification are either both matched or neither is.
 */
class PendingQueueRepositoryImpl @Inject constructor(
    private val dao: PendingQueueDao,
    private val database: ArthixDatabase,
) : PendingQueueRepository {

    override suspend fun addCapture(capture: PendingCaptureEntity) {
        dao.insertCapture(capture)
    }

    override suspend fun addNotification(notification: PendingNotificationEntity) {
        dao.insertNotification(notification)
    }

    override suspend fun getUnmatchedCapturesInWindow(
        start: Long,
        end: Long
    ): List<PendingCaptureEntity> {
        return dao.getUnmatchedCapturesInWindow(start, end)
    }

    override suspend fun getUnmatchedNotificationsInWindow(
        start: Long,
        end: Long
    ): List<PendingNotificationEntity> {
        return dao.getUnmatchedNotificationsInWindow(start, end)
    }

    override suspend fun getExpiredCaptures(cutoff: Long): List<PendingCaptureEntity> {
        return dao.getExpiredCaptures(cutoff)
    }

    override suspend fun markMatched(captureId: String, notificationId: String) {
        database.withTransaction {
            dao.markCaptureMatched(captureId)
            dao.markNotificationMatched(notificationId)
        }
    }

    override suspend fun markExpired(captureId: String) {
        dao.deactivateCapture(captureId)
    }

    override suspend fun runHousekeeping(olderThan: Long) {
        database.withTransaction {
            dao.deleteStaleInactiveCaptures(olderThan)
            dao.deleteStaleInactiveNotifications(olderThan)
        }
    }
}
