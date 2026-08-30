package com.chirag.arthix.domain.split

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SplitMathTest {

    @Test
    fun testEvenSplit_Divisible() {
        val total = 90L
        val participants = listOf("p0", "p1", "p2")
        val result = computeSplitShares(total, participants, SplitMode.Even)

        assertEquals(3, result.size)
        assertEquals(30L, result[0].sharePaise)
        assertEquals(30L, result[1].sharePaise)
        assertEquals(30L, result[2].sharePaise)
        assertEquals(total, result.sumOf { it.sharePaise })
    }

    @Test
    fun testEvenSplit_Indivisible() {
        // Worked example from PRD 3.4
        val total = 10000L // 100.00 INR
        val participants = listOf("p0", "p1", "p2")
        val result = computeSplitShares(total, participants, SplitMode.Even)

        assertEquals(3, result.size)
        // Remainder 1 paisa absorbed by index 0
        assertEquals(3334L, result[0].sharePaise)
        assertEquals(3333L, result[1].sharePaise)
        assertEquals(3333L, result[2].sharePaise)
        assertEquals(total, result.sumOf { it.sharePaise })
    }

    @Test
    fun testEvenSplit_MultipleParticipantCounts() {
        val total = 100L
        val counts = listOf(2, 3, 4, 5, 7)
        for (count in counts) {
            val participants = (0 until count).map { "p$it" }
            val result = computeSplitShares(total, participants, SplitMode.Even)
            assertEquals(count, result.size)
            assertEquals(total, result.sumOf { it.sharePaise })
        }
    }

    @Test
    fun testCustomSplit_ExactSum() {
        val total = 100L
        val participants = listOf("p0", "p1")
        val overrides = mapOf("p0" to 60L, "p1" to 40L)
        val result = computeSplitShares(total, participants, SplitMode.Custom(overrides))

        assertEquals(60L, result.find { it.participantId == "p0" }?.sharePaise)
        assertEquals(40L, result.find { it.participantId == "p1" }?.sharePaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCustomSplit_MismatchSumThrows() {
        val total = 100L
        val participants = listOf("p0", "p1")
        val overrides = mapOf("p0" to 50L, "p1" to 40L) // Sums to 90
        
        computeSplitShares(total, participants, SplitMode.Custom(overrides))
    }

    @Test
    fun testRecalculateProportional_Increase() {
        val oldShares = listOf(
            ParticipantShare("p0", 60L),
            ParticipantShare("p1", 40L)
        )
        val oldTotal = 100L
        val newTotal = 200L

        val result = recalculateProportional(oldShares, oldTotal, newTotal)
        assertEquals(120L, result[0].sharePaise)
        assertEquals(80L, result[1].sharePaise)
        assertEquals(newTotal, result.sumOf { it.sharePaise })
    }

    @Test
    fun testRecalculateProportional_DecreaseWithRounding() {
        val oldShares = listOf(
            ParticipantShare("p0", 60L),
            ParticipantShare("p1", 40L)
        )
        val oldTotal = 100L
        // 133 * 0.6 = 79.8 -> 80
        // 133 * 0.4 = 53.2 -> 53
        // sum = 133.
        val newTotal = 133L

        val result = recalculateProportional(oldShares, oldTotal, newTotal)
        assertEquals(newTotal, result.sumOf { it.sharePaise })
    }

    @Test
    fun testRecalculateProportional_SingleParticipant() {
        val oldShares = listOf(
            ParticipantShare("p0", 100L)
        )
        val oldTotal = 100L
        val newTotal = 50L

        val result = recalculateProportional(oldShares, oldTotal, newTotal)
        assertEquals(50L, result[0].sharePaise)
        assertEquals(newTotal, result.sumOf { it.sharePaise })
    }
}
