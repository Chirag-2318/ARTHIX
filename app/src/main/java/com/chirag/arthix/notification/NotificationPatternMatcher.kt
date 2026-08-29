package com.chirag.arthix.notification

import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.notification.model.ExtractedText
import com.chirag.arthix.notification.model.MatchResult
import com.chirag.arthix.notification.model.NotificationOutcome
import com.chirag.arthix.notification.model.ParsedInflow
import com.chirag.arthix.notification.model.ParsedOutflow
import com.chirag.arthix.notification.model.PatternConfig
import com.chirag.arthix.notification.model.PatternEntry
import com.chirag.arthix.notification.model.TextSource
import com.chirag.arthix.util.AmountParseResult
import com.chirag.arthix.util.AmountParser
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Matches notification text against per-app regex patterns, extracts
 * (amount, payee) tuples, and classifies direction/outcome.
 *
 * Pure Kotlin — no Android framework dependency. The [PatternConfig] is
 * loaded from a JSON string (passed in, not read from Context), making
 * this fully JVM-testable with synthetic input.
 *
 * Pipeline: raw text → outcome filter → per-app regex match → amount parse → result.
 */
object NotificationPatternMatcher {

    // ── JSON deserialization model ─────────────────────────────────────

    /** Mirrors the JSON structure of `assets/upi_patterns.json`. */
    private data class PatternConfigJson(
        @SerializedName("outflow_patterns") val outflowPatterns: List<PatternEntryJson>,
        @SerializedName("inflow_patterns") val inflowPatterns: List<PatternEntryJson>,
        @SerializedName("outcome_reject_keywords") val outcomeRejectKeywords: List<String>,
    )

    private data class PatternEntryJson(
        val app: String,
        val regex: String,
    )

    /**
     * Parse JSON config string into [PatternConfig].
     *
     * @param json contents of `assets/upi_patterns.json`.
     * @return parsed config, or null if JSON is malformed.
     */
    fun parseConfig(json: String): PatternConfig? {
        return try {
            val raw = Gson().fromJson(json, PatternConfigJson::class.java) ?: return null
            PatternConfig(
                outflowPatterns = raw.outflowPatterns.map { PatternEntry(it.app, it.regex) },
                inflowPatterns = raw.inflowPatterns.map { PatternEntry(it.app, it.regex) },
                outcomeRejectKeywords = raw.outcomeRejectKeywords,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Match a notification's text against the pattern config.
     *
     * @param extractedText the text extracted from the notification.
     * @param packageName the source app's package name.
     * @param config the loaded pattern config.
     * @return a [MatchResult] describing the parsed outcome.
     */
    fun match(
        extractedText: ExtractedText,
        packageName: String,
        config: PatternConfig,
    ): MatchResult {
        val text = extractedText.raw
        if (text.isBlank()) return MatchResult.NoMatch

        // Step 1: outcome classification BEFORE treating as a completed transaction (PRD §5.2)
        val outcome = OutcomeClassifier.classify(text, config.outcomeRejectKeywords)
        when (outcome) {
            NotificationOutcome.REJECTED -> return MatchResult.Rejected
            NotificationOutcome.REFUND -> {
                // Try to parse amount + payee from the text for refund netting
                val refundResult = tryParseAmountAndPayee(text, packageName, config.outflowPatterns)
                    ?: tryParseAmountAndPayee(text, packageName, config.inflowPatterns)
                return if (refundResult != null) {
                    MatchResult.RefundMatch(refundResult.first, refundResult.second)
                } else {
                    // Refund text that doesn't match any pattern — still flag as refund,
                    // caller can handle the "unparseable refund" case
                    MatchResult.Rejected
                }
            }
            NotificationOutcome.COMPLETED -> { /* continue to pattern matching */ }
        }

        // Step 2: try outflow patterns (scoped to this package)
        val outflowMatch = tryParseAmountAndPayee(text, packageName, config.outflowPatterns)
        if (outflowMatch != null) {
            val confidenceFlag = if (extractedText.source == TextSource.TEXT_FALLBACK) {
                ConfidenceFlag.NEEDS_REVIEW  // PRD §4.2: fallback source → lower confidence
            } else {
                ConfidenceFlag.CLEAN
            }
            return MatchResult.OutflowMatch(
                ParsedOutflow(
                    amountPaise = outflowMatch.first,
                    payee = outflowMatch.second,
                    packageName = packageName,
                    rawText = text,
                    confidenceFlag = confidenceFlag,
                )
            )
        }

        // Step 3: try inflow patterns
        val inflowMatch = tryParseAmountAndPayee(text, packageName, config.inflowPatterns)
        if (inflowMatch != null) {
            return MatchResult.InflowMatch(
                ParsedInflow(
                    amountPaise = inflowMatch.first,
                    payee = inflowMatch.second,
                    packageName = packageName,
                )
            )
        }

        return MatchResult.NoMatch
    }

    // ── Internal helpers ───────────────────────────────────────────────

    /**
     * Try to extract (amountPaise, payee) from text using the patterns
     * scoped to the given packageName.
     *
     * @return (amountPaise, payee) or null if no pattern matches.
     */
    private fun tryParseAmountAndPayee(
        text: String,
        packageName: String,
        patterns: List<PatternEntry>,
    ): Pair<Long, String>? {
        val appPatterns = patterns.filter { it.app == packageName }

        for (entry in appPatterns) {
            val regex = try {
                Regex(entry.regex, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                continue // skip malformed regex entries
            }

            val matchResult = regex.find(text) ?: continue

            // Group 1 = amount string, Group 2 = payee string
            val amountRaw = matchResult.groupValues.getOrNull(1) ?: continue
            val payeeRaw = matchResult.groupValues.getOrNull(2) ?: continue

            val parseResult = AmountParser.parse(amountRaw)
            if (parseResult is AmountParseResult.Success) {
                return parseResult.amountPaise to payeeRaw.trim()
            }
        }

        return null
    }
}
