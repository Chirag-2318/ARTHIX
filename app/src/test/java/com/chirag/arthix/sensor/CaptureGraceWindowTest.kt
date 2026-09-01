package com.chirag.arthix.sensor

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Tests for bounded capture grace window logic ([CaptureGraceWindowState]).
 *
 * Validates:
 * - 10s initial duration upon start
 * - Single extension up to 120s max limit on activity
 * - Subsequent extension attempts within same window are rejected
 * - Expiration detection after duration elapses
 * - Clean state reset upon stop
 */
class CaptureGraceWindowTest {

    private lateinit var state: CaptureGraceWindowState

    @Before
    fun setUp() {
        state = CaptureGraceWindowState(
            initialGraceMs = 10_000L,
            maxGraceMs = 120_000L
        )
    }

    @Test
    fun `initial start primes 10s grace window`() {
        val duration = state.start(nowMonotonic = 1000L, correlationId = "corr-1")

        assertThat(duration).isEqualTo(10_000L)
        assertThat(state.isActive).isTrue()
        assertThat(state.hasExtended).isFalse()
        assertThat(state.correlationId).isEqualTo("corr-1")
        assertThat(state.isExpired(nowMonotonic = 5000L)).isFalse()
        assertThat(state.isExpired(nowMonotonic = 11_000L)).isTrue()
    }

    @Test
    fun `extend within window extends up to 120s max cap`() {
        state.start(nowMonotonic = 1000L, correlationId = "corr-1")

        // At t=6000 (5s elapsed out of initial 10s), motion detected -> extend
        val remaining = state.extend(nowMonotonic = 6000L)

        assertThat(remaining).isNotNull()
        // 120_000 total - 5000 elapsed = 115_000 remaining
        assertThat(remaining).isEqualTo(115_000L)
        assertThat(state.hasExtended).isTrue()
        assertThat(state.isExpired(nowMonotonic = 15_000L)).isFalse()
        assertThat(state.isExpired(nowMonotonic = 121_000L)).isTrue()
    }

    @Test
    fun `second extend request is rejected to bound battery usage`() {
        state.start(nowMonotonic = 1000L, correlationId = "corr-1")

        val firstExtend = state.extend(nowMonotonic = 5000L)
        assertThat(firstExtend).isNotNull()

        // Second extend attempt
        val secondExtend = state.extend(nowMonotonic = 10_000L)
        assertThat(secondExtend).isNull()
    }

    @Test
    fun `stop resets active state and clears correlation`() {
        state.start(nowMonotonic = 1000L, correlationId = "corr-1")
        assertThat(state.isActive).isTrue()

        state.stop()

        assertThat(state.isActive).isFalse()
        assertThat(state.correlationId).isNull()
        assertThat(state.isExpired(nowMonotonic = 2000L)).isTrue()
    }
}
