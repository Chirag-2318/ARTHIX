package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A persisted financial report for a time period.
 *
 * Reports are persisted (not just computed and discarded) so the demo can
 * show report history and Phase 5's arithmetic-verification tests have
 * something durable to assert against.
 *
 * [categoryBreakdownJson] and [suggestionsJson] are JSON blobs stored as
 * Strings. They are write-once, read-whole — a JSON blob is the correct
 * amount of engineering for a hackathon timeline (PRD §2.6).
 *
 * [uncategorizedTotalPaise] ensures pending/unlabeled amounts stay visible
 * in the total, never invisible (EC-44).
 */
@Entity(
    tableName = "reports",
    indices = [Index(value = ["periodStart", "periodEnd"])]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodStart: Long,                      // epoch millis
    val periodEnd: Long,

    val categoryBreakdownJson: String,          // Map<String, Long> serialized via JsonConverters
    val netFlowPaise: Long,                     // inflow - outflow
    val suggestionsJson: String,                // List<String> serialized via JsonConverters
    val projectedTotalPaise: Long,
    val projectedSavingsPaise: Long,
    val uncategorizedTotalPaise: Long,          // EC-44

    val generatedAt: Long
)
