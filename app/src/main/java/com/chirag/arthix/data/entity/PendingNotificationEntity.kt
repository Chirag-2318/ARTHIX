package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A parsed notification from an allow-listed payment app, awaiting
 * reconciliation with a shake-capture event.
 *
 * [rawText] is the original notification text, kept for debugging low-confidence
 * parses. Never retained if it came from a non-allow-listed package — that
 * content is never written here at all, per the security baseline (this table
 * only ever receives allow-listed rows).
 */
@Entity(
    tableName = "pending_notifications",
    indices = [
        Index(value = ["timestampMonotonic"]),
        Index(value = ["matched"]),
        Index(value = ["active"]),
        Index(value = ["matched", "active", "timestampMonotonic"])  // reconciliation hot query
    ]
)
data class PendingNotificationEntity(
    @PrimaryKey val id: String,                   // UUID, generated at notification-parse time

    val timestampMonotonic: Long,
    val amountPaise: Long,
    val payee: String,
    val matched: Boolean = false,
    val active: Boolean = true,

    val rawText: String?,                         // original notification text for debug

    val sourceType: String = "UPI_APP_NOTIFICATION",  // TransactionSourceType name
    val senderAddress: String? = null,                // e.g. "VM-HDFCBK" for SMS sources

    val createdAt: Long
)
