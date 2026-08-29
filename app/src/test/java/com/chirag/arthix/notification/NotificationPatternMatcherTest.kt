package com.chirag.arthix.notification

import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.notification.model.ExtractedText
import com.chirag.arthix.notification.model.MatchResult
import com.chirag.arthix.notification.model.TextSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [NotificationPatternMatcher] — pattern matching, amount extraction,
 * outcome filtering, and per-app scoping.
 *
 * Pure JVM tests — no Android framework dependencies.
 */
class NotificationPatternMatcherTest {

    private val config = NotificationPatternMatcher.parseConfig(TEST_JSON)!!

    companion object {
        private val TEST_JSON = """
        {
          "outflow_patterns": [
            { "app": "com.google.android.apps.nbu.paisa.user", "regex": "₹\\s?([\\d,]+(?:\\.\\d{1,2})?)\\s+paid to\\s+(.+)" },
            { "app": "com.phonepe.app", "regex": "You paid ₹\\s?([\\d,]+(?:\\.\\d{1,2})?) to\\s+(.+)" },
            { "app": "net.one97.paytm", "regex": "Rs\\.?\\s?([\\d,]+(?:\\.\\d{1,2})?)\\s+sent to\\s+(.+)" }
          ],
          "inflow_patterns": [
            { "app": "com.google.android.apps.nbu.paisa.user", "regex": "You received ₹\\s?([\\d,]+(?:\\.\\d{1,2})?)\\s+from\\s+(.+)" },
            { "app": "com.phonepe.app", "regex": "₹\\s?([\\d,]+(?:\\.\\d{1,2})?) received from\\s+(.+)" }
          ],
          "outcome_reject_keywords": ["declined", "failed", "pending", "cancelled", "canceled", "reversed", "refunded"]
        }
        """.trimIndent()
    }

    // ── Config parsing ─────────────────────────────────────────────────

    @Test
    fun `parseConfig produces valid config from JSON`() {
        assertNotNull(config)
        assertEquals(3, config.outflowPatterns.size)
        assertEquals(2, config.inflowPatterns.size)
        assertEquals(7, config.outcomeRejectKeywords.size)
    }

    // ── GPay outflow ───────────────────────────────────────────────────

    @Test
    fun `GPay outflow notification matches correctly`() {
        val text = ExtractedText("₹500 paid to Ramesh Chai Stall", TextSource.BIG_TEXT)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.OutflowMatch)
        val match = result as MatchResult.OutflowMatch
        assertEquals(50000L, match.parsed.amountPaise)
        assertEquals("Ramesh Chai Stall", match.parsed.payee)
        assertEquals(ConfidenceFlag.CLEAN, match.parsed.confidenceFlag)
    }

    // ── PhonePe outflow ────────────────────────────────────────────────

    @Test
    fun `PhonePe outflow notification matches correctly`() {
        val text = ExtractedText("You paid ₹1,234.50 to Swiggy Delivery", TextSource.BIG_TEXT)
        val result = NotificationPatternMatcher.match(text, "com.phonepe.app", config)
        assertTrue(result is MatchResult.OutflowMatch)
        val match = result as MatchResult.OutflowMatch
        assertEquals(123450L, match.parsed.amountPaise)
        assertEquals("Swiggy Delivery", match.parsed.payee)
    }

    // ── Paytm outflow ──────────────────────────────────────────────────

    @Test
    fun `Paytm outflow notification matches correctly`() {
        val text = ExtractedText("Rs 250 sent to Zomato", TextSource.TEXT_LINES)
        val result = NotificationPatternMatcher.match(text, "net.one97.paytm", config)
        assertTrue(result is MatchResult.OutflowMatch)
        val match = result as MatchResult.OutflowMatch
        assertEquals(25000L, match.parsed.amountPaise)
        assertEquals("Zomato", match.parsed.payee)
    }

    // ── GPay inflow ────────────────────────────────────────────────────

    @Test
    fun `GPay inflow notification matches correctly`() {
        val text = ExtractedText("You received ₹1,000 from Niranjan Kumar", TextSource.BIG_TEXT)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.InflowMatch)
        val match = result as MatchResult.InflowMatch
        assertEquals(100000L, match.parsed.amountPaise)
        assertEquals("Niranjan Kumar", match.parsed.payee)
    }

    // ── No match ───────────────────────────────────────────────────────

    @Test
    fun `unrelated notification text produces NoMatch`() {
        val text = ExtractedText("Your order has been shipped!", TextSource.BIG_TEXT)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.NoMatch)
    }

    // ── Wrong package scoping ──────────────────────────────────────────

    @Test
    fun `GPay pattern does not match against PhonePe package`() {
        val text = ExtractedText("₹500 paid to Ramesh Chai Stall", TextSource.BIG_TEXT)
        // GPay text format, but submitted under PhonePe's package name
        val result = NotificationPatternMatcher.match(text, "com.phonepe.app", config)
        // PhonePe expects "You paid ₹..." format, not "₹... paid to"
        assertTrue(result is MatchResult.NoMatch)
    }

    // ── Rejected outcome ───────────────────────────────────────────────

    @Test
    fun `failed payment notification produces Rejected`() {
        val text = ExtractedText("₹500 paid to Ramesh failed due to server error", TextSource.BIG_TEXT)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.Rejected)
    }

    // ── TEXT_FALLBACK → NEEDS_REVIEW confidence ────────────────────────

    @Test
    fun `TEXT_FALLBACK source produces NEEDS_REVIEW confidence flag`() {
        val text = ExtractedText("₹500 paid to Ramesh Chai Stall", TextSource.TEXT_FALLBACK)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.OutflowMatch)
        val match = result as MatchResult.OutflowMatch
        assertEquals(ConfidenceFlag.NEEDS_REVIEW, match.parsed.confidenceFlag)
    }

    // ── Blank text ─────────────────────────────────────────────────────

    @Test
    fun `blank text produces NoMatch`() {
        val text = ExtractedText("", TextSource.NONE)
        val result = NotificationPatternMatcher.match(text, "com.google.android.apps.nbu.paisa.user", config)
        assertTrue(result is MatchResult.NoMatch)
    }
}
