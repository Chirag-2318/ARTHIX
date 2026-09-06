package com.chirag.arthix.ocr

import java.time.LocalDate

/**
 * Result of running [OcrDateExtractor] on raw OCR text.
 */
sealed interface OcrDateResult {

    /**
     * A valid receipt transaction date was successfully extracted.
     *
     * @param epochMillis Epoch timestamp in milliseconds (at start of day or noon UTC).
     * @param localDate The parsed [LocalDate].
     * @param isKeywordMatch True if found adjacent to a positive keyword anchor (e.g. "Date:", "Bill Date:").
     * @param isAmbiguous True if day and month could not be distinguished with certainty (e.g. 03/04/2026),
     *   defaulted to DD/MM per Indian convention but requiring low-confidence review.
     * @param rawSnippet The raw text substring that produced this date.
     */
    data class Found(
        val epochMillis: Long,
        val localDate: LocalDate,
        val isKeywordMatch: Boolean,
        val isAmbiguous: Boolean,
        val rawSnippet: String,
    ) : OcrDateResult

    /**
     * A date candidate was found, but it resolves to a future date.
     * Per design doc §2.6, future dates are rejected/flagged for manual confirmation.
     */
    data class FutureDate(
        val rawSnippet: String,
        val parsedDate: LocalDate,
    ) : OcrDateResult

    /**
     * No plausible receipt transaction date was found in the text.
     * Per design doc §2.7, this must never silently default to "today".
     */
    object NotFound : OcrDateResult
}
