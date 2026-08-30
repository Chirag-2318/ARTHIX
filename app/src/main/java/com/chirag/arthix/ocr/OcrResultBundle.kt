package com.chirag.arthix.ocr

import com.chirag.arthix.data.model.ConfidenceFlag

/**
 * The normalized output of the full OCR pipeline for one receipt capture.
 *
 * This is what [ReceiptCaptureActivity] builds and hands to the routing logic.
 * Phase 3's [ManualEntryPrefill] is constructed directly from this bundle —
 * the two types are intentionally separate so neither module needs to import
 * the other's internals.
 *
 * @param amountPaise extracted amount in integer paise (EC-46). Null when
 *   extraction produced [OcrAmountResult.NotFound] or [OcrAmountResult.OutOfBounds].
 * @param payee extracted vendor/payee name, or null if no reliable name
 *   was found (EC-33 — never insert garbage).
 * @param confidenceFlag CLEAN when a keyword match found the amount;
 *   NEEDS_REVIEW for fallback-largest-number or sanity-bound triggers (EC-30, EC-32).
 * @param rawText the full OCR text block — retained for debugging and for
 *   passing partial strings to the manual prefill screen (EC-31).
 * @param isLowConfidence true when the caller should route to manual prefill
 *   instead of showing a summary chip. Computed from [confidenceFlag] and whether
 *   [amountPaise] is null (caller checks both conditions together).
 */
data class OcrResultBundle(
    val amountPaise: Long?,
    val payee: String?,
    val confidenceFlag: ConfidenceFlag,
    val rawText: String,
    val isLowConfidence: Boolean,
)
