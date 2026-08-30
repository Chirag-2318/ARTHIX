package com.chirag.arthix.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [OcrVendorExtractor].
 *
 * All pure JVM — no Android framework, no Robolectric.
 * Covers EC-33: prefer null over garbage vendor name.
 */
class OcrVendorExtractorTest {

    @Test
    fun `typical receipt header - extracts vendor name`() {
        val text = "Cafe Mocha\n123 MG Road, Pune\nCoffee  ₹120\nTotal  ₹120"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Cafe Mocha", result)
    }

    @Test
    fun `vendor name on first line - returned directly`() {
        val text = "Swiggy Instamart\nOrder #12345\nTotal ₹450"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Swiggy Instamart", result)
    }

    @Test
    fun `first line is numeric only - skip to next valid line`() {
        // Receipt number or barcode on line 1 — should not be returned
        val text = "123456789\nDominos Pizza\nTotal ₹350"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Dominos Pizza", result)
    }

    @Test
    fun `first line is address - skip to next valid line`() {
        val text = "Shop 3, Building A, MG Road\nHotel Saraswati\nTotal ₹220"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Hotel Saraswati", result)
    }

    @Test
    fun `line with GST number - rejected, vendor name after`() {
        val text = "GSTIN: 27AADCB2230M1Z3\nMedical Store\nTotal ₹180"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Medical Store", result)
    }

    @Test
    fun `empty text - returns null (EC-33)`() {
        val result = OcrVendorExtractor.extract("")
        assertNull(result)
    }

    @Test
    fun `blank text - returns null`() {
        val result = OcrVendorExtractor.extract("   \n\n   ")
        assertNull(result)
    }

    @Test
    fun `all lines are addresses or numeric - returns null (EC-33)`() {
        // All lines have address tokens; no vendor name extractable.
        val text = "123 Street Road Nagar\n456 Building Colony\nGSTIN 27ABC1234"
        val result = OcrVendorExtractor.extract(text)
        assertNull(result)
    }

    @Test
    fun `very short lines - rejected, null returned when nothing valid`() {
        val text = "AB\n12\n₹"
        val result = OcrVendorExtractor.extract(text)
        assertNull(result)
    }

    @Test
    fun `line with two or more commas (address) - rejected`() {
        // ">= 2 commas" threshold: both "Shop 3, Floor 2, Building A, MG Road" (3 commas)
        // and "Shop 3, Building A, MG Road" (2 commas) should be rejected.
        val text = "Shop 3, Floor 2, Building A, MG Road\nPav Bhaji Stall\nTotal ₹60"
        val result = OcrVendorExtractor.extract(text)
        // First line has 3 commas, so it's rejected; second line should be returned.
        assertEquals("Pav Bhaji Stall", result)
    }


    @Test
    fun `line with majority digits - rejected`() {
        // A price line like "45 67 89" — digit ratio > 0.5, should be skipped
        val text = "450 780 120\nStreet Food Corner\nTotal ₹100"
        val result = OcrVendorExtractor.extract(text)
        assertEquals("Street Food Corner", result)
    }

    @Test
    fun `real-world receipt snippet - extracts vendor not address or total`() {
        val text = """
            HOTEL VILAS
            Near Railway Station, Pune
            FSSAI Lic: 1234567890
            Veg Thali   ₹120
            Chai        ₹20
            Total       ₹140
        """.trimIndent()
        val result = OcrVendorExtractor.extract(text)
        assertEquals("HOTEL VILAS", result)
    }
}
