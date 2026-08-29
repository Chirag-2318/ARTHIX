package com.chirag.arthix.sensor

import java.util.UUID

/**
 * IDLE → SHAKING → HOLD_CONFIRMED gesture state machine.
 *
 * The most technically important part of Phase 1 (techstack §1, EC-03).
 * Classifies whether a detected shake onset is a **short shake** (FR-1 path)
 * or a **shake-and-hold** (FR-7 report trigger path).
 *
 * **Ground rule (PRD §5.3):** no event is dispatched while in [State.SHAKING].
 * Dispatch happens exactly once, at the moment the state machine resolves to
 * one branch or the other.
 *
 * Pure logic — no Android dependencies. All timestamps are expected to be
 * monotonic (ms). Thread safety: NOT thread-safe; caller serializes invocation.
 *
 * PRD §5.1–5.5.
 */
class GestureStateMachine(
    private val config: ShakeDetectorConfigSnapshot = ShakeDetectorConfigSnapshot(),
    private val debounceGate: DebounceGate = DebounceGate(config.debounceMs),
) {

    // ── Public state ───────────────────────────────────────────────────

    enum class State { IDLE, SHAKING, HOLD_CONFIRMED }

    var currentState: State = State.IDLE
        private set

    // ── Callbacks (wired to Flow emitters in ShakeSensorManager) ───────

    var onShakeEvent: ((ShakeEvent) -> Unit)? = null
    var onShakeAndHoldEvent: ((ShakeAndHoldEvent) -> Unit)? = null
    var onCancellationSignal: ((ShakeCancellationSignal) -> Unit)? = null

    // ── Internal state ─────────────────────────────────────────────────

    /** Monotonic timestamp (ms) when SHAKING state was entered. */
    private var shakingEnteredAt: Long = 0L

    /**
     * Correlation ID for the current gesture cycle.
     * Generated when SHAKING is entered; used for the emitted event.
     */
    private var currentCorrelationId: String? = null

    /**
     * Monotonic timestamp (ms) when magnitude last dropped below threshold.
     * Used for stabilization detection (PRD §5.4 STABILIZE_MS).
     */
    private var belowThresholdSince: Long? = null

    /**
     * Monotonic timestamp (ms) when HOLD_CONFIRMED was entered.
     * Used for HOLD_MAX_MS safety timeout.
     */
    private var holdConfirmedAt: Long = 0L

    // ── Input methods ──────────────────────────────────────────────────

    /**
     * Called when the [OscillationDetector] fires a SHAKE_ONSET signal.
     *
     * Transitions: IDLE → SHAKING (starts hold timer).
     * If already in SHAKING or HOLD_CONFIRMED, the onset is ignored
     * (a gesture is already in progress).
     *
     * @param timestampMs monotonic time (ms) of the onset.
     */
    fun onShakeOnset(timestampMs: Long) {
        if (currentState != State.IDLE) return

        currentState = State.SHAKING
        shakingEnteredAt = timestampMs
        currentCorrelationId = UUID.randomUUID().toString()
        belowThresholdSince = null
    }

    /**
     * Called on every sensor sample while in [State.SHAKING] or [State.HOLD_CONFIRMED]
     * to track whether motion continues or has stopped.
     *
     * @param timestampMs monotonic time (ms) of the sample.
     * @param magnitude linear-acceleration magnitude (m/s²).
     */
    fun onSensorUpdate(timestampMs: Long, magnitude: Float) {
        when (currentState) {
            State.IDLE -> return // nothing to track
            State.SHAKING -> handleShakingUpdate(timestampMs, magnitude)
            State.HOLD_CONFIRMED -> handleHoldConfirmedUpdate(timestampMs, magnitude)
        }
    }

    // ── SHAKING state logic ────────────────────────────────────────────

    private fun handleShakingUpdate(timestampMs: Long, magnitude: Float) {
        val isAboveThreshold = magnitude >= config.accelThreshold

        if (isAboveThreshold) {
            // Motion continues — reset stabilization tracker
            belowThresholdSince = null

            // Check if we've crossed the hold threshold
            val shakingDuration = timestampMs - shakingEnteredAt
            if (shakingDuration >= config.holdThresholdMs) {
                transitionToHoldConfirmed(timestampMs)
            }
        } else {
            // Magnitude dropped below threshold
            if (belowThresholdSince == null) {
                belowThresholdSince = timestampMs
            }

            val belowDuration = timestampMs - (belowThresholdSince ?: timestampMs)

            if (belowDuration >= config.stabilizeMs) {
                // Motion has stopped (stabilized below threshold) before hold threshold
                // → this is a short shake (FR-1 path)
                commitShakeEvent(timestampMs)
            } else {
                // Check if the hold threshold is reached even during a below-threshold dip
                val shakingDuration = timestampMs - shakingEnteredAt
                if (shakingDuration >= config.holdThresholdMs) {
                    transitionToHoldConfirmed(timestampMs)
                }
            }
        }
    }

    // ── HOLD_CONFIRMED state logic ─────────────────────────────────────

    private fun handleHoldConfirmedUpdate(timestampMs: Long, magnitude: Float) {
        val isAboveThreshold = magnitude >= config.accelThreshold

        // Safety timeout: forces return to IDLE (PRD §5.4 HOLD_MAX_MS)
        val holdDuration = timestampMs - holdConfirmedAt
        if (holdDuration >= config.holdMaxMs) {
            returnToIdle()
            return
        }

        if (!isAboveThreshold) {
            if (belowThresholdSince == null) {
                belowThresholdSince = timestampMs
            }
            val belowDuration = timestampMs - (belowThresholdSince ?: timestampMs)
            if (belowDuration >= config.stabilizeMs) {
                // Hold gesture has ended naturally
                returnToIdle()
            }
        } else {
            belowThresholdSince = null
        }
    }

    // ── Transitions ────────────────────────────────────────────────────

    /**
     * SHAKING → commit as ShakeEvent → IDLE (the "short shake" path, FR-1).
     *
     * Only emits if the debounce gate allows (PRD §4).
     */
    private fun commitShakeEvent(timestampMs: Long) {
        val correlationId = currentCorrelationId ?: return

        if (debounceGate.tryPass(timestampMs)) {
            val event = ShakeEvent(
                correlationId = correlationId,
                timestampMonotonic = timestampMs,
            )
            onShakeEvent?.invoke(event)
        }

        returnToIdle()
    }

    /**
     * SHAKING → HOLD_CONFIRMED (the "shake-and-hold" path, FR-7).
     *
     * Emits a [ShakeAndHoldEvent]. Also emits a [ShakeCancellationSignal]
     * as a defensive measure (PRD §5.2) — in the current implementation,
     * no ShakeEvent should have been emitted while in SHAKING, but the
     * signal exists for Phase 2's stable contract.
     */
    private fun transitionToHoldConfirmed(timestampMs: Long) {
        val correlationId = currentCorrelationId ?: return

        currentState = State.HOLD_CONFIRMED
        holdConfirmedAt = timestampMs
        belowThresholdSince = null

        val holdDurationMs = timestampMs - shakingEnteredAt

        // Defensive cancellation signal (PRD §5.2)
        onCancellationSignal?.invoke(
            ShakeCancellationSignal(
                correlationId = correlationId,
                reason = "reclassified_as_hold",
            )
        )

        // Emit shake-and-hold event
        onShakeAndHoldEvent?.invoke(
            ShakeAndHoldEvent(
                correlationId = correlationId,
                timestampMonotonic = timestampMs,
                holdDurationMs = holdDurationMs,
            )
        )
    }

    /** Return to IDLE, clearing all in-flight gesture state. */
    private fun returnToIdle() {
        currentState = State.IDLE
        currentCorrelationId = null
        belowThresholdSince = null
        shakingEnteredAt = 0L
        holdConfirmedAt = 0L
    }

    /** Full reset — returns to IDLE and resets the debounce gate. */
    fun reset() {
        returnToIdle()
        debounceGate.reset()
    }
}
