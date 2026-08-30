package com.chirag.arthix.ocr

/**
 * Extracts a vendor / payee name from raw OCR text.
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Heuristic (EC-33)
 * Receipt headers in India typically place the shop/restaurant/vendor name on
 * the first line or the first few lines, before any itemized content. We:
 * 1. Filter out generic document titles ("Tax Invoice", "Bill of Supply", "Customer Copy").
 * 2. Filter out non-vendor metadata ("Bill No: 64", "Date: ...", "GSTIN: ...", "Table 4").
 * 3. Reject address-heavy or numeric-heavy lines.
 * 4. Return the first valid vendor candidate or null.
 *
 * **Key rule (EC-33):** returning null is always safer than returning garbage.
 */
object OcrVendorExtractor {

    /** Minimum character count for a line to be treated as a vendor name. */
    private const val MIN_VENDOR_NAME_LENGTH = 3

    /** Maximum character count — very long lines are usually address or item lines. */
    private const val MAX_VENDOR_NAME_LENGTH = 60

    /**
     * Tokens that indicate generic document headers rather than a vendor name.
     */
    private val GENERIC_HEADER_TOKENS = listOf(
        "tax invoice", "retail invoice", "bill of supply", "commercial invoice",
        "cash receipt", "sales receipt", "payment receipt", "customer copy",
        "merchant copy", "original copy", "duplicate copy", "triplicate copy",
        "order summary", "order details", "delivery receipt", "invoice", "receipt",
        "bill no", "bill #", "order id", "order no", "order #", "table no", "table #",
        "token no", "token #", "welcome", "thank you", "visit again", "thanks for visiting",
        "dine in", "takeaway", "delivery", "cashier", "steward", "waiter"
    )

    /**
     * Tokens that strongly suggest a line is an address or registration metadata fragment.
     */
    private val ADDRESS_METADATA_TOKENS = listOf(
        "nagar", "colony", "floor no", "building no",
        "plot no", "sector", "pin code", "pincode",
        "gstin", "gst no", "fssai", "cin no", "cin:", "pan no", "pan:",
        "phone", "tel:", "mobile", "ph:", "mob:", "email:", "www.", "http",
        "date:", "time:", "dated:"
    )

    /**
     * Extract the most likely vendor/business name from [ocrText].
     */
    fun extract(ocrText: String): String? {
        if (ocrText.isBlank()) return null

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Scan the top portion of the receipt (first 8 lines)
        val candidates = lines.take(8)

        for (line in candidates) {
            if (isValidVendorLine(line)) {
                return cleanVendorName(line)
            }
        }
        return null
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun isValidVendorLine(line: String): Boolean {
        // Too short or too long
        if (line.length < MIN_VENDOR_NAME_LENGTH) return false
        if (line.length > MAX_VENDOR_NAME_LENGTH) return false

        val lower = line.lowercase().trim()

        // Reject exact or prefix generic document headers ("TAX INVOICE", "RECEIPT", etc.)
        if (GENERIC_HEADER_TOKENS.any { lower == it || lower.startsWith("$it ") || lower.startsWith("$it:") || lower.startsWith("$it -") }) {
            return false
        }

        // Reject purely numeric lines or dates
        if (line.replace(" ", "").all { it.isDigit() || it == '.' || it == ',' || it == '/' || it == '-' || it == ':' }) {
            return false
        }

        // Reject lines with date formats like DD/MM/YYYY or DD-MM-YYYY
        if (lower.matches(Regex(""".*\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b.*"""))) {
            return false
        }

        // Reject lines with too many digits (item quantity/price lines)
        val digitRatio = line.count { it.isDigit() }.toFloat() / line.length
        if (digitRatio > 0.35f) return false

        // Reject lines that look like addresses or registration metadata
        if (ADDRESS_METADATA_TOKENS.any { lower.contains(it) }) return false

        // Reject lines with 2 or more commas (address indicator)
        if (line.count { it == ',' } >= 2) return false

        return true
    }

    private fun cleanVendorName(line: String): String {
        return line.trimStart('-', '*', '#', ':', ' ')
            .trimEnd('-', '*', '#', ':', ' ')
            .trim()
    }
}
