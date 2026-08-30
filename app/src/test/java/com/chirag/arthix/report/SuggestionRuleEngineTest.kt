package com.chirag.arthix.report

import com.chirag.arthix.report.engine.SuggestionRuleEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SuggestionRuleEngineTest {

    private lateinit var engine: SuggestionRuleEngine

    @Before
    fun setup() {
        engine = SuggestionRuleEngine()
    }

    @Test
    fun generateSuggestion_categoryAboveBaseline_picksHighestGrowthCategory() {
        val current = mapOf(
            "food" to 400_000L,      // +₹1,000 growth (+33%)
            "travel" to 200_000L,    // +₹1,500 growth (+300%)
            "shopping" to 100_000L,  // 0 growth
        )
        val previous = mapOf(
            "food" to 300_000L,
            "travel" to 50_000L,
            "shopping" to 100_000L,
        )

        val suggestion = engine.generateSuggestion(current, previous)

        assertThat(suggestion).isNotNull()
        assertThat(suggestion?.category).isEqualTo("travel")
        assertThat(suggestion?.currentSpendPaise).isEqualTo(200_000L)
        assertThat(suggestion?.targetReductionPercentage).isEqualTo(20)
        assertThat(suggestion?.projectedSavingsPaise).isEqualTo(40_000L) // 20% of ₹2,000 = ₹400
    }

    @Test
    fun generateSuggestion_zeroBaseline_picksHighestSpendCategory() {
        val current = mapOf(
            "food" to 250_000L,
            "travel" to 80_000L,
        )
        val previous = emptyMap<String, Long>()

        val suggestion = engine.generateSuggestion(current, previous)

        assertThat(suggestion).isNotNull()
        assertThat(suggestion?.category).isEqualTo("food")
        assertThat(suggestion?.currentSpendPaise).isEqualTo(250_000L)
        assertThat(suggestion?.projectedSavingsPaise).isEqualTo(50_000L) // 20% of ₹2,500 = ₹500
    }

    @Test
    fun generateSuggestion_excludesUncategorizedCategory() {
        // EC-44: Uncategorized spend must not be selected as a suggestion target
        val current = mapOf(
            "uncategorized" to 500_000L,
            "food" to 100_000L,
        )
        val previous = emptyMap<String, Long>()

        val suggestion = engine.generateSuggestion(current, previous)

        assertThat(suggestion).isNotNull()
        assertThat(suggestion?.category).isEqualTo("food")
    }
}
