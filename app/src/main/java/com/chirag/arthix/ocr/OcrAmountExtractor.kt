package com.chirag.arthix.ocr

import com.chirag.arthix.util.AmountParser
import com.chirag.arthix.util.AmountParseResult

/**
 * Extracts a payment amount from raw OCR text.
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Algorithm (EC-30)
 * 1. **Keyword-proximity pass** (preferred, CLEAN confidence):
 *    Scan each line for prioritized "total" keywords. When found, extract
 *    the amount using regex from the same line; if not on the same line,
 *    check the next valid non-blank line. Return the first keyword-adjacent amount found.
 *
 * 2. **Negative identifier filtering**:
 *    Lines containing non-financial identifiers ("Bill No: 64", "Order #64",
 *    "Table 4", "Token 12", "Phone: ...") are strictly excluded from amount extraction.
 *
 * 3. **Largest-number fallback** (NEEDS_REVIEW confidence, EC-30):
 *    If no keyword match was found, parse candidates from valid non-metadata lines
 *    and return the largest plausible value.
 *
 * 4. **Sanity bounds** (EC-32):
 *    Any candidate below ₹1 (100 paise) or above ₹50,000 (5,000,000 paise)
 *    is returned as [OcrAmountResult.OutOfBounds].
 */
object OcrAmountExtractor {

    /** Minimum plausible transaction amount in paise (₹1 = 100 paise). */
    const val MIN_AMOUNT_PAISE = 100L      // ₹1

    /** Maximum plausible transaction amount in paise (₹50,000 = 5,000,000 paise). */
    const val MAX_AMOUNT_PAISE = 5_000_000L  // ₹50,000

    /**
     * Keyword set for the proximity-match pass (EC-30).
     * Matched case-insensitively. Ordered by specificity (most specific first).
     */
    private val AMOUNT_KEYWORDS = listOf(
        "grand total",
        "amount payable",
        "net payable",
        "total payable",
        "total amount",
        "final total",
        "final amount",
        "order total",
        "bill total",
        "to pay",
        "net amount",
        "total bill",
        "paid amount",
        "amount paid",
        "balance due",
        "bill amt",
        "total amt",
        "total inr",
        "total rs",
        "total",
        "item total",
        "items total",
        "subtotal",
        "sub total",
        "sub-total",
    )

    /**
     * Lines containing these tokens are non-amount metadata (bill numbers, dates, phones, etc.)
     * and should not be parsed as transaction amounts.
     */
    private val NEGATIVE_LINE_KEYWORDS = listOf(
        "bill no", "bill #", "bill num", "bill number",
        "invoice no", "invoice #", "invoice num", "invoice number",
        "order id", "order no", "order #", "order num", "order number",
        "token no", "token #", "table no", "table #", "table:",
        "kot no", "kot #", "kot:",
        "gstin", "gst no", "fssai", "cin no", "cin:",
        "phone", "tel:", "mobile", "ph:", "mob:",
        "pax:", "cashier", "waiter", "steward",
        "item count", "total items count", "total qty", "qty total"
    )

    // Regex matching amounts with optional currency symbol and trailing formatting
    private val AMOUNT_PATTERN = Regex(
        """(?:₹|INR|Rs\.?)\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2}|(?:\.[oO]{2}))?)\s*(?:/-|/=|\*)?""",
        RegexOption.IGNORE_CASE
    )
    private val NUMERIC_PATTERN = Regex(
        """\b([0-9]+(?:,[0-9]+)*(?:\.[0-9]{1,2}|(?:\.[oO]{2}))?)\s*(?:/-|/=|\*)?""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extract the most plausible payment amount from [ocrText].
     */
    fun extract(ocrText: String): OcrAmountResult {
        if (ocrText.isBlank()) return OcrAmountResult.NotFound

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // ── Pass 1: keyword-proximity (by keyword priority) ───────────────────
        val keywordResult = extractByKeyword(lines)
        if (keywordResult != null) {
            return checkBounds(keywordResult, isKeywordMatch = true)
        }

        // ── Pass 2: largest-number fallback ────────────────────────────────────
        val largestResult = extractLargestNumber(lines)
        if (largestResult != null) {
            return checkBounds(largestResult, isKeywordMatch = false)
        }

        return OcrAmountResult.NotFound
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun extractByKeyword(lines: List<String>): Long? {
        // Iterate by keyword priority first so Grand Total / To Pay / Total Payable beat Subtotal / Item total
        for (keyword in AMOUNT_KEYWORDS) {
            for (i in lines.indices) {
                val lineOriginal = lines[i]
                val lineLower = lineOriginal.lowercase()

                if (!lineLower.contains(keyword)) continue

                // If the line is strictly a negative metadata line (e.g. "Bill No: 64") without an actual total keyword
                if (isNegativeLine(lineLower) && !lineLower.startsWith(keyword) && !lineLower.contains("total:") && !lineLower.contains("to pay")) {
                    continue
                }

                // 1. Try extracting an amount on the same line after or around the keyword
                val keywordIdx = lineLower.indexOf(keyword)
                val afterKeyword = lineOriginal.substring(keywordIdx + keyword.length)
                val amountFromSameLine = extractBestAmountFromText(afterKeyword).ifNull {
                    extractBestAmountFromText(lineOriginal)
                }

                if (amountFromSameLine != null) {
                    return amountFromSameLine
                }

                // 2. Try the next non-blank line (if not a negative metadata line)
                val nextLine = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
                if (nextLine != null && !isNegativeLine(nextLine.lowercase())) {
                    val parsed = extractBestAmountFromText(nextLine)
                    if (parsed != null) return parsed
                }
            }
        }
        return null
    }

    private fun extractLargestNumber(lines: List<String>): Long? {
        var largest: Long? = null
        for (line in lines) {
            val lineLower = line.lowercase()
            // Exclude lines with negative metadata (Bill No, Order #, Phone, Date, GSTIN, etc.)
            if (isNegativeLine(lineLower)) continue

            val amountsInLine = findAllAmountsInText(line)
            for (paise in amountsInLine) {
                if (largest == null || paise > largest) {
                    largest = paise
                }
            }
        }
        return largest
    }

    /**
     * Extracts the best candidate amount from a line or snippet.
     * Prioritizes amounts with currency symbols.
     */
    private fun extractBestAmountFromText(text: String): Long? {
        if (text.isBlank()) return null

        // 1. Search for currency-prefixed amounts
        val currencyMatches = AMOUNT_PATTERN.findAll(text).toList()
        for (match in currencyMatches) {
            val rawCandidate = match.value
            val parsed = cleanAndParseAmount(rawCandidate)
            if (parsed != null) return parsed
        }

        // 2. Search for plain decimal or integer numbers
        val numericMatches = NUMERIC_PATTERN.findAll(text).toList()
        for (match in numericMatches) {
            val rawCandidate = match.groupValues[1]
            val parsed = cleanAndParseAmount(rawCandidate)
            if (parsed != null) return parsed
        }

        return null
    }

    private fun findAllAmountsInText(text: String): List<Long> {
        val results = mutableListOf<Long>()
        val matches = NUMERIC_PATTERN.findAll(text)
        for (match in matches) {
            val parsed = cleanAndParseAmount(match.groupValues[1])
            if (parsed != null) {
                results.add(parsed)
            }
        }
        return results
    }

    /**
     * Cleans OCR artifacts (.oo/.OO -> .00, trailing /-, etc.) and parses using AmountParser.
     */
    private fun cleanAndParseAmount(raw: String): Long? {
        var cleaned = raw.trim()
            .replace(Regex("""\.[oO]{2}"""), ".00")
            .replace(Regex("""\.[oO]([0-9])"""), ".0$1")
            .replace(Regex("""\.([0-9])[oO]"""), ".$10")
            .trimEnd('/', '-', '=', '*', ')', ']', ',', ' ')

        // Strip currency words/symbols
        cleaned = cleaned.replace(Regex("""^(?:₹|INR|Rs\.?)\s*""", RegexOption.IGNORE_CASE), "").trim()

        if (cleaned.isBlank()) return null
        return when (val result = AmountParser.parse(cleaned)) {
            is AmountParseResult.Success -> result.amountPaise
            is AmountParseResult.Failure -> null
        }
    }

    private fun isNegativeLine(lineLower: String): Boolean {
        return NEGATIVE_LINE_KEYWORDS.any { lineLower.contains(it) }
    }

    private inline fun Long?.ifNull(block: () -> Long?): Long? = this ?: block()

    private fun checkBounds(paise: Long, isKeywordMatch: Boolean): OcrAmountResult {
        return if (paise < MIN_AMOUNT_PAISE || paise > MAX_AMOUNT_PAISE) {
            OcrAmountResult.OutOfBounds(rawText = paise.toString())
        } else {
            OcrAmountResult.Found(amountPaise = paise, isKeywordMatch = isKeywordMatch)
        }
    }
}
