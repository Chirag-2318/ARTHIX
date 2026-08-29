package com.chirag.arthix.notification

import com.chirag.arthix.notification.model.DedupResult
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [DedupChecker] — PRD §6 unit tests #8, #9, and payee similarity.
 *
 * Pure JVM tests — no Android framework dependencies.
 */
class DedupCheckerTest {

    // ── Test #8: same amount, similar payee, within 10s → Duplicate ────

    @Test
    fun `same payment from app and bank within 10s is Duplicate`() {
        // Bank notifications often differ only in casing/minor formatting,
        // not major truncation. "Ramesh Chai Stall" vs "RAMESH CHAI STALL"
        // normalizes to identical strings → similarity = 1.0.
        val recentOutflows = listOf(
            DedupChecker.RecentOutflow(
                transactionId = 1L,
                amountPaise = 50000L,  // ₹500.00
                payee = "Ramesh Chai Stall",
                timestampMs = 100_000L,
            )
        )
        val result = DedupChecker.check(
            amountPaise = 50000L,
            payee = "RAMESH CHAI STALL",  // bank notification: same name, different casing
            timestampMs = 105_000L, // 5 seconds later
            recentOutflows = recentOutflows,
        )
        assertTrue("Expected Duplicate", result is DedupResult.Duplicate)
    }

    // ── Test: outside 10s window → NewRecord ───────────────────────────

    @Test
    fun `same payment outside dedup window is NewRecord`() {
        val recentOutflows = listOf(
            DedupChecker.RecentOutflow(
                transactionId = 1L,
                amountPaise = 50000L,
                payee = "Ramesh Chai Stall",
                timestampMs = 100_000L,
            )
        )
        val result = DedupChecker.check(
            amountPaise = 50000L,
            payee = "Ramesh Chai Stall",
            timestampMs = 115_000L, // 15 seconds later — outside 10s window
            recentOutflows = recentOutflows,
        )
        assertTrue("Expected NewRecord", result is DedupResult.NewRecord)
    }

    // ── Test: different amount, same payee, within window → NewRecord ──

    @Test
    fun `different amount to same payee within window is NewRecord`() {
        val recentOutflows = listOf(
            DedupChecker.RecentOutflow(
                transactionId = 1L,
                amountPaise = 50000L,
                payee = "Swiggy Delivery",
                timestampMs = 100_000L,
            )
        )
        val result = DedupChecker.check(
            amountPaise = 75000L, // different amount
            payee = "Swiggy Delivery",
            timestampMs = 103_000L,
            recentOutflows = recentOutflows,
        )
        assertTrue("Expected NewRecord", result is DedupResult.NewRecord)
    }

    // ── Test: payee similarity check ───────────────────────────────────

    @Test
    fun `heavily abbreviated payee is correctly below dedup threshold`() {
        // "ramesh chai stall" vs "ramesh chai" → edit distance 6, maxLen 17
        // similarity = 1 - 6/17 ≈ 0.65 — correctly below the 0.8 threshold.
        // This means a heavily abbreviated bank name will NOT be deduped,
        // which is the safer behavior (create a new record rather than
        // silently merge two genuinely different payments).
        val similarity = DedupChecker.payeeSimilarity("RAMESH CHAI STALL", "Ramesh Chai")
        assertTrue("Expected similarity < 0.8 for heavy abbreviation, got $similarity", similarity < 0.8)
    }

    @Test
    fun `minor formatting difference is above dedup threshold`() {
        // "Swiggy Delivery" vs "SWIGGY DELIVERY" → identical after normalization
        val similarity = DedupChecker.payeeSimilarity("Swiggy Delivery", "Swiggy Dlvry")
        // "swiggy delivery" vs "swiggy dlvry" → edit distance 3, maxLen 15
        // similarity = 1 - 3/15 = 0.8 — exactly at threshold
        assertTrue("Expected similarity >= 0.8, got $similarity", similarity >= 0.8)
    }

    @Test
    fun `identical payees have similarity of 1_0`() {
        val similarity = DedupChecker.payeeSimilarity("Swiggy Delivery", "SWIGGY DELIVERY")
        assertTrue("Expected 1.0, got $similarity", similarity == 1.0)
    }

    @Test
    fun `completely different payees have low similarity`() {
        val similarity = DedupChecker.payeeSimilarity("Amazon", "Zomato")
        assertTrue("Expected low similarity, got $similarity", similarity < 0.5)
    }

    // ── Test #9: two genuine separate payments, same amount+payee, within 10s ──

    @Test
    fun `two genuine payments same amount same payee within 10s are flagged as Duplicate`() {
        // This is a KNOWN-HARD case per PRD §11 Test #9:
        // amount+payee+window alone cannot perfectly distinguish this from
        // a bank+app double-notification. The dedup check will flag it as
        // Duplicate — this is documented behavior, not a bug.
        val recentOutflows = listOf(
            DedupChecker.RecentOutflow(
                transactionId = 1L,
                amountPaise = 25000L,
                payee = "Chai Point",
                timestampMs = 100_000L,
            )
        )
        val result = DedupChecker.check(
            amountPaise = 25000L,
            payee = "Chai Point",
            timestampMs = 108_000L, // 8 seconds later
            recentOutflows = recentOutflows,
        )
        // Documenting the known limitation: this WILL be flagged as duplicate
        assertTrue("Known limitation: indistinguishable from double-notif", result is DedupResult.Duplicate)
    }
}
