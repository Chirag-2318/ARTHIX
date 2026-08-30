package com.chirag.arthix.report.model

/**
 * Container holding verified numbers and their allowed string representations (EC-48).
 *
 * Used by [GroundingValidator] to check every numeric token in generated LLM phrasing.
 */
data class GroundingWhitelist(
    val allowedNumbers: Set<String>,
) {
    companion object {
        /**
         * Build a whitelist from verified [ComputedReportData].
         */
        fun fromComputedData(data: ComputedReportData): GroundingWhitelist {
            val tokens = mutableSetOf<String>()

            // Standard period constants
            tokens.addAll(listOf("7", "14", "30", "1", "2", "3", "4", "5", "6", "100"))

            // Helper to add all formatting variations of a paise value
            fun addPaiseVariations(paise: Long) {
                val absPaise = kotlin.math.abs(paise)
                val rupees = absPaise / 100
                tokens.add(absPaise.toString())
                tokens.add(rupees.toString())
                // Formatted with commas: "4,500"
                tokens.add("%,d".format(rupees))
                // Decimal: "4500.00"
                tokens.add("%.2f".format(rupees.toDouble()))
            }

            addPaiseVariations(data.netFlowPaise)
            addPaiseVariations(data.totalOutflowPaise)
            addPaiseVariations(data.totalInflowPaise)
            addPaiseVariations(data.uncategorizedTotalPaise)
            addPaiseVariations(data.projectedTotalPaise)
            addPaiseVariations(data.projectedSavingsPaise)

            data.categoryBreakdown.values.forEach { addPaiseVariations(it) }

            data.suggestion?.let {
                addPaiseVariations(it.currentSpendPaise)
                addPaiseVariations(it.baselineSpendPaise)
                addPaiseVariations(it.projectedSavingsPaise)
                tokens.add(it.percentageAboveBaseline.toString())
                tokens.add("${it.percentageAboveBaseline}%")
                tokens.add(it.targetReductionPercentage.toString())
                tokens.add("${it.targetReductionPercentage}%")
            }

            return GroundingWhitelist(tokens)
        }
    }
}
