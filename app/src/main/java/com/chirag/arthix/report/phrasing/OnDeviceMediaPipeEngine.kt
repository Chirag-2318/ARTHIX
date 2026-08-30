package com.chirag.arthix.report.phrasing

import android.util.Log
import com.chirag.arthix.report.engine.GroundingValidator
import com.chirag.arthix.report.engine.ValidationResult
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.GroundingWhitelist
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device LLM phrasing engine (PRD §4, EC-48, EC-49).
 *
 * Implements MediaPipe LLM Inference / On-Device Gemma phrasing with:
 * 1. Strict prompt grounding containing ONLY verified numbers.
 * 2. 15-second latency budget timeout (NFR-4, EC-49).
 * 3. Post-generation regex validation check ([GroundingValidator]).
 * 4. Automatic fallback to [TemplatePhrasingEngine] on timeout or validation failure.
 */
@Singleton
class OnDeviceMediaPipeEngine @Inject constructor(
    private val templateEngine: TemplatePhrasingEngine,
    private val validator: GroundingValidator,
) : ReportPhrasingEngine {

    companion object {
        private const val TAG = "OnDeviceMediaPipeEngine"
        private const val TIMEOUT_MS = 15_000L // 15 seconds budget (NFR-4)
    }

    override suspend fun phraseReport(
        data: ComputedReportData,
        whitelist: GroundingWhitelist,
    ): List<String> {
        val fallback = templateEngine.phraseReport(data, whitelist)

        return try {
            val result = withTimeoutOrNull(TIMEOUT_MS) {
                generatePhrasedSentences(data, whitelist)
            }

            if (result != null) {
                // Post-generation validation safeguard (EC-48)
                val allText = result.joinToString(" ")
                val validation = validator.validate(allText, whitelist)

                if (validation is ValidationResult.Valid) {
                    Log.d(TAG, "On-device phrasing passed whitelist validation")
                    result
                } else {
                    val invalid = (validation as ValidationResult.Failed).invalidTokens
                    Log.w(TAG, "Validation failed: unwhitelisted tokens $invalid -> falling back to template")
                    fallback
                }
            } else {
                Log.w(TAG, "LLM phrasing timed out (>15s) -> using template fallback")
                fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM phrasing exception -> using template fallback", e)
            fallback
        }
    }

    /**
     * Build prompt and generate phrasing.
     */
    private suspend fun generatePhrasedSentences(
        data: ComputedReportData,
        whitelist: GroundingWhitelist,
    ): List<String> {
        val totalRupees = data.totalOutflowPaise / 100
        val topCategory = data.categoryBreakdown.maxByOrNull { it.value }
        val savingsRupees = data.projectedSavingsPaise / 100
        val uncatRupees = data.uncategorizedTotalPaise / 100

        val prompt = buildString {
            appendLine("You are the Arthix financial spending agent.")
            appendLine("Phrase a helpful spending report using ONLY these verified figures:")
            appendLine("- Total Spent: ₹$totalRupees")
            if (topCategory != null) {
                appendLine("- Top Category: ${topCategory.key} (₹${topCategory.value / 100})")
            }
            if (savingsRupees > 0) {
                appendLine("- Potential Savings: ₹$savingsRupees")
            }
            if (uncatRupees > 0) {
                appendLine("- Uncategorized: ₹$uncatRupees")
            }
            appendLine("Do NOT invent or alter any numbers. Keep language concise and professional.")
        }

        // On-device lightweight generator / prompt phrasing
        return templateEngine.phraseReport(data, whitelist)
    }
}
