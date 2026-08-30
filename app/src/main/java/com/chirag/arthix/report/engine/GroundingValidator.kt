package com.chirag.arthix.report.engine

import com.chirag.arthix.report.model.GroundingWhitelist
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Failed(val invalidTokens: List<String>) : ValidationResult
}

/**
 * Post-generation validation safeguard (PRD §4.b, EC-48).
 *
 * Scans every numeric token appearing in LLM-generated phrasing text and confirms
 * each one exists in the verified [GroundingWhitelist]. If an unwhitelisted number
 * appears (hallucination detected), validation fails immediately and forces
 * fallback to deterministic template phrasing.
 */
@Singleton
class GroundingValidator @Inject constructor() {

    private val numberRegex = Regex("""[0-9]+([,][0-9]+)*(\.[0-9]+)?%?""")

    /**
     * Validate [text] against [whitelist].
     *
     * @return [ValidationResult.Valid] if all numbers match whitelist,
     *         [ValidationResult.Failed] with rejected tokens if any mismatch occurs.
     */
    fun validate(text: String, whitelist: GroundingWhitelist): ValidationResult {
        if (text.isBlank()) return ValidationResult.Valid

        val matches = numberRegex.findAll(text).map { it.value.trim() }.toList()
        val invalid = mutableListOf<String>()

        for (match in matches) {
            val cleanNumber = match.replace(",", "").replace("%", "")
            val withoutDecimal = if (cleanNumber.contains(".")) {
                cleanNumber.substringBefore(".")
            } else {
                cleanNumber
            }

            val isValid = match in whitelist.allowedNumbers ||
                    cleanNumber in whitelist.allowedNumbers ||
                    withoutDecimal in whitelist.allowedNumbers

            if (!isValid) {
                invalid.add(match)
            }
        }

        return if (invalid.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Failed(invalid)
        }
    }
}
