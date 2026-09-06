package com.chirag.arthix.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit test suite for [OcrDateExtractor].
 *
 * Pure JVM tests covering all requirements and edge cases from
 * Docs/ARTHIX_OCR_Date_Extraction_Design.md §2 and §6.
 */
class OcrDateExtractorTest {

    private val refDate = LocalDate.of(2026, 9, 6)

    // ── 1. Common Format Recognition (§2.1) ──────────────────────────────────

    @Test
    fun `extracts DD-MM-YYYY slash format with keyword anchor`() {
        val text = "CAFE MOCHA\nBill Date: 14/08/2026\nGrand Total: ₹450"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
        assertTrue(result.isKeywordMatch)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `extracts DD-MM-YYYY dash format`() {
        val text = "SUPERMART\nInvoice Date: 18-07-2026\nTotal: 1200"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 7, 18), result.localDate)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `extracts DD-MM-YYYY dot format`() {
        val text = "RETAIL STORE\nDate: 22.06.2026\nAmount: ₹899"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 6, 22), result.localDate)
    }

    @Test
    fun `extracts DD-MM-YY 2-digit year format`() {
        val text = "HOTEL SARAVANA\nDate: 14/08/26\nTotal: ₹320"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
    }

    @Test
    fun `extracts DD MMM YYYY named month format`() {
        val text = "STARBUCKS\nTxn Date: 14 Aug 2026\nTotal: ₹750"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
        assertTrue(result.isKeywordMatch)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `extracts ISO YYYY-MM-DD format`() {
        val text = "DIGITAL POS\nOrder Date: 2026-08-14\nTotal: 500"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
    }

    // ── 2. OCR Character Normalization (§2.2) ────────────────────────────────

    @Test
    fun `normalizes OCR character confusion O, I, l, S, B in digits`() {
        // 'l' instead of '1', 'O' instead of '0', 'B' instead of '8', 'S' instead of '5'
        val text = "THERMAL BILL\nDate: l4/OB/2O26\nTotal: 100"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
    }

    @Test
    fun `normalizes S as 5 and B as 8`() {
        val text = "PHARMACY\nDate: 2B/0S/2026\nTotal: 250"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        // 2B -> 28, 0S -> 05
        assertEquals(LocalDate.of(2026, 5, 28), result.localDate)
    }

    // ── 3. Multiple Dates on One Receipt (§2.3) ──────────────────────────────

    @Test
    fun `prefers purchase date over due date and expiry date`() {
        val text = """
            ELECTRICITY BOARD
            Bill Date: 12/08/2026
            Due Date: 28/08/2026
            Payment Due: ₹2400
        """.trimIndent()

        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 12), result.localDate)
        assertTrue(result.isKeywordMatch)
    }

    @Test
    fun `strictly excludes due and expiry dates even if only ones on bill`() {
        val text = """
            COUPON / VOUCHER
            Valid Until: 20/08/2026
            Exp Date: 30/08/2026
            Amount: ₹500
        """.trimIndent()

        val result = OcrDateExtractor.extract(text, referenceDate = refDate)
        assertTrue(result is OcrDateResult.NotFound)
    }

    @Test
    fun `multiple unlabeled dates - prefers date near top of receipt`() {
        val text = """
            QUICK RESTAURANT
            Receipt #48129
            14/08/2026
            Burger x2: ₹300
            Fries: ₹100
            Printed on: 01/01/2025
            Thank you!
        """.trimIndent()

        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 8, 14), result.localDate)
    }

    // ── 4. Ambiguous Day and Month Order (§2.4) ──────────────────────────────

    @Test
    fun `ambiguous date 03-04-2026 defaults to Indian DD-MM and marks ambiguous`() {
        val text = "SWEET SHOP\nDate: 03/04/2026\nTotal: 250"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 4, 3), result.localDate) // 3rd April
        assertTrue(result.isAmbiguous)
    }

    @Test
    fun `unambiguous date when first component exceeds 12`() {
        val text = "SWEET SHOP\nDate: 25/04/2026\nTotal: 250"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 4, 25), result.localDate)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `unambiguous date when second component exceeds 12`() {
        val text = "IMPORT STORE\nDate: 04/25/2026\nTotal: 1500"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(2026, 4, 25), result.localDate)
        assertFalse(result.isAmbiguous)
    }

    // ── 5. Two-Digit Years (§2.5) ────────────────────────────────────────────

    @Test
    fun `two-digit year resolves to past century if 2000-plus would be future`() {
        // Ref year is 2026. Year '98' -> 1998, not 2098
        val text = "ARCHIVE BILL\nDate: 15/03/98\nTotal: 100"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.Found)
        result as OcrDateResult.Found
        assertEquals(LocalDate.of(1998, 3, 15), result.localDate)
    }

    // ── 6. Future Date Guard (§2.6) ──────────────────────────────────────────

    @Test
    fun `future date is flagged and never silently accepted`() {
        // Ref date is 2026-09-06. Date on receipt is 2026-11-20 (future)
        val text = "FUTURE STORE\nDate: 20/11/2026\nTotal: 500"
        val result = OcrDateExtractor.extract(text, referenceDate = refDate)

        assertTrue(result is OcrDateResult.FutureDate)
        result as OcrDateResult.FutureDate
        assertEquals(LocalDate.of(2026, 11, 20), result.parsedDate)
    }

    // ── 7. No Date Found At All (§2.7) ───────────────────────────────────────

    @Test
    fun `no date found returns NotFound without falling back to today`() {
        val text = """
            SWIGGY ORDER
            Items ₹450
            Delivery ₹30
            Total: ₹480
        """.trimIndent()

        val result = OcrDateExtractor.extract(text, referenceDate = refDate)
        assertTrue(result is OcrDateResult.NotFound)
    }
}
