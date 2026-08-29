package com.chirag.arthix.data.repository.impl

import com.chirag.arthix.data.dao.ReportDao
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-DAO-backed implementation of [ReportRepository].
 * Straightforward delegation — reports are write-once, read-whole blobs.
 */
class ReportRepositoryImpl @Inject constructor(
    private val dao: ReportDao
) : ReportRepository {

    override suspend fun save(report: ReportEntity): Long {
        return dao.insert(report)
    }

    override suspend fun getForPeriod(start: Long, end: Long): ReportEntity? {
        return dao.getForPeriod(start, end)
    }

    override fun observeAll(): Flow<List<ReportEntity>> {
        return dao.observeAll()
    }
}
