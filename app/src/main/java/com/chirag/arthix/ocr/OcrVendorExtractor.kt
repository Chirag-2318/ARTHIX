package com.chirag.arthix.ocr

/**
 * Extracts a vendor / payee name from raw OCR text.
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Heuristic (EC-33)
 * Receipt headers in India typically place the shop/restaurant/vendor name on
 * the first line or the first few lines, before any itemized content. We:
 * 1. Take the first non-blank, non-numeric-only line from the top of the text.
 * 2. Reject lines that look like addresses (contain common address tokens:
 *    "road", "street", "nagar", "floor", "building", comma-heavy).
 * 3. Reject lines shorter than [MIN_VENDOR_NAME_LENGTH] characters (likely
 *    a single-char artefact or separator).
 * 4. If no line passes the filters → return null.
 *
 * **Key rule (EC-33):** returning null is always safer than returning garbage.
 * The Phase 3 manual entry screen already handles a null payee gracefully.
 */
object OcrVendorExtractor {

    /** Minimum character count for a line to be treated as a vendor name. */
    private const val MIN_VENDOR_NAME_LENGTH = 3

    /** Maximum character count — very long lines are usually address or item lines. */
    private const val MAX_VENDOR_NAME_LENGTH = 60

    /**
     * Tokens that strongly suggest a line is an address fragment rather than a vendor name.
     * Matched case-insensitively.
     *
     * Deliberately narrow: "road" and "street" are excluded because they appear in
     * common Indian food vendor names like "Street Food Stall" or "MG Road Biryani".
     * Only tokens that are unambiguously address-specific are kept here.
     */
    private val ADDRESS_TOKENS = listOf(
        "nagar", "colony", "floor no", "building no",
        "plot no", "sector", "pin code", "pincode",
        "gstin", "gst no", "fssai", "cin no",
    )


    /**
     * Extract the most likely vendor/business name from [ocrText].
     *
     * @param ocrText raw multi-line text from ML Kit.
     * @return the extracted name, or null if no reliable name found (EC-33).
     */
    fun extract(ocrText: String): String? {
        if (ocrText.isBlank()) return null

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Scan the top portion of the receipt (first 8 lines is enough for a header).
        val candidates = lines.take(8)

        for (line in candidates) {
            if (isValidVendorLine(line)) return line
        }
        return null
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun isValidVendorLine(line: String): Boolean {
        // Too short or too long
        if (line.length < MIN_VENDOR_NAME_LENGTH) return false
        if (line.length > MAX_VENDOR_NAME_LENGTH) return false

        // Reject purely numeric lines (could be a receipt/invoice number)
        if (line.replace(" ", "").all { it.isDigit() || it == '.' || it == ',' }) return false

        // Reject lines with too many digits (item quantity/price lines)
        val digitRatio = line.count { it.isDigit() }.toFloat() / line.length
        if (digitRatio > 0.5f) return false

        // Reject lines that look like addresses
        val lower = line.lowercase()
        if (ADDRESS_TOKENS.any { lower.contains(it) }) return false

        // Reject lines with 2 or more commas — reliably indicates an address
        // (e.g. "Shop 3, Building A, MG Road"). Vendor names with 2+ commas are rare.
        if (line.count { it == ',' } >= 2) return false

        return true
    }
}
