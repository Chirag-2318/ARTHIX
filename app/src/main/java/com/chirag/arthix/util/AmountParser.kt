package com.chirag.arthix.util

/**
 * Result type for [AmountParser.parse].
 */
sealed class AmountParseResult {
    /** Successfully parsed amount in integer paise. */
    data class Success(val amountPaise: Long) : AmountParseResult()

    /** Parsing failed — [reason] describes why for debugging. */
    data class Failure(val reason: String) : AmountParseResult()
}

/**
 * Single shared entry point for amount normalization, called by both
 * Phase 2 (notification text) and Phase 4 (OCR text).
 *
 * Behavior contract (PRD §7, EC-11):
 * - Strips leading currency markers: ₹, Rs., Rs, INR (case-insensitive,
 *   optional trailing period, optional surrounding whitespace).
 * - Strips thousands separators (`,`) before numeric parsing.
 * - Decimal component maps to paise: `1450.5` → `145050`; `1450` → `145000`.
 * - Decimal component beyond 2 digits → Failure (ambiguous sub-paise).
 * - Negative amounts → Failure.
 * - Empty string, non-numeric garbage, multiple decimal points → Failure.
 * - No sanity-bound rejection (e.g. >₹50,000) — that's caller-side policy.
 *
 * This utility has **zero** Android framework dependency, so it can be
 * unit-tested on the JVM without Robolectric.
 */
object AmountParser {

    // Currency prefixes to strip, ordered longest-first to avoid partial matches.
    // Case-insensitive matching is handled separately.
    private val CURRENCY_PREFIX_REGEX = Regex(
        """^[\s]*(₹|INR|Rs\.?)\s*""",
        RegexOption.IGNORE_CASE
    )

    fun parse(raw: String): AmountParseResult {
        val trimmed = raw.trim()

        if (trimmed.isEmpty()) {
            return AmountParseResult.Failure("Empty input")
        }

        // Check for negative sign before or after currency marker
        if (trimmed.contains('-')) {
            return AmountParseResult.Failure("Negative amounts are not valid")
        }

        // Strip currency prefix
        val afterPrefix = CURRENCY_PREFIX_REGEX.replace(trimmed, "").trim()

        if (afterPrefix.isEmpty()) {
            return AmountParseResult.Failure("No numeric content after stripping currency prefix")
        }

        // Validate comma placement before stripping:
        // Reject malformed separator patterns like "1,4,50" (not standard Indian/Western grouping).
        // We accept: no commas, or standard patterns where commas separate digit groups properly.
        if (!isValidCommaPlacement(afterPrefix)) {
            return AmountParseResult.Failure("Malformed separator placement")
        }

        // Strip commas
        val withoutCommas = afterPrefix.replace(",", "")

        // Validate: must be a valid decimal number at this point
        if (withoutCommas.count { it == '.' } > 1) {
            return AmountParseResult.Failure("Multiple decimal points")
        }

        // Split into integer and decimal parts
        val parts = withoutCommas.split(".")

        // Validate integer part is all digits
        val integerPart = parts[0]
        if (integerPart.isEmpty() || !integerPart.all { it.isDigit() }) {
            return AmountParseResult.Failure("Non-numeric content in integer part")
        }

        val rupees = integerPart.toLongOrNull()
            ?: return AmountParseResult.Failure("Integer part out of range")

        // Handle decimal part (paise)
        val paise: Long = if (parts.size == 2) {
            val decimalPart = parts[1]
            if (decimalPart.isEmpty()) {
                // Trailing decimal point like "450." — treat as zero paise
                0L
            } else if (decimalPart.length > 2) {
                return AmountParseResult.Failure("Decimal component beyond 2 digits is ambiguous")
            } else if (!decimalPart.all { it.isDigit() }) {
                return AmountParseResult.Failure("Non-numeric content in decimal part")
            } else {
                // Pad single-digit decimal: "450.5" → 50 paise, not 5 paise
                val padded = decimalPart.padEnd(2, '0')
                padded.toLong()
            }
        } else {
            0L
        }

        val totalPaise = rupees * 100 + paise
        return AmountParseResult.Success(totalPaise)
    }

    /**
     * Validates comma placement in the numeric string.
     *
     * Accepts:
     * - No commas: "1450", "1450.50"
     * - Standard Western grouping: "1,450", "12,450", "1,234,567"
     * - Indian grouping: "1,45,000", "12,34,567"
     *
     * Rejects:
     * - Malformed: "1,4,50", ",450", "450,", "1,,450"
     * - Commas in decimal part
     */
    private fun isValidCommaPlacement(input: String): Boolean {
        if (!input.contains(',')) return true

        // Split off decimal part — commas only valid in integer part
        val integerPart = input.split(".")[0]
        val decimalPart = if (input.contains(".")) input.substringAfter(".") else null

        // No commas allowed in decimal portion
        if (decimalPart != null && decimalPart.contains(',')) return false

        // Must not start or end with comma, no consecutive commas
        if (integerPart.startsWith(",") || integerPart.endsWith(",")) return false
        if (integerPart.contains(",,")) return false

        // Split by commas and validate each group
        val groups = integerPart.split(",")
        if (groups.size < 2) return false

        // First group: 1-3 digits (leading group)
        if (groups[0].isEmpty() || groups[0].length > 3) return false
        if (!groups[0].all { it.isDigit() }) return false

        // Remaining groups: must be exactly 2 or 3 digits each,
        // but all remaining groups must have the same width
        // (Western = all 3, Indian = all 2 except possibly the last)
        // Simplification: accept groups of 2 or 3 digits.
        for (i in 1 until groups.size) {
            if (groups[i].length !in 2..3) return false
            if (!groups[i].all { it.isDigit() }) return false
        }

        return true
    }
}
