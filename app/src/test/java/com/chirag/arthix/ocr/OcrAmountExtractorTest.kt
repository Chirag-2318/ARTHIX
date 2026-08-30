package com.chirag.arthix.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OcrAmountExtractor].
 *
 * All pure JVM — no Android framework, no Robolectric.
 * Covers the cases mandated by the Phase 4 PRD §12 and EC-30, EC-32.
 */
class OcrAmountExtractorTest {

    // ── Keyword-proximity pass (EC-30, CLEAN path) ─────────────────────────────

    @Test
    fun `keyword Total on same line - extracts correct amount CLEAN`() {
        val text = "SWIGGY RECEIPT\nItems  ₹350\nDelivery  ₹30\nTotal: ₹450"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(45000L, result.amountPaise)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `keyword Grand Total on same line - extracts correct amount`() {
        val text = "Cafe Mocha\nCoffee  ₹120\nGrand Total  Rs.1,450.00"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(145000L, result.amountPaise)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `keyword Amount Payable on same line - extracts correct amount`() {
        val text = "Invoice\nAmount Payable: INR 2300"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(230000L, result.amountPaise)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `keyword Total on next line - amount on following line extracted`() {
        val text = "Hotel Bill\nFood  300\nTotal\n₹850"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(85000L, result.amountPaise)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `keyword case insensitive - TOTAL upper-case matches`() {
        val text = "TOTAL ₹999"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(99900L, result.amountPaise)
        assertTrue(result.isKeywordMatch)
    }

    // ── Largest-number fallback (EC-30, NEEDS_REVIEW path) ────────────────────

    @Test
    fun `no keyword - fallback to largest number, NEEDS_REVIEW`() {
        val text = "Street Food\n₹30  vada pav\n₹10  chai\n₹80  dabeli"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(8000L, result.amountPaise)   // ₹80 is largest
        assertTrue(!result.isKeywordMatch)
    }

    @Test
    fun `no keyword single number - returns it as NEEDS_REVIEW`() {
        val text = "Vegetable\n₹45"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(4500L, result.amountPaise)
        assertTrue(!result.isKeywordMatch)
    }

    // ── Sanity bounds (EC-32) ──────────────────────────────────────────────────

    @Test
    fun `amount above MAX_AMOUNT - returns OutOfBounds`() {
        // ₹75,000 = 7,500,000 paise — above 5,000,000 limit
        val text = "Total: ₹75,000"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.OutOfBounds)
    }

    @Test
    fun `amount below MIN_AMOUNT - returns OutOfBounds`() {
        // ₹0.50 = 50 paise — below 100 paise limit
        val text = "Total: ₹0.50"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.OutOfBounds)
    }

    @Test
    fun `amount exactly at MIN boundary - valid`() {
        // ₹1.00 = 100 paise exactly — should be accepted
        val text = "Total: ₹1"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(100L, result.amountPaise)
    }

    @Test
    fun `amount exactly at MAX boundary - valid`() {
        // ₹50,000 = 5,000,000 paise exactly — should be accepted
        val text = "Total: ₹50,000"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(5_000_000L, result.amountPaise)
    }

    // ── NotFound cases ─────────────────────────────────────────────────────────

    @Test
    fun `empty text - returns NotFound`() {
        val result = OcrAmountExtractor.extract("")
        assertEquals(OcrAmountResult.NotFound, result)
    }

    @Test
    fun `blank text - returns NotFound`() {
        val result = OcrAmountExtractor.extract("   \n\n  ")
        assertEquals(OcrAmountResult.NotFound, result)
    }

    @Test
    fun `text with no numbers - returns NotFound`() {
        val result = OcrAmountExtractor.extract("Thank you for visiting!\nHave a nice day.")
        assertEquals(OcrAmountResult.NotFound, result)
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Test
    fun `Grand Total preferred over subtotal Total`() {
        // "Grand Total" should win because it appears earlier in AMOUNT_KEYWORDS list
        val text = "Subtotal  Total: ₹200\nGrand Total: ₹230"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        // Grand Total line has ₹230
        assertEquals(23000L, result.amountPaise)
    }

    @Test
    fun `receipt with Indian number format 1 lakh 45 thousand`() {
        // ₹1,45,000 should parse correctly (Indian grouping)
        val text = "Total: ₹1,45,000"
        val result = OcrAmountExtractor.extract(text)
        // ₹1,45,000 = 14,500,000 paise — above MAX, so OutOfBounds
        assertTrue(result is OcrAmountResult.OutOfBounds)
    }

    @Test
    fun `receipt with decimal paise in amount`() {
        val text = "Total ₹1,250.75"
        val result = OcrAmountExtractor.extract(text)
        assertTrue(result is OcrAmountResult.Found)
        result as OcrAmountResult.Found
        assertEquals(125075L, result.amountPaise)
    }
}
