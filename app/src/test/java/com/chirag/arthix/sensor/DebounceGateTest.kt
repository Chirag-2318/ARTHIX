package com.chirag.arthix.sensor

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Tests for [DebounceGate] (PRD §4.2, §10.1).
 *
 * Validates:
 * - First pass always succeeds
 * - Second pass within debounce window is suppressed
 * - Pass after window expiry succeeds (fresh window starts)
 * - Suppressed pass does NOT extend the window
 */
class DebounceGateTest {

    private lateinit var gate: DebounceGate

    @Before
    fun setUp() {
        gate = DebounceGate(debounceMs = 2000L)
    }

    @Test
    fun `first pass always succeeds`() {
        assertThat(gate.tryPass(0L)).isTrue()
    }

    @Test
    fun `second pass within window is suppressed`() {
        assertThat(gate.tryPass(0L)).isTrue()          // opens window through 2000ms
        assertThat(gate.tryPass(500L)).isFalse()        // 500ms < 2000ms → suppressed
        assertThat(gate.tryPass(1000L)).isFalse()       // 1000ms < 2000ms → suppressed
        assertThat(gate.tryPass(1999L)).isFalse()       // 1999ms < 2000ms → suppressed
    }

    @Test
    fun `pass after window expiry succeeds`() {
        assertThat(gate.tryPass(0L)).isTrue()            // window: 0 → 2000
        assertThat(gate.tryPass(1500L)).isFalse()        // suppressed
        assertThat(gate.tryPass(2000L)).isTrue()          // exactly at expiry → new window: 2000 → 4000
        assertThat(gate.tryPass(2500L)).isFalse()         // within new window → suppressed
        assertThat(gate.tryPass(4100L)).isTrue()          // past new window → opens fresh
    }

    @Test
    fun `suppressed pass does not extend window`() {
        // PRD §4.2: "a second physical shake at t=1500ms is suppressed and does NOT
        // push the window out to t=3500ms; a third shake at t=2100ms emits normally"
        assertThat(gate.tryPass(0L)).isTrue()            // window: 0 → 2000
        assertThat(gate.tryPass(1500L)).isFalse()        // suppressed, window stays at 2000
        assertThat(gate.tryPass(2100L)).isTrue()          // 2100 > 2000 → passes, new window: 2100 → 4100
    }

    @Test
    fun `reset clears debounce state`() {
        assertThat(gate.tryPass(0L)).isTrue()
        assertThat(gate.tryPass(500L)).isFalse()

        gate.reset()

        // After reset, should pass immediately
        assertThat(gate.tryPass(600L)).isTrue()
    }

    @Test
    fun `rapid triple shake produces exactly one pass`() {
        // Three shakes at 0ms, 300ms, 800ms — all within a 2000ms window
        assertThat(gate.tryPass(0L)).isTrue()
        assertThat(gate.tryPass(300L)).isFalse()
        assertThat(gate.tryPass(800L)).isFalse()
    }
}
