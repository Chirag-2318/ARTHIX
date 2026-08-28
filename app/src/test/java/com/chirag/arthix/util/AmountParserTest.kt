package com.chirag.arthix.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AmountParser] — PRD §10 test table + additional edge cases.
 * Pure JVM tests, no Android dependency needed.
 */
class AmountParserTest {

    // ---- PRD §10 test table: expected successes ----

    @Test
    fun `parse rupee symbol with comma separator and decimals`() {
        val result = AmountParser.parse("₹1,450.00")
        assertSuccess(145000L, result)
    }

    @Test
    fun `parse Rs dot prefix without decimals`() {
        val result = AmountParser.parse("Rs.1450")
        assertSuccess(145000L, result)
    }

    @Test
    fun `parse INR prefix with decimals`() {
        val result = AmountParser.parse("INR 1450.50")
        assertSuccess(145050L, result)
    }

    @Test
    fun `parse lowercase rs with space`() {
        val result = AmountParser.parse("rs 450")
        assertSuccess(45000L, result)
    }

    @Test
    fun `parse zero amount`() {
        val result = AmountParser.parse("₹0")
        assertSuccess(0L, result)
    }

    // ---- PRD §10 test table: expected failures ----

    @Test
    fun `reject negative amount`() {
        val result = AmountParser.parse("-₹450")
        assertFailure(result)
    }

    @Test
    fun `reject sub-paise decimal (more than 2 decimal digits)`() {
        val result = AmountParser.parse("₹450.999")
        assertFailure(result)
    }

    @Test
    fun `reject empty string`() {
        val result = AmountParser.parse("")
        assertFailure(result)
    }

    @Test
    fun `reject non-numeric garbage`() {
        val result = AmountParser.parse("paid")
        assertFailure(result)
    }

    @Test
    fun `reject malformed separator placement`() {
        val result = AmountParser.parse("₹1,4,50")
        assertFailure(result)
    }

    // ---- Additional edge cases ----

    @Test
    fun `parse single digit decimal padded to paise`() {
        // "450.5" should be 450 rupees 50 paise = 45050 paise
        val result = AmountParser.parse("₹450.5")
        assertSuccess(45050L, result)
    }

    @Test
    fun `parse large amount with Indian comma grouping`() {
        // "1,23,456.78" = 123456.78 rupees = 12345678 paise
        val result = AmountParser.parse("₹1,23,456.78")
        assertSuccess(12345678L, result)
    }

    @Test
    fun `parse amount with no prefix`() {
        val result = AmountParser.parse("1450")
        assertSuccess(145000L, result)
    }

    @Test
    fun `parse amount with trailing decimal point`() {
        // "450." should be valid, treated as 450.00
        val result = AmountParser.parse("₹450.")
        assertSuccess(45000L, result)
    }

    @Test
    fun `reject multiple decimal points`() {
        val result = AmountParser.parse("₹4.5.0")
        assertFailure(result)
    }

    @Test
    fun `reject currency symbol only`() {
        val result = AmountParser.parse("₹")
        assertFailure(result)
    }

    @Test
    fun `parse whitespace around amount`() {
        val result = AmountParser.parse("  ₹ 1450  ")
        assertSuccess(145000L, result)
    }

    @Test
    fun `parse INR case insensitive`() {
        val result = AmountParser.parse("inr 250.99")
        assertSuccess(25099L, result)
    }

    @Test
    fun `parse Rs without dot`() {
        val result = AmountParser.parse("Rs 300")
        assertSuccess(30000L, result)
    }

    @Test
    fun `reject amount with letters mixed in`() {
        val result = AmountParser.parse("₹12abc34")
        assertFailure(result)
    }

    // ---- Helpers ----

    private fun assertSuccess(expectedPaise: Long, result: AmountParseResult) {
        assertTrue(
            "Expected Success($expectedPaise) but got $result",
            result is AmountParseResult.Success
        )
        assertEquals(expectedPaise, (result as AmountParseResult.Success).amountPaise)
    }

    private fun assertFailure(result: AmountParseResult) {
        assertTrue(
            "Expected Failure but got $result",
            result is AmountParseResult.Failure
        )
    }
}
