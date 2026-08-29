package com.chirag.arthix.notification

import com.chirag.arthix.notification.model.DedupResult
import kotlin.math.max

/**
 * Deduplication checker for notification-level double-notifications (PRD §6).
 *
 * A single real payment frequently produces two notifications — one from the
 * UPI app, one from the bank's own SMS/notification. Without dedup, this
 * becomes two Transaction records for one real payment.
 *
 * Pure Kotlin — no Android dependencies, testable with synthetic data.
 *
 * Dedup criteria:
 * 1. Same amount (exact match in paise)
 * 2. Close payee-string match (Levenshtein similarity ≥ threshold)
 * 3. Within short time window (default 10s)
 */
object DedupChecker {

    /**
     * Check whether a new outflow candidate is a duplicate of a recently
     * committed transaction.
     *
     * @param amountPaise the new candidate's amount.
     * @param payee the new candidate's payee string.
     * @param timestampMs the new candidate's monotonic timestamp.
     * @param recentOutflows recently committed outflow transactions within
     *        the dedup window, each as (transactionId, amountPaise, payee, timestampMs).
     * @param windowMs dedup window (default 10_000ms per PRD §6.2).
     * @param similarityThreshold payee string similarity threshold (default 0.8 per PRD §6.2).
     * @return [DedupResult.Duplicate] with the existing transaction's ID, or [DedupResult.NewRecord].
     */
    fun check(
        amountPaise: Long,
        payee: String,
        timestampMs: Long,
        recentOutflows: List<RecentOutflow>,
        windowMs: Long = 10_000L,
        similarityThreshold: Double = 0.8,
    ): DedupResult {
        for (existing in recentOutflows) {
            // 1. Within time window
            val timeDelta = kotlin.math.abs(timestampMs - existing.timestampMs)
            if (timeDelta > windowMs) continue

            // 2. Same amount (exact match)
            if (amountPaise != existing.amountPaise) continue

            // 3. Payee similarity check
            val similarity = payeeSimilarity(payee, existing.payee)
            if (similarity >= similarityThreshold) {
                return DedupResult.Duplicate(existing.transactionId)
            }
        }

        return DedupResult.NewRecord
    }

    /**
     * Represents a recently committed outflow for dedup comparison.
     */
    data class RecentOutflow(
        val transactionId: Long,
        val amountPaise: Long,
        val payee: String,
        val timestampMs: Long,
    )

    // ── Payee similarity (Levenshtein-based) ───────────────────────────

    /**
     * Normalized payee-string similarity.
     *
     * `1 - (editDistance / max(len(a), len(b)))` after normalization
     * (lowercase, whitespace-collapsed, punctuation-stripped).
     *
     * PRD §6.2: bank notifications often abbreviate/format payee names
     * differently ("RAMESH CHAI STALL" vs "Ramesh Chai").
     *
     * @return similarity score in [0.0, 1.0], where 1.0 = identical.
     */
    fun payeeSimilarity(a: String, b: String): Double {
        val normA = normalize(a)
        val normB = normalize(b)
        if (normA == normB) return 1.0
        val maxLen = max(normA.length, normB.length)
        if (maxLen == 0) return 1.0
        val distance = levenshteinDistance(normA, normB)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * Normalize a payee string: lowercase, collapse whitespace, strip punctuation.
     */
    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Standard Levenshtein edit distance — O(n*m) DP.
     * Inline implementation — no external library needed for this (~15 lines).
     */
    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }
}
