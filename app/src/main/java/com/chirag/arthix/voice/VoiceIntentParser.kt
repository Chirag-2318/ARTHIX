package com.chirag.arthix.voice

import com.chirag.arthix.domain.category.TransactionCategoryAiClassifier

/**
 * Parses a normalized STT transcript into a structured [VoiceIntent].
 *
 * Zero Android framework imports — fully unit-testable on the JVM.
 *
 * ## Intent priority order (highest first)
 * 1. Discard  — hard keyword match, checked FIRST (EC-25)
 * 2. Split    — "split with", "amongst", "between" trigger phrase (FR-6 contract)
 * 3. CategoryAndAmount — both a category keyword AND an amount in one utterance
 * 4. Amount   — only an amount (for AWAITING_AMOUNT records, EC-24)
 * 5. Category — only a category keyword
 * 6. Unclear  — nothing matched
 *
 * ## Category matching
 * Leverages [TransactionCategoryAiClassifier] for 200+ merchants and categories,
 * plus keyword synonyms and Levenshtein fuzzy distance (EC-28).
 */
object VoiceIntentParser {

    /** Fixed category taxonomy. */
    val CATEGORIES = listOf("Food", "Travel", "Shopping", "Bills", "Groceries", "Other")

    /**
     * Synonym map: spoken word → canonical category.
     * Keys are lowercase single words or short phrases.
     */
    private val CATEGORY_SYNONYMS = mapOf(
        // Food
        "food" to "Food", "restaurant" to "Food", "cafe" to "Food", "coffee" to "Food",
        "lunch" to "Food", "dinner" to "Food", "breakfast" to "Food",
        "snack" to "Food", "swiggy" to "Food", "zomato" to "Food",
        "chai" to "Food", "tea" to "Food", "grocery" to "Groceries",
        "groceries" to "Groceries", "vegetables" to "Groceries", "fruit" to "Groceries",
        "sabzi" to "Groceries", "khana" to "Food", "bhojan" to "Food",
        // Travel
        "travel" to "Travel", "cab" to "Travel", "auto" to "Travel", "bus" to "Travel",
        "train" to "Travel", "metro" to "Travel", "ola" to "Travel",
        "uber" to "Travel", "rapido" to "Travel", "petrol" to "Travel",
        "fuel" to "Travel", "flight" to "Travel", "ticket" to "Travel",
        "transport" to "Travel", "taxi" to "Travel", "rickshaw" to "Travel",
        // Shopping
        "shopping" to "Shopping", "clothes" to "Shopping", "shirt" to "Shopping", "shoes" to "Shopping",
        "amazon" to "Shopping", "flipkart" to "Shopping", "mall" to "Shopping",
        "shop" to "Shopping", "store" to "Shopping", "purchase" to "Shopping",
        "buy" to "Shopping", "online" to "Shopping",
        // Bills
        "bills" to "Bills", "bill" to "Bills", "electricity" to "Bills", "recharge" to "Bills",
        "wifi" to "Bills", "broadband" to "Bills", "rent" to "Bills",
        // Other
        "other" to "Other", "medicine" to "Other",
        "medical" to "Other", "hospital" to "Other", "misc" to "Other",
        "miscellaneous" to "Other"
    )

    /**
     * Discard keywords (EC-25). Checked as whole-word presence in the transcript.
     * Order doesn't matter — any match → Discard.
     */
    private val DISCARD_KEYWORDS = setOf(
        "skip", "not real", "ignore", "cancel", "discard",
        "nope", "none", "no", "delete", "remove", "wrong"
    )

    private val INFLOW_KEYWORDS = setOf(
        "got", "received", "refunded", "credited", "from"
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
        val groupPattern = Regex("(?i)\\b(?:(?:one|two|three|four|five|six|seven|eight|nine|ten|\\d+)\\s+(?:people|persons|guys|friends|of us|members))|\\b(?:all of us|both of us|each of us|everyone|everybody|my friends)\\b")
        val cleanTextForAmount = groupPattern.replace(text, " ")
        val amount = SpokenAmountParser.parse(cleanTextForAmount) ?: SpokenAmountParser.parse(text)
        val category = resolveCategory(text)
        val payee = extractPayee(text, category)
        val isIncome = INFLOW_KEYWORDS.any { Regex("\\b$it\\b").containsMatchIn(text) }
        val direction = if (isIncome) com.chirag.arthix.data.model.Direction.INFLOW else com.chirag.arthix.data.model.Direction.OUTFLOW

        if (splitIntent != null) {
            return splitIntent.copy(
                amountPaise = amount,
                category = category,
                payee = payee ?: splitIntent.names.firstOrNull(),
                direction = direction,
            )
        }

        // ── 3 + 4 + 5. Category and/or Amount ──────────────────────────────
        return when {
            category != null && amount != null ->
                VoiceIntent.CategoryAndAmount(category, amount, originalPhrase = text, payee = payee, direction = direction)
            amount != null ->
                VoiceIntent.Amount(amount, payee = payee, direction = direction)
            category != null ->
                VoiceIntent.Category(category, originalPhrase = text, payee = payee, direction = direction)
            else ->
                VoiceIntent.Unclear
        }
    }

    private val EXCLUDED_CATEGORY_WORDS = setOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
        "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety",
        "hundred", "thousand", "lakh", "lac", "crore", "rupee", "rupees", "rs", "inr", "paise", "paisa", "only", "bucks"
    )

    private val PREPOSITIONS = setOf("to", "at", "for", "in", "from", "on", "with", "between", "among", "amongst", "across", "by")
    private val ACTION_WORDS = setOf("paid", "pay", "spent", "spend", "give", "gave", "transfer", "transferred", "send", "sent", "log", "add", "split", "splitting", "divide", "dividing", "share", "sharing", "bought", "buy", "purchase")
    private val ARTICLE_WORDS = setOf("the", "a", "an", "and", "or", "of", "my", "our")
    private val GROUP_DESCRIPTOR_WORDS = setOf("people", "persons", "guys", "friends", "members", "everyone", "everybody", "both", "all", "each")

    /**
     * Extracts a clean, human-readable payee / place / merchant from [text].
     * Examples:
     * - "splitting 450 on swiggy amongst three people ojas niranjan and chirag" -> "Swiggy"
     * - "450 to ojas" -> "Ojas"
     * - "500 at starbucks with rahul" -> "Starbucks"
     * - "lunch at kfc 300" -> "KFC"
     * - "cab 200 to airport" -> "Airport"
     */
    fun extractPayee(text: String, category: String? = null): String? {
        val lower = text.lowercase().trim()

        // 1. Try prepositional phrase "on/at/for/in/to/from <merchant>" stopping before split triggers/participants/amounts
        val prepRegex = Regex("\\b(?:on|at|for|in|from|to)\\s+([a-zA-Z0-9 &+.'-]+?)(?=\\s+(?:amongst|among|between|with|across|split|divide|share|and|,|\\d|$))")
        val match = prepRegex.find(lower)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            val cleanCandidate = cleanPayeeCandidate(candidate, category)
            if (!cleanCandidate.isNullOrBlank()) {
                return cleanCandidate
            }
        }

        // 2. Direct fallback prepositional search
        val fallbackPrepRegex = Regex("\\b(?:to|at|for|in|from|on)\\s+([a-zA-Z0-9 &+.'-]+)")
        val fallbackMatch = fallbackPrepRegex.find(lower)
        if (fallbackMatch != null) {
            val candidate = fallbackMatch.groupValues[1].trim()
            val cleanCandidate = cleanPayeeCandidate(candidate, category)
            if (!cleanCandidate.isNullOrBlank()) {
                return cleanCandidate
            }
        }

        // 3. Filter words from raw text that are not numbers, actions, articles, prepositions, or group words
        val words = lower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val remaining = words.filter { word ->
            word !in EXCLUDED_CATEGORY_WORDS &&
            word !in PREPOSITIONS &&
            word !in ACTION_WORDS &&
            word !in ARTICLE_WORDS &&
            word !in GROUP_DESCRIPTOR_WORDS &&
            word !in DISCARD_KEYWORDS &&
            !CATEGORIES.any { it.equals(word, ignoreCase = true) } &&
            !word.all { it.isDigit() }
        }

        if (remaining.isNotEmpty()) {
            val candidate = remaining.joinToString(" ")
            val clean = cleanPayeeCandidate(candidate, category)
            if (!clean.isNullOrBlank()) return clean
        }

        return null
    }

    private fun cleanPayeeCandidate(candidate: String, category: String?): String? {
        val tokens = candidate.split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = tokens.filter { token ->
            token !in EXCLUDED_CATEGORY_WORDS &&
            token !in PREPOSITIONS &&
            token !in ACTION_WORDS &&
            token !in ARTICLE_WORDS &&
            token !in GROUP_DESCRIPTOR_WORDS &&
            token !in DISCARD_KEYWORDS &&
            !CATEGORIES.any { it.equals(token, ignoreCase = true) } &&
            !token.all { it.isDigit() }
        }

        if (filtered.isEmpty()) return null

        val result = filtered.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.trim()

        return if (result.length >= 2) result else null
    }

    // ── Category resolution ───────────────────────────────────────────────────

    /**
     * Resolves [text] to a canonical category (e.g. "Food", "Travel", "Shopping", "Bills", "Groceries") or null.
     */
    fun resolveCategory(text: String): String? {
        val lower = text.lowercase().trim()

        // 1. AI Classifier check (covers 200+ merchants and categories)
        val aiResult = TransactionCategoryAiClassifier.classifyOutflow(lower)
        if (aiResult != null) return aiResult

        val words = lower.split(Regex("\\s+")).filter { it.isNotBlank() }

        // 2. Exact canonical match
        for (word in words) {
            val match = CATEGORIES.find { it.equals(word, ignoreCase = true) }
            if (match != null) return match
        }

        // 3. Synonym lookup
        for (word in words) {
            val syn = CATEGORY_SYNONYMS[word]
            if (syn != null) return syn
        }
        for ((synonym, category) in CATEGORY_SYNONYMS) {
            if (synonym.contains(' ') && lower.contains(synonym)) return category
        }

        // 4. Levenshtein fuzzy match (EC-28)
        for (word in words) {
            if (word in EXCLUDED_CATEGORY_WORDS || word.all { it.isDigit() }) continue

            val maxDist = if (word.length <= 4) 1 else 2
            for (category in CATEGORIES) {
                if (levenshtein(word, category.lowercase()) <= maxDist) return category
            }
        }

        return null
    }

    // ── Discard detection (EC-25) ──────────────────────────────────────────────

    private fun isDiscardIntent(text: String): Boolean {
        val words = text.split(Regex("\\s+"))
        for (word in words) {
            if (word in DISCARD_KEYWORDS) return true
        }
        for (keyword in DISCARD_KEYWORDS) {
            if (keyword.contains(' ') && text.contains(keyword)) return true
        }
        return false
    }

    // ── Split intent (FR-6) ────────────────────────────────────────────────────

    /**
     * Detects split intents (e.g. "splitting ₹450 on Swiggy amongst three people, Ojas, Niranjan and Chirag")
     * and cleanly extracts individual participant names without merchant words or group count phrases.
     */
    fun parseSplitIntent(text: String): VoiceIntent.Split? {
        val lower = text.lowercase().trim()

        val splitKeywords = listOf(
            "split with", "split between", "split amongst", "split among", "split on", "split across",
            "divide with", "divide between", "divide amongst", "divide among", "divide across",
            "share with", "share between", "share amongst", "share among",
            "amongst", "among", "between", "across", "split", "splitting", "divide", "dividing", "share", "with"
        ).sortedByDescending { it.length }

        val hasSplitIndicator = splitKeywords.any { lower.contains(it) } || lower.contains(" and ") || lower.contains(",")
        if (!hasSplitIndicator) return null

        // Find the participants segment — typically follows "amongst", "among", "between", "with", "across", or "split with"
        val participantTriggers = listOf(
            "amongst", "among", "between", "across", "with", "split with", "divide with", "share with"
        ).sortedByDescending { it.length }

        val matchedTrigger = participantTriggers.firstOrNull { lower.contains(it) }
        var rawParticipants = if (matchedTrigger != null) {
            val idx = lower.indexOf(matchedTrigger)
            text.substring(idx + matchedTrigger.length).trim()
        } else {
            val generalTrigger = listOf("splitting", "split", "divide", "share").firstOrNull { lower.contains(it) }
            if (generalTrigger != null) {
                val idx = lower.indexOf(generalTrigger)
                text.substring(idx + generalTrigger.length).trim()
            } else {
                text.trim()
            }
        }

        if (rawParticipants.isBlank()) return null

        // Remove group count descriptions (e.g. "three people", "3 people", "3 of us", "all of us", "four guys")
        val groupPattern = Regex("(?i)\\b(?:(?:one|two|three|four|five|six|seven|eight|nine|ten|\\d+)\\s+(?:people|persons|guys|friends|of us|members))|\\b(?:all of us|both of us|each of us|everyone|everybody|my friends)\\b")
        rawParticipants = groupPattern.replace(rawParticipants, " ").trim()

        // Stop words to remove if they appear as participant names
        val ignoreWords = setOf(
            "me", "myself", "us", "the", "bill", "money", "amount", "expense", "transaction",
            "with", "and", "between", "among", "amongst", "across", "for", "to", "on", "at", "split", "splitting",
            "divide", "share", "add", "people", "persons", "guys", "friends", "swiggy", "zomato", "uber", "ola", "kfc", "starbucks", "amazon"
        )

        // Split on connectors: commas, "and", "&", "+", "with", "between", "among", "amongst"
        val rawTokens = rawParticipants
            .split(Regex("\\s*,\\s*|\\s+and\\s+|\\s*&\\s*|\\s*\\+\\s*|\\s+with\\s+|\\s+between\\s+|\\s+amongst?\\s+|\\s+across\\s+"))
            .map { it.trim().trim('.', '!', '?') }
            .filter { it.isNotBlank() }

        // If a token contains multiple space-separated words (e.g. "ojas niranjan"), expand them into distinct names
        val expandedTokens = mutableListOf<String>()
        for (token in rawTokens) {
            val words = token.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size > 1) {
                for (w in words) {
                    if (w.lowercase() !in ignoreWords && !w.all { it.isDigit() }) {
                        expandedTokens.add(w)
                    }
                }
            } else {
                expandedTokens.add(token)
            }
        }

        val names = expandedTokens
            .map { candidate ->
                var c = candidate
                listOf("with ", "between ", "among ", "amongst ", "to ", "for ", "on ", "split ", "divide ").forEach { prefix ->
                    if (c.startsWith(prefix, ignoreCase = true)) {
                        c = c.substring(prefix.length).trim()
                    }
                }
                c
            }
            .filter { candidate ->
                val lowerCandidate = candidate.lowercase()
                candidate.isNotBlank() &&
                candidate.length >= 2 &&
                !ignoreWords.contains(lowerCandidate) &&
                !EXCLUDED_CATEGORY_WORDS.contains(lowerCandidate) &&
                !GROUP_DESCRIPTOR_WORDS.contains(lowerCandidate) &&
                !CATEGORIES.any { it.equals(lowerCandidate, ignoreCase = true) } &&
                !candidate.all { it.isDigit() }
            }
            .map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }

        return if (names.isNotEmpty()) VoiceIntent.Split(names) else null
    }

    // ── Levenshtein distance (EC-28) ───────────────────────────────────────────

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
