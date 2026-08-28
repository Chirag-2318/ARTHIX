package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for persisted financial reports.
 *
 * FROZEN as of Phase 0. Implementation written by Phase 3/5.
 */
interface ReportRepository {

    /** Persist a generated report. Returns the auto-generated row ID. */
    suspend fun save(report: ReportEntity): Long

    /** Check if a report has already been generated for a specific period. */
    suspend fun getForPeriod(start: Long, end: Long): ReportEntity?

    /** Reactive stream of all reports, newest first. */
    fun observeAll(): Flow<List<ReportEntity>>
}
