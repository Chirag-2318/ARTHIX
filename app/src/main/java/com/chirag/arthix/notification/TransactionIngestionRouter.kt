package com.chirag.arthix.notification

import android.util.Log
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.notification.model.ConfidenceLevel
import com.chirag.arthix.notification.model.NotificationOutcome
import com.chirag.arthix.notification.model.ParsedInflow
import com.chirag.arthix.notification.model.ParsedOutflow
import com.chirag.arthix.notification.model.TransactionCandidate
import com.chirag.arthix.notification.model.TransactionSourceType

/**
 * Single entry point for both UPI notification and bank SMS candidates
 * into the reconciliation engine (Phase 2.1).
 *
 * Responsibilities:
 * - Accepts [TransactionCandidate] from either source
 * - Runs cross-source deduplication (SMS + notification for same payment)
 * - Routes to [ReconciliationEngine] via existing API
 * - Structured logging (no sensitive content in Logcat)
 *
 * All routing runs on the caller's context — the engine's own serial
 * dispatcher handles concurrency.
 */
class TransactionIngestionRouter(
    private val engine: ReconciliationEngine,
) {

    companion object {
        private const val TAG = "IngestionRouter"

        /** Cross-source dedup window: SMS + notification within 30s = same payment. */
        const val CROSS_SOURCE_DEDUP_WINDOW_MS = 30_000L
    }

    /**
     * Recently ingested candidates for cross-source dedup.
     * Key = amountPaise, Value = list of (fingerprint, timestampMs, sourceType).
     *
     * This is an in-memory cache — process death clears it, which is safe
     * because the worst case is a duplicate transaction that the user can
     * discard. The alternative (persisted dedup state) adds complexity
     * without meaningful benefit for a 30s window.
     */
    private val recentCandidates = mutableListOf<RecentCandidate>()

    private data class RecentCandidate(
        val amountPaise: Long,
        val direction: Direction,
        val fingerprint: String,
        val sourceType: TransactionSourceType,
        val timestampMs: Long,
    )

    /**
     * Route a transaction candidate into the reconciliation engine.
     *
     * @param candidate the normalized, source-agnostic candidate.
     */
    fun ingest(candidate: TransactionCandidate) {
        // Structured log — no sensitive financial content
        Log.d(TAG, "INGEST source=${candidate.sourceType} " +
            "direction=${candidate.direction} " +
            "amountPresent=true " +
            "confidence=${candidate.confidence} " +
            "sender=${candidate.senderAddress ?: candidate.sourcePackage ?: "unknown"}")

        // ── Step 1: outcome routing ────────────────────────────────
        when (candidate.outcome) {
            NotificationOutcome.REJECTED -> {
                Log.d(TAG, "REJECTED source=${candidate.sourceType}")
                return
            }
            NotificationOutcome.REFUND -> {
                Log.d(TAG, "REFUND source=${candidate.sourceType}")
                engine.onRefundNotification(
                    candidate.amountPaise,
                    candidate.payee ?: "",
                )
                return
            }
            NotificationOutcome.COMPLETED -> { /* continue */ }
        }

        // ── Step 2: cross-source dedup ─────────────────────────────
        if (isDuplicateCrossSource(candidate)) {
            Log.d(TAG, "CROSS_SOURCE_DEDUP source=${candidate.sourceType}")
            return
        }
        recordCandidate(candidate)

        // ── Step 3: route to engine ────────────────────────────────
        val confidenceFlag = when (candidate.confidence) {
            ConfidenceLevel.HIGH -> ConfidenceFlag.CLEAN
            ConfidenceLevel.MEDIUM -> ConfidenceFlag.NEEDS_REVIEW
            ConfidenceLevel.LOW -> ConfidenceFlag.NEEDS_REVIEW
        }

        when (candidate.direction) {
            Direction.OUTFLOW -> {
                engine.onNotificationCandidate(
                    ParsedOutflow(
                        amountPaise = candidate.amountPaise,
                        payee = candidate.payee ?: "Unknown",
                        packageName = candidate.sourcePackage ?: "sms:${candidate.senderAddress}",
                        rawText = "",  // Privacy: never store raw text
                        confidenceFlag = confidenceFlag,
                    )
                )
            }
            Direction.INFLOW -> {
                engine.onInflowNotification(
                    ParsedInflow(
                        amountPaise = candidate.amountPaise,
                        payee = candidate.payee ?: "Unknown",
                        packageName = candidate.sourcePackage ?: "sms:${candidate.senderAddress}",
                    )
                )
            }
        }
    }

    // ── Cross-source dedup ─────────────────────────────────────────────

    /**
     * Check if a candidate is a duplicate of a recently ingested candidate
     * from a different source.
     *
     * Criteria (all must match):
     * 1. Same amount (exact paise)
     * 2. Same direction
     * 3. Different source type (UPI + SMS for same payment)
     * 4. Within [CROSS_SOURCE_DEDUP_WINDOW_MS]
     */
    private fun isDuplicateCrossSource(candidate: TransactionCandidate): Boolean {
        val now = candidate.timestampMs
        // Prune expired entries
        recentCandidates.removeAll { now - it.timestampMs > CROSS_SOURCE_DEDUP_WINDOW_MS }

        return recentCandidates.any { recent ->
            recent.amountPaise == candidate.amountPaise &&
                recent.direction == candidate.direction &&
                recent.sourceType != candidate.sourceType &&
                kotlin.math.abs(now - recent.timestampMs) <= CROSS_SOURCE_DEDUP_WINDOW_MS
        }
    }

    private fun recordCandidate(candidate: TransactionCandidate) {
        recentCandidates.add(
            RecentCandidate(
                amountPaise = candidate.amountPaise,
                direction = candidate.direction,
                fingerprint = candidate.rawFingerprint,
                sourceType = candidate.sourceType,
                timestampMs = candidate.timestampMs,
            )
        )
        // Keep the list bounded
        if (recentCandidates.size > 50) {
            recentCandidates.removeAt(0)
        }
    }
}
