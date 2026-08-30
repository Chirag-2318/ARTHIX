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
 *    Scan each line for a "total" keyword. When found, extract the amount
 *    from the same line first; if not on the same line, check the very
 *    next non-blank line. Return the first keyword-adjacent amount found.
 *
 * 2. **Largest-number fallback** (NEEDS_REVIEW confidence, EC-30):
 *    If no keyword match was found, parse every number in the full text
 *    and return the largest valid value. This is explicitly flagged as
 *    low-confidence so the caller and the UI can surface it for review.
 *
 * 3. **Sanity bounds** (EC-32):
 *    Any candidate — from either pass — below ₹1 (100 paise) or above
 *    ₹50,000 (5_000_000 paise) is returned as [OcrAmountResult.OutOfBounds].
 *    Caller must route to manual confirmation; never silently commit.
 *
 * ## Reuse of AmountParser
 * All numeric text normalisation (₹/Rs./INR prefix stripping, thousand
 * separator handling, paise conversion) is delegated to the shared
 * [AmountParser] already defined in Phase 0, so OCR and notification
 * parsing stay consistent (EC-11).
 */
object OcrAmountExtractor {

    /**
     * Minimum plausible transaction amount in paise (₹1 = 100 paise).
     * Below this, the extracted value is almost certainly a misread (EC-32).
     */
    const val MIN_AMOUNT_PAISE = 100L      // ₹1

    /**
     * Maximum plausible transaction amount in paise (₹50,000 = 5,000,000 paise).
     * Above this, an OCR misread (e.g. ₹ symbol mis-identified, decimal off by
     * one position) is overwhelmingly likely (EC-32).
     */
    const val MAX_AMOUNT_PAISE = 5_000_000L  // ₹50,000

    /**
     * Keyword set for the proximity-match pass (EC-30).
     * Matched case-insensitively. Longest strings first to prevent "total"
     * from matching before "grand total" on the same line.
     */
    private val AMOUNT_KEYWORDS = listOf(
        "grand total",
        "amount payable",
        "net amount",
        "total amount",
        "total payable",
        "bill total",
        "total",
    )

    /**
     * Extract the most plausible payment amount from [ocrText].
     *
     * @param ocrText the raw multi-line text block produced by ML Kit.
     * @return an [OcrAmountResult] — never throws.
     */
    fun extract(ocrText: String): OcrAmountResult {
        if (ocrText.isBlank()) return OcrAmountResult.NotFound

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // ── Pass 1: keyword-proximity ──────────────────────────────────────────
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

    /**
     * Scans [lines] for any [AMOUNT_KEYWORDS] hit.
     * When a keyword line is found:
     *  - First tries to parse an amount from the portion AFTER the keyword on the same line.
     *  - If no amount on the keyword line, tries the very next non-blank line.
     * Returns the first successful paise value, or null if none found.
     *
     * Works on lowercased lines so matching is case-insensitive, since AmountParser
     * handles ₹/Rs./INR normalisation anyway.
     */
    private fun extractByKeyword(lines: List<String>): Long? {
        for (i in lines.indices) {
            val lineOriginal = lines[i]
            val lineLower = lineOriginal.lowercase()

            val matchedKeyword = AMOUNT_KEYWORDS.firstOrNull { lineLower.contains(it) }
                ?: continue

            // Extract the portion after the keyword (case-insensitive).
            // Strip leading delimiter characters (:, -, spaces, tabs) before parsing.
            val afterKeyword = lineLower
                .substringAfter(matchedKeyword)
                .trimStart(':', '-', ' ', '\t', '.')
                .trim()

            if (afterKeyword.isNotEmpty()) {
                val parsed = tryParseAmount(afterKeyword)
                if (parsed != null) return parsed
            }

            // Try the next non-blank line.
            val nextLine = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
            if (nextLine != null) {
                val parsed = tryParseAmount(nextLine.trim())
                if (parsed != null) return parsed
            }
        }
        return null
    }


    /**
     * Parses every token in [lines] through [AmountParser] and returns the
     * largest valid paise value found, or null if none parseable.
     *
     * Tokens are split on whitespace. Non-numeric tokens are skipped silently.
     */
    private fun extractLargestNumber(lines: List<String>): Long? {
        var largest: Long? = null
        for (line in lines) {
            // Split on whitespace; each token is tried independently.
            for (token in line.split(Regex("\\s+"))) {
                val paise = tryParseAmount(token) ?: continue
                if (largest == null || paise > largest) {
                    largest = paise
                }
            }
        }
        return largest
    }

    /**
     * Tries to parse [text] through [AmountParser].
     * Returns the paise value on success, null on any failure.
     */
    private fun tryParseAmount(text: String): Long? {
        if (text.isBlank()) return null
        return when (val result = AmountParser.parse(text)) {
            is AmountParseResult.Success -> result.amountPaise
            is AmountParseResult.Failure -> null
        }
    }

    /**
     * Applies the sanity bounds check (EC-32) to a candidate [paise] value.
     * Returns [OcrAmountResult.OutOfBounds] with [rawText] if out of range.
     */
    private fun checkBounds(paise: Long, isKeywordMatch: Boolean): OcrAmountResult {
        return if (paise < MIN_AMOUNT_PAISE || paise > MAX_AMOUNT_PAISE) {
            OcrAmountResult.OutOfBounds(rawText = paise.toString())
        } else {
            OcrAmountResult.Found(amountPaise = paise, isKeywordMatch = isKeywordMatch)
        }
    }
}
