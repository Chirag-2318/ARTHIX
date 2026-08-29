package com.chirag.arthix.sensor

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Tests for [GestureStateMachine] (PRD §5, §10.1).
 *
 * Validates all FSM transitions:
 * - Short shake path: onset → magnitude drops → ShakeEvent emitted
 * - Hold path: onset → sustained motion → ShakeAndHoldEvent + ShakeCancellationSignal
 * - Safety timeout: HOLD_MAX_MS forces return to IDLE
 * - Debounce integration: rapid double-onset → exactly one event
 * - State invariants: always IDLE after gesture completes
 */
class GestureStateMachineTest {

    private val config = ShakeDetectorConfigSnapshot(
        accelThreshold = 12f,
        holdThresholdMs = 1200L,
        stabilizeMs = 150L,
        holdMaxMs = 5000L,
        debounceMs = 2000L,
    )

    private lateinit var fsm: GestureStateMachine

    private val shakeEvents = mutableListOf<ShakeEvent>()
    private val holdEvents = mutableListOf<ShakeAndHoldEvent>()
    private val cancellations = mutableListOf<ShakeCancellationSignal>()

    @Before
    fun setUp() {
        fsm = GestureStateMachine(config, DebounceGate(config.debounceMs))
        fsm.onShakeEvent = { shakeEvents.add(it) }
        fsm.onShakeAndHoldEvent = { holdEvents.add(it) }
        fsm.onCancellationSignal = { cancellations.add(it) }
        shakeEvents.clear()
        holdEvents.clear()
        cancellations.clear()
    }

    // ── Short shake path (FR-1) ────────────────────────────────────────

    @Test
    fun `onset then quick magnitude drop emits ShakeEvent only`() {
        // Enter SHAKING
        fsm.onShakeOnset(0L)
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.SHAKING)

        // Above threshold for a bit
        fsm.onSensorUpdate(50L, 15f)
        fsm.onSensorUpdate(100L, 14f)

        // Magnitude drops below threshold
        fsm.onSensorUpdate(200L, 5f)

        // Stays below for STABILIZE_MS (150ms)
        fsm.onSensorUpdate(300L, 3f)
        fsm.onSensorUpdate(360L, 2f) // 360 - 200 = 160ms > 150ms stabilize

        // Should have committed a ShakeEvent and returned to IDLE
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
        assertThat(shakeEvents).hasSize(1)
        assertThat(holdEvents).isEmpty()
        assertThat(cancellations).isEmpty()
    }

    @Test
    fun `no event emitted while still in SHAKING`() {
        // Enter SHAKING
        fsm.onShakeOnset(0L)

        // Provide above-threshold samples but not enough for hold
        fsm.onSensorUpdate(50L, 15f)
        fsm.onSensorUpdate(100L, 16f)
        fsm.onSensorUpdate(200L, 14f)

        // Still in SHAKING — nothing emitted yet
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.SHAKING)
        assertThat(shakeEvents).isEmpty()
        assertThat(holdEvents).isEmpty()
    }

    // ── Hold path (FR-7) ───────────────────────────────────────────────

    @Test
    fun `onset then sustained motion past hold threshold emits ShakeAndHoldEvent`() {
        // Enter SHAKING
        fsm.onShakeOnset(0L)

        // Keep motion above threshold past HOLD_THRESHOLD_MS (1200ms)
        for (t in 100L..1300L step 100L) {
            fsm.onSensorUpdate(t, 15f)
        }

        // Should have transitioned to HOLD_CONFIRMED
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.HOLD_CONFIRMED)

        // ShakeAndHoldEvent emitted, with ShakeCancellationSignal
        assertThat(holdEvents).hasSize(1)
        assertThat(cancellations).hasSize(1)
        assertThat(shakeEvents).isEmpty()

        // Cancellation and hold event share the same correlationId
        assertThat(cancellations[0].correlationId).isEqualTo(holdEvents[0].correlationId)
        assertThat(cancellations[0].reason).isEqualTo("reclassified_as_hold")

        // Hold duration should be approximately 1200-1300ms
        assertThat(holdEvents[0].holdDurationMs).isAtLeast(1200L)
    }

    @Test
    fun `hold confirmed returns to idle when motion drops`() {
        // Enter SHAKING → HOLD_CONFIRMED
        fsm.onShakeOnset(0L)
        for (t in 100L..1300L step 100L) {
            fsm.onSensorUpdate(t, 15f)
        }
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.HOLD_CONFIRMED)

        // Motion drops
        fsm.onSensorUpdate(1400L, 3f)
        // Stabilize
        fsm.onSensorUpdate(1560L, 2f) // 1560 - 1400 = 160ms > 150ms

        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
    }

    // ── Safety timeout (HOLD_MAX_MS) ───────────────────────────────────

    @Test
    fun `hold exceeding HOLD_MAX_MS forces return to IDLE`() {
        // Enter SHAKING → HOLD_CONFIRMED
        fsm.onShakeOnset(0L)
        for (t in 100L..1300L step 100L) {
            fsm.onSensorUpdate(t, 15f)
        }
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.HOLD_CONFIRMED)

        // The hold was confirmed at ~1200-1300ms. Keep motion going past HOLD_MAX_MS (5000ms).
        // HOLD_MAX_MS is measured from HOLD_CONFIRMED entry, so we need ~5000ms more.
        val holdConfirmedAt = holdEvents[0].timestampMonotonic
        for (t in (holdConfirmedAt + 100L)..(holdConfirmedAt + 5100L) step 100L) {
            fsm.onSensorUpdate(t, 15f)
        }

        // Safety timeout should have forced return to IDLE
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
    }

    // ── Debounce integration ───────────────────────────────────────────

    @Test
    fun `rapid double onset within debounce window emits exactly one event`() {
        // First shake gesture: onset → quick drop → ShakeEvent
        fsm.onShakeOnset(0L)
        fsm.onSensorUpdate(50L, 15f)
        fsm.onSensorUpdate(100L, 5f)
        fsm.onSensorUpdate(260L, 3f)  // 260-100 = 160ms > stabilize
        assertThat(shakeEvents).hasSize(1)
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)

        // Second shake gesture at 500ms (within 2000ms debounce window)
        fsm.onShakeOnset(500L)
        fsm.onSensorUpdate(550L, 15f)
        fsm.onSensorUpdate(600L, 5f)
        fsm.onSensorUpdate(760L, 3f) // drop + stabilize
        // This should be suppressed by debounce
        assertThat(shakeEvents).hasSize(1) // still just 1
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
    }

    @Test
    fun `shake after debounce window expiry emits second event`() {
        // First shake at t=0
        fsm.onShakeOnset(0L)
        fsm.onSensorUpdate(50L, 15f)
        fsm.onSensorUpdate(100L, 5f)
        fsm.onSensorUpdate(260L, 3f)
        assertThat(shakeEvents).hasSize(1)

        // Second shake at t=2100ms (past 2000ms debounce)
        fsm.onShakeOnset(2100L)
        fsm.onSensorUpdate(2150L, 15f)
        fsm.onSensorUpdate(2200L, 5f)
        fsm.onSensorUpdate(2360L, 3f)
        assertThat(shakeEvents).hasSize(2) // both emitted
    }

    // ── State invariants ───────────────────────────────────────────────

    @Test
    fun `state is IDLE after completed shake gesture`() {
        fsm.onShakeOnset(0L)
        fsm.onSensorUpdate(50L, 15f)
        fsm.onSensorUpdate(100L, 5f)
        fsm.onSensorUpdate(260L, 3f)

        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
    }

    @Test
    fun `state is IDLE after completed hold gesture`() {
        fsm.onShakeOnset(0L)
        for (t in 100L..1300L step 100L) {
            fsm.onSensorUpdate(t, 15f)
        }
        // Now in HOLD_CONFIRMED, motion drops
        fsm.onSensorUpdate(1400L, 3f)
        fsm.onSensorUpdate(1560L, 2f)

        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.IDLE)
    }

    @Test
    fun `onset while already in SHAKING is ignored`() {
        fsm.onShakeOnset(0L)
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.SHAKING)

        // Second onset while in SHAKING — should be ignored
        fsm.onShakeOnset(200L)
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.SHAKING)
    }

    @Test
    fun `mid-shake dip shorter than STABILIZE_MS does not end gesture`() {
        fsm.onShakeOnset(0L)
        fsm.onSensorUpdate(50L, 15f)  // above threshold

        // Brief dip below threshold (only 100ms, less than 150ms stabilize)
        fsm.onSensorUpdate(100L, 5f)
        fsm.onSensorUpdate(200L, 15f)  // back above threshold at 200ms — dip was only 100ms

        // Should still be in SHAKING, no event committed
        assertThat(fsm.currentState).isEqualTo(GestureStateMachine.State.SHAKING)
        assertThat(shakeEvents).isEmpty()
    }
}
