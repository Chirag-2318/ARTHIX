package com.chirag.arthix.notification

import com.chirag.arthix.notification.model.NotificationOutcome

/**
 * Classifies a notification's payment outcome BEFORE it enters the
 * reconciliation pipeline (PRD §5.2).
 *
 * Pure function — no Android dependencies, testable with synthetic strings.
 *
 * Decision tree:
 * 1. If text contains "refund" or "reversed" → REFUND (routed to §5.3 netting)
 * 2. If text contains any other reject keyword → REJECTED (silently dropped)
 * 3. Otherwise → COMPLETED (enters matching pipeline)
 *
 * Note: REFUND check runs first because "refunded" contains no other reject
 * keyword, but "reversed" does not overlap with "pending"/"failed"/etc.
 * The ordering ensures refunds are never accidentally classified as REJECTED.
 */
object OutcomeClassifier {

    /**
     * Classify the outcome of a notification's text.
     *
     * @param text the full extracted notification text (not lowercased — this function handles it).
     * @param rejectKeywords the keywords to check against (from [PatternConfig.outcomeRejectKeywords]).
     * @return the classified outcome.
     */
    fun classify(text: String, rejectKeywords: List<String>): NotificationOutcome {
        val lowerText = text.lowercase()

        // Check for refund/reversal first — these are not outright rejections,
        // they're inflow-netting candidates (PRD §5.3)
        if ("refund" in lowerText || "reversed" in lowerText) {
            return NotificationOutcome.REFUND
        }

        // Check for rejection keywords
        if (rejectKeywords.any { it.lowercase() in lowerText }) {
            return NotificationOutcome.REJECTED
        }

        return NotificationOutcome.COMPLETED
    }
}
