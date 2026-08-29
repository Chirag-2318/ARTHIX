package com.chirag.arthix.sensor

/**
 * A debounced, oscillation-validated, hold-disambiguated short shake event.
 *
 * Consumed by Phase 2 (Reconciliation Engine) — receipt means
 * "create exactly one PendingCapture" with no further gating.
 *
 * PRD §5.5 — Phase-1 event payload crossing the module boundary.
 */
data class ShakeEvent(
    /** UUID generated at commit time — join key for chip trigger + Phase 2 PendingCapture. */
    val correlationId: String,

    /**
     * [android.os.SystemClock.elapsedRealtime] at commit.
     * Monotonic clock — never wall clock (EC-19).
     */
    val timestampMonotonic: Long,
)

/**
 * A shake-and-hold event — the user held sustained motion past [holdDurationMs].
 *
 * Consumed by Phase 5 (Report trigger). Phase 5 does not need additional
 * context from this phase to trigger report generation.
 *
 * PRD §5.5 — Phase-1 event payload crossing the module boundary.
 */
data class ShakeAndHoldEvent(
    /** UUID generated at HOLD_CONFIRMED transition. */
    val correlationId: String,

    /**
     * [android.os.SystemClock.elapsedRealtime] at HOLD_CONFIRMED transition.
     * Monotonic clock — never wall clock (EC-19).
     */
    val timestampMonotonic: Long,

    /** Actual measured hold duration (ms) — for future tuning/telemetry. */
    val holdDurationMs: Long,
)

/**
 * Defensive cancellation signal — invalidates a previously emitted [ShakeEvent]
 * that has been reclassified (e.g., as a hold gesture).
 *
 * Per PRD §5.2–5.3: in the current straight-line implementation this is
 * a defensive/traceability measure (no ShakeEvent should have been emitted
 * while still in SHAKING), but the interface must exist for Phase 2's
 * stable contract regardless of internal implementation changes.
 *
 * PRD §5.5 — Phase-1 event payload crossing the module boundary.
 */
data class ShakeCancellationSignal(
    /** Matches the [ShakeEvent.correlationId] being invalidated. */
    val correlationId: String,

    /** Reason for cancellation — default "reclassified_as_hold". */
    val reason: String = "reclassified_as_hold",
)
