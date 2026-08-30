package com.chirag.arthix.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [SpokenAmountParser].
 *
 * All pure JVM — zero Android framework dependencies.
 * Covers English number words, Indian-English phrases ("four fifty", "twelve hundred"),
 * digit-word hybrids ("4 fifty", "2 thousand"), currency prefixes, and edge cases.
 */
class SpokenAmountParserTest {

    @Test
    fun `direct numbers - parse correctly`() {
        assertEquals(45000L, SpokenAmountParser.parse("450"))
        assertEquals(120000L, SpokenAmountParser.parse("1200"))
        assertEquals(5000L, SpokenAmountParser.parse("50"))
        assertEquals(100L, SpokenAmountParser.parse("1"))
    }

    @Test
    fun `currency markers and prefixes - ignored and parsed`() {
        assertEquals(45000L, SpokenAmountParser.parse("₹450"))
        assertEquals(45000L, SpokenAmountParser.parse("rs 450"))
        assertEquals(45000L, SpokenAmountParser.parse("450 rupees"))
        assertEquals(45000L, SpokenAmountParser.parse("four hundred fifty rupees only"))
    }

    @Test
    fun `hundreds and compound numbers - parse correctly`() {
        assertEquals(45000L, SpokenAmountParser.parse("four hundred fifty"))
        assertEquals(45000L, SpokenAmountParser.parse("four hundred and fifty"))
        assertEquals(10000L, SpokenAmountParser.parse("one hundred"))
        assertEquals(99900L, SpokenAmountParser.parse("nine hundred ninety nine"))
    }

    @Test
    fun `indian-english shorthand - four fifty to 450`() {
        // "four fifty" -> 450 rupees = 45000 paise
        assertEquals(45000L, SpokenAmountParser.parse("four fifty"))
        assertEquals(25000L, SpokenAmountParser.parse("two fifty"))
        assertEquals(85000L, SpokenAmountParser.parse("eight fifty"))
    }

    @Test
    fun `hundreds as multiplier - twelve hundred to 1200`() {
        assertEquals(120000L, SpokenAmountParser.parse("twelve hundred"))
        assertEquals(150000L, SpokenAmountParser.parse("fifteen hundred"))
        assertEquals(250000L, SpokenAmountParser.parse("twenty five hundred"))
    }

    @Test
    fun `thousands and lakhs - parse correctly`() {
        assertEquals(200000L, SpokenAmountParser.parse("two thousand"))
        assertEquals(235000L, SpokenAmountParser.parse("two thousand three hundred fifty"))
        assertEquals(10000000L, SpokenAmountParser.parse("one lakh"))
    }

    @Test
    fun `hybrid digit word combinations`() {
        assertEquals(45000L, SpokenAmountParser.parse("4 fifty"))
        assertEquals(200000L, SpokenAmountParser.parse("2 thousand"))
        assertEquals(500000L, SpokenAmountParser.parse("5 thousand rupees"))
    }

    @Test
    fun `empty or non numeric input - returns null`() {
        assertNull(SpokenAmountParser.parse(""))
        assertNull(SpokenAmountParser.parse("   "))
        assertNull(SpokenAmountParser.parse("hello world"))
        assertNull(SpokenAmountParser.parse("just food"))
    }
}
