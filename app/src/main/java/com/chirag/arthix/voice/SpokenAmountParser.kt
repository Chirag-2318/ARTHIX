package com.chirag.arthix.voice

import com.chirag.arthix.util.AmountParseResult
import com.chirag.arthix.util.AmountParser

/**
 * Parses spoken-number text into integer paise.
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Scope (EC-29)
 * Supports English number words and common Indian-English hybrid forms
 * as spoken by this persona. Hindi number words are explicitly OUT OF SCOPE
 * for this hackathon build — stated as an accepted limitation, not a silent gap.
 * Supported: "four fifty", "twelve hundred", "two thousand three hundred fifty",
 * "four hundred and fifty rupees", mixed digit+word: "4 fifty", "2 thousand".
 *
 * ## Algorithm
 * 1. Normalize: strip "rupees"/"rs"/"inr"/punctuation, lowercase.
 * 2. Tokenize into word/digit groups.
 * 3. Evaluate using a simple accumulator:
 *    - Multiplier words ("hundred", "thousand", "lakh") multiply the running group.
 *    - Additive words/digits add to the current group.
 *    - "and" is a no-op connector.
 * 4. Convert final rupee value to paise (* 100).
 *
 * If the input looks like a plain numeric string (e.g. "450"), delegates to
 * [AmountParser] directly rather than through the word-parser.
 */
object SpokenAmountParser {

    // Words that this parser skips (connectors, currency markers)
    private val SKIP_WORDS = setOf("and", "rupees", "rupee", "rs", "inr", "only", "paisa", "paise")

    // Number word → integer value mapping
    private val NUMBER_WORDS = mapOf(
        "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L,
        "four" to 4L, "five" to 5L, "six" to 6L, "seven" to 7L,
        "eight" to 8L, "nine" to 9L, "ten" to 10L,
        "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L,
        "fourteen" to 14L, "fifteen" to 15L, "sixteen" to 16L,
        "seventeen" to 17L, "eighteen" to 18L, "nineteen" to 19L,
        "twenty" to 20L, "thirty" to 30L, "forty" to 40L,
        "fifty" to 50L, "sixty" to 60L, "seventy" to 70L,
        "eighty" to 80L, "ninety" to 90L,
    )

    // Multiplier words → scale factor
    private val MULTIPLIERS = mapOf(
        "hundred" to 100L,
        "thousand" to 1_000L,
        "lakh" to 100_000L,
        "lac" to 100_000L,
    )

    /**
     * Attempt to parse [text] as a spoken amount.
     *
     * @return amount in paise on success, null if the text cannot be interpreted.
     */
    fun parse(text: String): Long? {
        if (text.isBlank()) return null

        val normalized = normalize(text)

        // Fast path: if it's already a plain/currency-prefixed numeric, use AmountParser.
        val directParse = AmountParser.parse(normalized)
        if (directParse is AmountParseResult.Success) {
            return directParse.amountPaise
        }

        // Word-number path
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return evalTokens(tokens)
    }

    // ── Private ────────────────────────────────────────────────────────────────

    /**
     * Strips currency markers, punctuation, and lowercases.
     */
    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[₹,.]"), " ")   // ₹ and separators become spaces; don't strip decimal yet
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Converts a list of normalized tokens (words + digit strings) into a rupee value,
     * then converts to paise.  Returns null if no numeric content found.
     *
     * The algorithm handles:
     * - "four fifty"                → 4*100 + 50     = 450 (hundred implied before tens+units ≤ 99)
     * - "twelve hundred"           → 12 * 100        = 1200
     * - "two thousand three hundred fifty" → 2350
     * - "four hundred and fifty"   → 450
     * - "4 fifty"                  → 450
     */
    private fun evalTokens(tokens: List<String>): Long? {
        var total = 0L
        var current = 0L
        var hadAnyNumber = false

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            if (token in SKIP_WORDS) { i++; continue }

            val multiplier = MULTIPLIERS[token]
            if (multiplier != null) {
                // If "hundred" and current is 0, treat as "one hundred"
                if (current == 0L) current = 1L
                current *= multiplier
                if (multiplier >= 1_000) {
                    // "thousand" / "lakh" flushes current into total
                    total += current
                    current = 0L
                }
                hadAnyNumber = true
                i++
                continue
            }

            val wordValue = NUMBER_WORDS[token]
            if (wordValue != null) {
                // If a tens word follows a previous value that was < 100 and we haven't
                // seen a multiplier, it's a "twelve fifty" → 1250 pattern only when
                // previous current ≥ 10. Handle "four fifty" as 4*100 + 50.
                if (wordValue in 10L..99L && current > 0L && current < 100L) {
                    // E.g. "four fifty": current=4, token="fifty" → interpret as hundreds + tens
                    // "four" (4) + "fifty" (50) where "four" implied hundreds
                    total += current * 100L
                    current = wordValue
                } else {
                    current += wordValue
                }
                hadAnyNumber = true
                i++
                continue
            }

            // Try as a plain integer (handles "4 fifty", "2000" etc.)
            val longVal = token.toLongOrNull()
            if (longVal != null) {
                if (longVal in 10L..99L && current > 0L && current < 100L) {
                    total += current * 100L
                    current = longVal
                } else {
                    current += longVal
                }
                hadAnyNumber = true
                i++
                continue
            }

            // Unrecognized token — if we already parsed a valid amount group, stop to prevent adding later counts
            if (hadAnyNumber && (current > 0L || total > 0L)) {
                total += current
                current = 0L
                break
            }
            i++
        }

        if (!hadAnyNumber) return null

        total += current
        return if (total <= 0L) null else total * 100L  // rupees → paise
    }
}
