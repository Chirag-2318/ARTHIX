package com.chirag.arthix.ocr

import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.Locale

/**
 * Extracts a purchase/transaction date from raw OCR text on receipts.
 *
 * Implements the specification from Docs/ARTHIX_OCR_Date_Extraction_Design.md:
 * - Pure Kotlin, zero Android framework dependencies (JVM testable).
 * - Handles OCR character distortions (O->0, I->1, S->5, B->8).
 * - Prioritizes purchase/billing anchors over unlabeled dates.
 * - Strictly excludes due, expiry, and validity dates.
 * - Defaults ambiguous dates to Indian DD/MM/YYYY order with low-confidence flagging.
 * - Infers 2-digit year century preventing future dates.
 * - Guards against future dates.
 * - Returns [OcrDateResult.NotFound] if no date is found (never silently falls back to today).
 */
object OcrDateExtractor {

    private val POSITIVE_KEYWORDS = listOf(
        "bill date",
        "invoice date",
        "txn date",
        "transaction date",
        "trans date",
        "order date",
        "pos date",
        "billing date",
        "receipt date",
        "dated",
        "date:",
        "date :",
        "date",
    )

    private val NEGATIVE_KEYWORDS = listOf(
        "due date",
        "due on",
        "due by",
        "due :",
        "due:",
        "payment due",
        "balance due",
        "expiry date",
        "expiry",
        "exp date",
        "exp:",
        "exp :",
        "valid until",
        "valid up to",
        "valid till",
        "best before",
        "warranty until",
        "warranty up to",
        "delivery date",
        "delivery on",
        "ship date",
    )

    private val MONTH_MAP = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12,
    )

    // Regex 1: Named month (e.g. 14 Aug 2026, 14-Aug-2026, 14 August 2026, Aug 14 2026, 14 Aug 26)
    private val NAMED_MONTH_PATTERN_1 = Regex(
        """\b([0-3]?[0-9])[\s/.-]+([A-Za-z]{3,9})[\s/.,-]+([0-9]{2,4})\b"""
    )
    private val NAMED_MONTH_PATTERN_2 = Regex(
        """\b([A-Za-z]{3,9})[\s/.-]+([0-3]?[0-9])(?:st|nd|rd|th)?[\s/.,-]+([0-9]{2,4})\b"""
    )

    // Regex 2: ISO (YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD)
    private val ISO_PATTERN = Regex(
        """\b([12][0-9]{3})[\s/.-]+([0-1]?[0-9])[\s/.-]+([0-3]?[0-9])\b"""
    )

    // Regex 3: Potential numeric dates (e.g. 14/08/2026, 14-08-26, 14.08.2026)
    // Characters can include OCR distortions like O, I, l, S, B
    private val NUMERIC_CANDIDATE_PATTERN = Regex(
        """\b([0-9OIlSB]{1,2})[\s/.-]+([0-9OIlSB]{1,2})[\s/.-]+([0-9OIlSB]{2,4})\b"""
    )

    data class DateCandidate(
        val localDate: LocalDate,
        val lineIndex: Int,
        val isKeywordMatch: Boolean,
        val isAmbiguous: Boolean,
        val rawSnippet: String,
    )

    /**
     * Extracts the most plausible transaction date from [ocrText].
     *
     * @param ocrText The raw OCR output.
     * @param referenceDate The reference date used for century inference and future guards (defaults to current date).
     * @param zoneId Timezone used for epoch timestamp calculation.
     */
    fun extract(
        ocrText: String,
        referenceDate: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): OcrDateResult {
        if (ocrText.isBlank()) return OcrDateResult.NotFound

        val rawLines = ocrText.lines()
        val candidates = mutableListOf<DateCandidate>()
        var futureCandidate: OcrDateResult.FutureDate? = null

        for (lineIdx in rawLines.indices) {
            val lineOriginal = rawLines[lineIdx].trim()
            if (lineOriginal.isEmpty()) continue

            val lineLower = lineOriginal.lowercase(Locale.ROOT)

            // 1. Check negative keyword lines (Due, Expiry, Valid Until) -> strictly discard (design doc §2.3)
            if (isNegativeLine(lineLower)) {
                continue
            }

            val hasPositiveKeyword = isPositiveLine(lineLower)

            // Extract candidates from this line
            val lineCandidates = findDatesInLine(lineOriginal, lineIdx, hasPositiveKeyword, referenceDate)
            for (candidate in lineCandidates) {
                if (candidate.localDate.isAfter(referenceDate)) {
                    if (futureCandidate == null) {
                        futureCandidate = OcrDateResult.FutureDate(
                            rawSnippet = candidate.rawSnippet,
                            parsedDate = candidate.localDate,
                        )
                    }
                } else {
                    candidates.add(candidate)
                }
            }
        }

        if (candidates.isEmpty()) {
            return futureCandidate ?: OcrDateResult.NotFound
        }

        // Resolution order per design doc §2.3:
        // 1. Prefer dates explicitly labeled with a positive purchase keyword.
        // 2. If multiple unlabeled candidates exist, prefer the one near the top of the receipt (lowest lineIndex).
        val bestCandidate = candidates.minWithOrNull(
            compareBy<DateCandidate> { !it.isKeywordMatch } // true (false < true) so keyword matches come first
                .thenBy { it.lineIndex }
        ) ?: return futureCandidate ?: OcrDateResult.NotFound

        val epochMillis = bestCandidate.localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        return OcrDateResult.Found(
            epochMillis = epochMillis,
            localDate = bestCandidate.localDate,
            isKeywordMatch = bestCandidate.isKeywordMatch,
            isAmbiguous = bestCandidate.isAmbiguous,
            rawSnippet = bestCandidate.rawSnippet,
        )
    }

    private fun findDatesInLine(
        line: String,
        lineIndex: Int,
        hasPositiveKeyword: Boolean,
        referenceDate: LocalDate,
    ): List<DateCandidate> {
        val results = mutableListOf<DateCandidate>()

        // 1. Check Named Month: "14 Aug 2026" or "14-Aug-2026"
        for (match in NAMED_MONTH_PATTERN_1.findAll(line)) {
            val dayStr = match.groupValues[1]
            val monthStr = match.groupValues[2].lowercase(Locale.ROOT)
            val yearStr = match.groupValues[3]

            val monthNum = MONTH_MAP[monthStr] ?: continue
            val day = dayStr.toIntOrNull() ?: continue
            val year = parseYear(yearStr, referenceDate.year) ?: continue

            val parsed = safeLocalDate(year, monthNum, day)
            if (parsed != null) {
                results.add(
                    DateCandidate(
                        localDate = parsed,
                        lineIndex = lineIndex,
                        isKeywordMatch = hasPositiveKeyword,
                        isAmbiguous = false,
                        rawSnippet = match.value,
                    )
                )
            }
        }

        // 2. Check Named Month: "Aug 14, 2026"
        for (match in NAMED_MONTH_PATTERN_2.findAll(line)) {
            val monthStr = match.groupValues[1].lowercase(Locale.ROOT)
            val dayStr = match.groupValues[2]
            val yearStr = match.groupValues[3]

            val monthNum = MONTH_MAP[monthStr] ?: continue
            val day = dayStr.toIntOrNull() ?: continue
            val year = parseYear(yearStr, referenceDate.year) ?: continue

            val parsed = safeLocalDate(year, monthNum, day)
            if (parsed != null) {
                results.add(
                    DateCandidate(
                        localDate = parsed,
                        lineIndex = lineIndex,
                        isKeywordMatch = hasPositiveKeyword,
                        isAmbiguous = false,
                        rawSnippet = match.value,
                    )
                )
            }
        }

        // 3. Check ISO: "2026-08-14"
        for (match in ISO_PATTERN.findAll(line)) {
            val year = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toIntOrNull() ?: continue
            val day = match.groupValues[3].toIntOrNull() ?: continue

            val parsed = safeLocalDate(year, month, day)
            if (parsed != null) {
                results.add(
                    DateCandidate(
                        localDate = parsed,
                        lineIndex = lineIndex,
                        isKeywordMatch = hasPositiveKeyword,
                        isAmbiguous = false,
                        rawSnippet = match.value,
                    )
                )
            }
        }

        // 4. Check Numeric Formats with character normalization (DD/MM/YYYY, DD/MM/YY, etc.)
        for (match in NUMERIC_CANDIDATE_PATTERN.findAll(line)) {
            val p1Clean = normalizeOcrDigits(match.groupValues[1])
            val p2Clean = normalizeOcrDigits(match.groupValues[2])
            val p3Clean = normalizeOcrDigits(match.groupValues[3])

            val num1 = p1Clean.toIntOrNull() ?: continue
            val num2 = p2Clean.toIntOrNull() ?: continue
            val year = parseYear(p3Clean, referenceDate.year) ?: continue

            // Determine day and month
            // Case A: First is > 12 and second <= 12 -> Unambiguously DD/MM
            // Case B: Second is > 12 and first <= 12 -> Unambiguously MM/DD
            // Case C: Both <= 12 (e.g. 03/04/2026) -> Default to Indian DD/MM, mark ambiguous (design doc §2.4)
            val (day, month, isAmbiguous) = when {
                num1 > 12 && num2 in 1..12 -> Triple(num1, num2, false)
                num2 > 12 && num1 in 1..12 -> Triple(num2, num1, false)
                num1 in 1..31 && num2 in 1..12 -> Triple(num1, num2, true)
                else -> continue
            }

            val parsed = safeLocalDate(year, month, day)
            if (parsed != null) {
                // Check if already captured by ISO or named month matching the same range
                val isDuplicate = results.any { it.localDate == parsed }
                if (!isDuplicate) {
                    results.add(
                        DateCandidate(
                            localDate = parsed,
                            lineIndex = lineIndex,
                            isKeywordMatch = hasPositiveKeyword,
                            isAmbiguous = isAmbiguous,
                            rawSnippet = match.value,
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Normalizes OCR digit confusions in a numeric date token:
     * O/o -> 0, I/l/| -> 1, S/s -> 5, B -> 8
     */
    fun normalizeOcrDigits(token: String): String {
        return token
            .replace('O', '0')
            .replace('o', '0')
            .replace('I', '1')
            .replace('l', '1')
            .replace('|', '1')
            .replace('S', '5')
            .replace('s', '5')
            .replace('B', '8')
    }

    /**
     * Parses a 2-digit or 4-digit year string.
     * For 2-digit years, infers century based on [currentYear] (design doc §2.5).
     * If 2000 + yy exceeds currentYear, rolls back to 1900 + yy so it never resolves to a future date.
     */
    fun parseYear(rawYear: String, currentYear: Int): Int? {
        val cleaned = normalizeOcrDigits(rawYear)
        val value = cleaned.toIntOrNull() ?: return null

        return when {
            cleaned.length == 4 && value in 1900..2100 -> value
            cleaned.length == 2 && value in 0..99 -> {
                val candidate2000 = 2000 + value
                if (candidate2000 > currentYear) {
                    1900 + value
                } else {
                    candidate2000
                }
            }
            else -> null
        }
    }

    private fun safeLocalDate(year: Int, month: Int, day: Int): LocalDate? {
        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun isPositiveLine(lineLower: String): Boolean {
        return POSITIVE_KEYWORDS.any { keyword ->
            lineLower.contains(keyword)
        }
    }

    private fun isNegativeLine(lineLower: String): Boolean {
        return NEGATIVE_KEYWORDS.any { keyword ->
            lineLower.contains(keyword)
        }
    }
}
