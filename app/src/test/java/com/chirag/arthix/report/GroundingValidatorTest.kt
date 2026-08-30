package com.chirag.arthix.report

import com.chirag.arthix.report.engine.GroundingValidator
import com.chirag.arthix.report.engine.ValidationResult
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.ComputedSuggestion
import com.chirag.arthix.report.model.GroundingWhitelist
import com.chirag.arthix.report.model.ReportPeriod
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class GroundingValidatorTest {

    private lateinit var validator: GroundingValidator
    private lateinit var whitelist: GroundingWhitelist

    @Before
    fun setup() {
        validator = GroundingValidator()

        val sampleData = ComputedReportData(
            period = ReportPeriod.currentWeek(),
            categoryBreakdown = mapOf("food" to 350_000L, "travel" to 120_000L),
            totalInflowPaise = 100_000L,
            totalOutflowPaise = 470_000L,
            netFlowPaise = -370_000L,
            uncategorizedTotalPaise = 50_000L,
            projectedTotalPaise = 520_000L,
            projectedSavingsPaise = 70_000L,
            noPriorData = false,
            suggestion = ComputedSuggestion(
                category = "food",
                currentSpendPaise = 350_000L,
                baselineSpendPaise = 250_000L,
                percentageAboveBaseline = 40,
                targetReductionPercentage = 20,
                projectedSavingsPaise = 70_000L,
            )
        )

        whitelist = GroundingWhitelist.fromComputedData(sampleData)
    }

    @Test
    fun validate_matchingNumbers_passesValidation() {
        val validText = "You spent ₹4,700 this week across 2 categories. Food spend was ₹3,500, which is 40% above baseline. Cutting back by 20% would save ₹700 over 7 days."
        val result = validator.validate(validText, whitelist)

        assertThat(result).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun validate_hallucinatedAmount_failsValidation() {
        // EC-48 Safeguard Test:
        // LLM hallucinates ₹9,999 or ₹8,500 (numbers not in the verified whitelist)
        val hallucinatedText = "You spent ₹9,999 this week and can save ₹8,500."
        val result = validator.validate(hallucinatedText, whitelist)

        assertThat(result).isInstanceOf(ValidationResult.Failed::class.java)
        val failed = result as ValidationResult.Failed
        assertThat(failed.invalidTokens).contains("9,999")
        assertThat(failed.invalidTokens).contains("8,500")
    }

    @Test
    fun validate_hallucinatedPercentage_failsValidation() {
        // LLM hallucinates 85% instead of verified 40%
        val hallucinatedPct = "Food spend was 85% above baseline."
        val result = validator.validate(hallucinatedPct, whitelist)

        assertThat(result).isInstanceOf(ValidationResult.Failed::class.java)
        val failed = result as ValidationResult.Failed
        assertThat(failed.invalidTokens).contains("85%")
    }
}
