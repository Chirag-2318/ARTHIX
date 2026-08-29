package com.chirag.arthix.notification

import com.chirag.arthix.notification.model.NotificationOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [OutcomeClassifier] — PRD §5.2 unit tests #2–5.
 *
 * Pure JVM tests — no Android framework dependencies.
 */
class OutcomeClassifierTest {

    private val rejectKeywords = listOf(
        "declined", "failed", "pending", "cancelled", "canceled", "reversed", "refunded"
    )

    // ── Test #2: outflow text containing "declined" → REJECTED ─────────

    @Test
    fun `declined keyword produces REJECTED`() {
        val text = "₹500 payment to Ramesh Chai was declined"
        assertEquals(NotificationOutcome.REJECTED, OutcomeClassifier.classify(text, rejectKeywords))
    }

    // ── Test #3: outflow text containing "pending" → REJECTED ──────────

    @Test
    fun `pending keyword produces REJECTED`() {
        val text = "₹200 payment to Swiggy is pending"
        assertEquals(NotificationOutcome.REJECTED, OutcomeClassifier.classify(text, rejectKeywords))
    }

    // ── Test #4: outflow text containing "failed" → REJECTED ───────────

    @Test
    fun `failed keyword produces REJECTED`() {
        val text = "Payment of ₹1,500 to Flipkart failed"
        assertEquals(NotificationOutcome.REJECTED, OutcomeClassifier.classify(text, rejectKeywords))
    }

    // ── Test #5: outflow text containing "reversed"/"refund" → REFUND ──

    @Test
    fun `reversed keyword produces REFUND`() {
        val text = "₹300 payment to Amazon was reversed"
        assertEquals(NotificationOutcome.REFUND, OutcomeClassifier.classify(text, rejectKeywords))
    }

    @Test
    fun `refund keyword produces REFUND`() {
        val text = "Refund of ₹450 received from Zomato"
        assertEquals(NotificationOutcome.REFUND, OutcomeClassifier.classify(text, rejectKeywords))
    }

    @Test
    fun `refunded keyword produces REFUND not REJECTED`() {
        // "refunded" contains the reject keyword "refunded" but should be REFUND, not REJECTED
        val text = "₹200 refunded by PhonePe"
        assertEquals(NotificationOutcome.REFUND, OutcomeClassifier.classify(text, rejectKeywords))
    }

    // ── Positive case: no reject keyword → COMPLETED ───────────────────

    @Test
    fun `clean payment text produces COMPLETED`() {
        val text = "₹500 paid to Ramesh Chai Stall"
        assertEquals(NotificationOutcome.COMPLETED, OutcomeClassifier.classify(text, rejectKeywords))
    }

    @Test
    fun `case insensitivity works`() {
        val text = "₹500 payment FAILED due to network error"
        assertEquals(NotificationOutcome.REJECTED, OutcomeClassifier.classify(text, rejectKeywords))
    }
}
