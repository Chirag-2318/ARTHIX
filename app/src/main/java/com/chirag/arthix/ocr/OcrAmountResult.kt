package com.chirag.arthix.ocr

/**
 * Result type for [OcrAmountExtractor.extract].
 *
 * Kept as a sealed class so every call site must handle all cases —
 * no silent drop of a failed extraction.
 */
sealed class OcrAmountResult {

    /**
     * An amount was found and passed sanity bounds.
     *
     * @param amountPaise integer paise (EC-46, never Float).
     * @param isKeywordMatch true if the amount was found adjacent to a
     *   "Total"/"Grand Total"/"Amount Payable" keyword (EC-30, CLEAN path).
     *   false means it came from the largest-number fallback (NEEDS_REVIEW path).
     */
    data class Found(
        val amountPaise: Long,
        val isKeywordMatch: Boolean,
    ) : OcrAmountResult()

    /**
     * An amount candidate was found, but it falls outside the plausible range
     * (< ₹1 or > ₹50,000). Caller must route to manual confirmation (EC-32).
     *
     * @param rawText the raw extracted text for debug / prefill purposes.
     */
    data class OutOfBounds(val rawText: String) : OcrAmountResult()

    /** No usable amount could be extracted from the text at all. */
    object NotFound : OcrAmountResult()
}
