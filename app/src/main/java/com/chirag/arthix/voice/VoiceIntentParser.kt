package com.chirag.arthix.voice

/**
 * Parses a normalized STT transcript into a structured [VoiceIntent].
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Intent priority order (highest first)
 * 1. Discard  — hard keyword match, checked FIRST (EC-25)
 * 2. Split    — "split with" trigger phrase (FR-6 contract)
 * 3. CategoryAndAmount — both a category keyword AND an amount in one utterance
 * 4. Amount   — only an amount (for AWAITING_AMOUNT records, EC-24)
 * 5. Category — only a category keyword
 * 6. Unclear  — nothing matched
 *
 * ## Category fuzzy matching (EC-28)
 * Levenshtein distance ≤ 2 against each of the four canonical categories,
 * PLUS a keyword synonym map (e.g. "restaurant"→Food, "cab"→Travel).
 * Original spoken phrase is preserved in [VoiceIntent.Category.originalPhrase]
 * as a sub-tag — never discarded.
 *
 * ## Language scope (EC-29)
 * English and common Indian-English code-switch phrases supported.
 * Pure Hindi number words and category names are out of scope for this build —
 * documented as an accepted limitation, not a silent gap.
 */
object VoiceIntentParser {

    /** Fixed category taxonomy (EC-28). */
    val CATEGORIES = listOf("food", "travel", "shopping", "other")

    /**
     * Synonym map: spoken word → canonical category.
     * Keys are lowercase single words or short phrases.
     */
    private val CATEGORY_SYNONYMS = mapOf(
        // Food
        "restaurant" to "food", "cafe" to "food", "coffee" to "food",
        "lunch" to "food", "dinner" to "food", "breakfast" to "food",
        "snack" to "food", "swiggy" to "food", "zomato" to "food",
        "chai" to "food", "tea" to "food", "grocery" to "food",
        "groceries" to "food", "vegetables" to "food", "fruit" to "food",
        "sabzi" to "food", "khana" to "food", "bhojan" to "food",
        // Travel
        "cab" to "travel", "auto" to "travel", "bus" to "travel",
        "train" to "travel", "metro" to "travel", "ola" to "travel",
        "uber" to "travel", "rapido" to "travel", "petrol" to "travel",
        "fuel" to "travel", "flight" to "travel", "ticket" to "travel",
        "transport" to "travel", "taxi" to "travel", "rickshaw" to "travel",
        // Shopping
        "clothes" to "shopping", "shirt" to "shopping", "shoes" to "shopping",
        "amazon" to "shopping", "flipkart" to "shopping", "mall" to "shopping",
        "shop" to "shopping", "store" to "shopping", "purchase" to "shopping",
        "buy" to "shopping", "online" to "shopping",
        // Other
        "bill" to "other", "electricity" to "other", "medicine" to "other",
        "medical" to "other", "hospital" to "other", "misc" to "other",
        "miscellaneous" to "other", "recharge" to "other",
    )

    /**
     * Discard keywords (EC-25). Checked as whole-word presence in the transcript.
     * Order doesn't matter — any match → Discard.
     */
    private val DISCARD_KEYWORDS = setOf(
        "skip", "not real", "ignore", "cancel", "discard",
        "nope", "none", "no", "delete", "remove", "wrong",
    )

    /**
     * Parse [transcript] (lower-cased, trimmed) into a [VoiceIntent].
     * [transcript] should already be normalized to lowercase by the caller.
     */
    fun parse(transcript: String): VoiceIntent {
        val text = transcript.lowercase().trim()
        if (text.isBlank()) return VoiceIntent.Unclear

        // ── 1. Discard (EC-25) ──────────────────────────────────────────────
        if (isDiscardIntent(text)) return VoiceIntent.Discard

        // ── 2. Split (FR-6 contract) ────────────────────────────────────────
        val splitIntent = parseSplitIntent(text)
        if (splitIntent != null) return splitIntent

        // ── 3 + 4 + 5. Category and/or Amount ──────────────────────────────
        val category = resolveCategory(text)
        val amount = SpokenAmountParser.parse(text)

        return when {
            category != null && amount != null ->
                VoiceIntent.CategoryAndAmount(category, amount, originalPhrase = text)
            amount != null ->
                VoiceIntent.Amount(amount)
            category != null ->
                VoiceIntent.Category(category, originalPhrase = text)
            else ->
                VoiceIntent.Unclear
        }
    }

    private val EXCLUDED_CATEGORY_WORDS = setOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
        "hundred", "thousand", "lakh", "lac", "crore", "rupee", "rupees", "rs", "inr", "paise", "paisa", "only"
    )

    // ── Category resolution (EC-28) ────────────────────────────────────────────

    /**
     * Resolves [text] to a canonical category or null.
     *
     * Priority:
     * 1. Exact match to a canonical category word present in text
     * 2. Synonym map lookup (any word in text matches a synonym)
     * 3. Levenshtein distance against each canonical category (max dist 1 for length <= 4, 2 for longer)
     */
    fun resolveCategory(text: String): String? {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Exact canonical match
        for (word in words) {
            if (word in CATEGORIES) return word
        }

        // Synonym lookup
        for (word in words) {
            val syn = CATEGORY_SYNONYMS[word]
            if (syn != null) return syn
        }
        // Multi-word synonym lookup
        for ((synonym, category) in CATEGORY_SYNONYMS) {
            if (synonym.contains(' ') && text.contains(synonym)) return category
        }

        // Levenshtein fuzzy match (EC-28) — ignore number words and digit tokens
        for (word in words) {
            if (word in EXCLUDED_CATEGORY_WORDS || word.all { it.isDigit() }) continue

            val maxDist = if (word.length <= 4) 1 else 2
            for (category in CATEGORIES) {
                if (levenshtein(word, category) <= maxDist) return category
            }
        }

        return null
    }

    // ── Discard detection (EC-25) ──────────────────────────────────────────────

    private fun isDiscardIntent(text: String): Boolean {
        // Single-word match
        val words = text.split(Regex("\\s+"))
        for (word in words) {
            if (word in DISCARD_KEYWORDS) return true
        }
        // Multi-word phrase match (e.g. "not real", "ignore that one")
        for (keyword in DISCARD_KEYWORDS) {
            if (keyword.contains(' ') && text.contains(keyword)) return true
        }
        return false
    }

    // ── Split intent (FR-6) ────────────────────────────────────────────────────

    /**
     * Detects "split with X and Y" patterns and extracts name candidates.
     * Ambiguous names (matching multiple contacts) are surfaced by returning
     * all candidates — Phase 6 renders the tap-to-pick resolution UI (EC-36).
     */
    private fun parseSplitIntent(text: String): VoiceIntent.Split? {
        val splitTriggers = listOf("split with", "divide with", "share with")
        val trigger = splitTriggers.firstOrNull { text.contains(it) } ?: return null

        val afterTrigger = text.substringAfter(trigger).trim()
        if (afterTrigger.isBlank()) return null

        // Split on "and", "," connectors → individual name candidates with capitalized first letters
        val names = afterTrigger
            .split(Regex("\\s*,\\s*|\\s+and\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
            .map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }

        return if (names.isNotEmpty()) VoiceIntent.Split(names) else null
    }

    // ── Levenshtein distance (EC-28) ───────────────────────────────────────────

    /**
     * Classic dynamic-programming Levenshtein distance.
     * Used only for short category-word strings (≤ 10 chars) so O(n²) is fine.
     */
    fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[m][n]
    }
}
