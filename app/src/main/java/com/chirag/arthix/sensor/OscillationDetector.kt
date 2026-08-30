package com.chirag.arthix.sensor

/**
 * Oscillation-based shake detector — pure logic, no Android dependencies.
 *
 * Detects deliberate shake gestures by counting **direction reversals**
 * within a rolling time window, rather than relying on a single acceleration
 * spike (which is indistinguishable from drops, potholes, or firm taps).
 *
 * A reversal is logged when the acceleration magnitude swings from above
 * [config.accelThreshold] to below the negative of the derivative threshold
 * (i.e., the phone accelerated hard one way, then hard the other way).
 *
 * PRD §3.1–3.3. All parameters are runtime-tunable via [ShakeDetectorConfigSnapshot].
 *
 * Thread safety: NOT thread-safe. Caller must ensure [onSensorSample] is invoked
 * from a single thread (the SensorEventListener callback thread in production).
 */
class OscillationDetector(
    private val config: ShakeDetectorConfigSnapshot = ShakeDetectorConfigSnapshot(),
) {

    /**
     * Callback invoked when a SHAKE_ONSET is detected (≥ [config.minReversals]
     * reversals within [config.tWindowMs]).
     */
    var onShakeOnset: ((timestampMs: Long) -> Unit)? = null

    // ── Internal state ─────────────────────────────────────────────────

    /**
     * Represents a threshold-crossing event.
     * [timestampMs] is when the crossing occurred.
     * [positive] is true if the magnitude crossed above +threshold (peak),
     * false if it crossed below (trough on the derivative).
     */
    private data class CrossingEvent(
        val timestampMs: Long,
        val positive: Boolean,
    )

    /** Rolling list of threshold crossings within the detection window. */
    private val crossings = mutableListOf<CrossingEvent>()

    /**
     * Direction of the last threshold crossing:
     * - null = no crossing yet
     * - true = last crossing was above +threshold (positive peak)
     * - false = last crossing was below -threshold (negative trough)
     */
    private var lastCrossingDirection: Boolean? = null

    /**
     * Whether the magnitude is currently above the threshold.
     * Used to detect transitions (edges), not sustained states.
     */
    private var isAboveThreshold = false

    /**
     * Whether the magnitude was above threshold on the previous sample.
     * Used for edge detection.
     */
    private var wasAboveThreshold = false

    /**
     * Previous magnitude sample — used to compute signed derivative
     * for reversal direction.
     */
    private var prevMagnitude = 0f

    /** Whether we've received at least one sample (so prevMagnitude is valid). */
    private var hasPrevSample = false

    /**
     * Process a new sensor sample.
     *
     * @param timestampMs monotonic timestamp (ms) — [SystemClock.elapsedRealtime] in production.
     * @param magnitude linear-acceleration magnitude (m/s²), i.e. sqrt(x² + y² + z²).
     */
    fun onSensorSample(timestampMs: Long, magnitude: Float) {
        if (!hasPrevSample) {
            prevMagnitude = magnitude
            hasPrevSample = true
            return
        }

        val threshold = config.accelThreshold
        isAboveThreshold = magnitude >= threshold

        // Edge detection: we care about transitions, not sustained states.
        // A "positive crossing" = magnitude just crossed above threshold (rising edge).
        // A "negative crossing" = magnitude just dropped below threshold (falling edge).
        if (isAboveThreshold && !wasAboveThreshold) {
            // Rising edge — magnitude crossed above threshold
            onThresholdCrossing(timestampMs, positive = true)
        } else if (!isAboveThreshold && wasAboveThreshold) {
            // Falling edge — magnitude dropped below threshold
            onThresholdCrossing(timestampMs, positive = false)
        }

        wasAboveThreshold = isAboveThreshold
        prevMagnitude = magnitude
    }

    /**
     * Handle a threshold crossing event. A reversal is counted when
     * the crossing direction differs from the last one (the signal
     * swung from one side to the other).
     */
    private fun onThresholdCrossing(timestampMs: Long, positive: Boolean) {
        // Prune crossings outside the detection window
        pruneOldCrossings(timestampMs)

        // Only count as a reversal if direction differs from the last crossing
        val isReversal = lastCrossingDirection != null && lastCrossingDirection != positive

        // Record this crossing
        crossings.add(CrossingEvent(timestampMs, positive))
        lastCrossingDirection = positive

        if (isReversal) {
            // Count total reversals in the current window
            val reversalCount = countReversalsInWindow(timestampMs)
            if (reversalCount >= config.minReversals) {
                // SHAKE_ONSET detected
                onShakeOnset?.invoke(timestampMs)
                // Reset state after onset — start fresh for the next gesture
                reset()
            }
        }
    }

    /**
     * Count the number of direction reversals in the current window.
     * A reversal = consecutive crossings with different directions.
     */
    private fun countReversalsInWindow(currentTimestampMs: Long): Int {
        val windowStart = currentTimestampMs - config.tWindowMs
        val windowedCrossings = crossings.filter { it.timestampMs >= windowStart }

        if (windowedCrossings.size < 2) return 0

        var reversals = 0
        for (i in 1 until windowedCrossings.size) {
            if (windowedCrossings[i].positive != windowedCrossings[i - 1].positive) {
                reversals++
            }
        }
        return reversals
    }

    /** Remove crossings older than the detection window. */
    private fun pruneOldCrossings(currentTimestampMs: Long) {
        val windowStart = currentTimestampMs - config.tWindowMs
        crossings.removeAll { it.timestampMs < windowStart }
        if (crossings.isEmpty()) {
            lastCrossingDirection = null
        } else {
            lastCrossingDirection = crossings.last().positive
        }
    }

    /** Reset all detector state — called after onset fires or externally. */
    fun reset() {
        crossings.clear()
        lastCrossingDirection = null
        isAboveThreshold = false
        wasAboveThreshold = false
        prevMagnitude = 0f
        hasPrevSample = false
    }
}
