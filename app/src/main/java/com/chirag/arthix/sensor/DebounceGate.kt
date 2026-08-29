package com.chirag.arthix.sensor

/**
 * Debounce gate — ensures a single physical shake never produces more
 * than one committed Phase-1 event.
 *
 * PRD §4: "a rapid double-shake within 2s produces exactly one PendingCapture, not two."
 *
 * Pure logic, injectable time source for testability. No Android dependencies.
 *
 * Semantics (PRD §4.2):
 * - On first [tryPass] → allows passage, starts a debounce window of [debounceMs].
 * - While the window is active, subsequent [tryPass] calls are suppressed (return false).
 * - Suppressed passes do NOT extend the window — a shake at t=0 opens a window
 *   through t=2000ms; a second shake at t=1500ms is suppressed and does NOT
 *   push the window to t=3500ms.
 * - A pass after the window expires starts a fresh window.
 *
 * Thread safety: NOT thread-safe. Caller must ensure sequential invocation
 * (guaranteed by the single SensorEventListener callback thread in production).
 */
class DebounceGate(
    private val debounceMs: Long = ShakeDetectorConfig.DEFAULT_DEBOUNCE_MS,
) {

    /**
     * Monotonic timestamp (ms) at which the current debounce window expires.
     * Null if no window is active (first pass or after expiry).
     */
    private var windowExpiresAt: Long? = null

    /**
     * Attempt to pass through the debounce gate.
     *
     * @param timestampMs current monotonic time (ms) — [SystemClock.elapsedRealtime] in production.
     * @return true if the event should be emitted (gate open); false if suppressed (gate closed).
     */
    fun tryPass(timestampMs: Long): Boolean {
        val expiresAt = windowExpiresAt

        return if (expiresAt == null || timestampMs >= expiresAt) {
            // Gate is open — allow passage, start a new debounce window
            windowExpiresAt = timestampMs + debounceMs
            true
        } else {
            // Gate is closed — suppress, do NOT extend the window
            false
        }
    }

    /** Reset the gate to its initial state (no active window). */
    fun reset() {
        windowExpiresAt = null
    }
}
