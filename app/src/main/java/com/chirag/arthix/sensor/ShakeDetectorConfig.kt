package com.chirag.arthix.sensor

import android.content.Context
import android.content.SharedPreferences

/**
 * Runtime-tunable configuration for the shake detection pipeline.
 *
 * All parameters are [SharedPreferences]-backed so they can be adjusted
 * without a rebuild — per PRD §3.5 and §5.4, this is a hard requirement,
 * not optional polish. Phase 5 testing determines final values; this phase's
 * job is to make that a config change, not a code change.
 *
 * STARTING VALUES (require empirical tuning on real device):
 * - ACCEL_THRESHOLD: 12 m/s² — deliberate wrist-flick should clear; walking should not
 * - T_WINDOW_MS: 500 ms — reversal counting budget
 * - MIN_REVERSALS: 2 — hard requirement per EC-01, not tunable down to 1
 * - HOLD_THRESHOLD_MS: 1200 ms — sustained motion past this = shake-and-hold
 * - STABILIZE_MS: 150 ms — below-threshold duration to consider motion "stopped"
 * - HOLD_MAX_MS: 5000 ms — safety ceiling, forces return to IDLE
 * - DEBOUNCE_MS: 2000 ms — single physical shake → single event
 */
class ShakeDetectorConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Oscillation detector parameters (PRD §3.3) ─────────────────────

    /** Linear-acceleration magnitude (m/s²) a deliberate shake must clear. */
    var accelThreshold: Float
        get() = prefs.getFloat(KEY_ACCEL_THRESHOLD, DEFAULT_ACCEL_THRESHOLD)
        set(value) = prefs.edit().putFloat(KEY_ACCEL_THRESHOLD, value).apply()

    /** Time window (ms) within which [minReversals] direction reversals must occur. */
    var tWindowMs: Long
        get() = prefs.getLong(KEY_T_WINDOW_MS, DEFAULT_T_WINDOW_MS)
        set(value) = prefs.edit().putLong(KEY_T_WINDOW_MS, value).apply()

    /**
     * Minimum reversal count to classify motion as a shake.
     * Hard requirement ≥ 2 per EC-01 — do not tune down to 1.
     */
    var minReversals: Int
        get() = prefs.getInt(KEY_MIN_REVERSALS, DEFAULT_MIN_REVERSALS)
        set(value) = prefs.edit().putInt(KEY_MIN_REVERSALS, maxOf(2, value)).apply()

    // ── Gesture state machine parameters (PRD §5.4) ────────────────────

    /** Sustained motion past this duration (ms) → shake-and-hold, not short shake. */
    var holdThresholdMs: Long
        get() = prefs.getLong(KEY_HOLD_THRESHOLD_MS, DEFAULT_HOLD_THRESHOLD_MS)
        set(value) = prefs.edit().putLong(KEY_HOLD_THRESHOLD_MS, value).apply()

    /**
     * Below-threshold duration (ms) required to consider motion "stopped."
     * Prevents a single mid-shake dip from being misread as gesture end.
     */
    var stabilizeMs: Long
        get() = prefs.getLong(KEY_STABILIZE_MS, DEFAULT_STABILIZE_MS)
        set(value) = prefs.edit().putLong(KEY_STABILIZE_MS, value).apply()

    /** Safety ceiling (ms) — forces return to IDLE even if motion never cleanly drops. */
    var holdMaxMs: Long
        get() = prefs.getLong(KEY_HOLD_MAX_MS, DEFAULT_HOLD_MAX_MS)
        set(value) = prefs.edit().putLong(KEY_HOLD_MAX_MS, value).apply()

    // ── Debounce parameter (PRD §4.2) ──────────────────────────────────

    /** Suppression window (ms) after an emitted event — prevents double-fire. */
    var debounceMs: Long
        get() = prefs.getLong(KEY_DEBOUNCE_MS, DEFAULT_DEBOUNCE_MS)
        set(value) = prefs.edit().putLong(KEY_DEBOUNCE_MS, value).apply()

    companion object {
        const val PREFS_NAME = "shake_detector_config"

        // Keys
        private const val KEY_ACCEL_THRESHOLD = "accel_threshold"
        private const val KEY_T_WINDOW_MS = "t_window_ms"
        private const val KEY_MIN_REVERSALS = "min_reversals"
        private const val KEY_HOLD_THRESHOLD_MS = "hold_threshold_ms"
        private const val KEY_STABILIZE_MS = "stabilize_ms"
        private const val KEY_HOLD_MAX_MS = "hold_max_ms"
        private const val KEY_DEBOUNCE_MS = "debounce_ms"

        // Defaults (starting values — require empirical tuning)
        const val DEFAULT_ACCEL_THRESHOLD = 15f           // m/s²
        const val DEFAULT_T_WINDOW_MS = 1000L             // ms
        const val DEFAULT_MIN_REVERSALS = 3               // hard minimum per EC-01
        const val DEFAULT_HOLD_THRESHOLD_MS = 1200L       // ms
        const val DEFAULT_STABILIZE_MS = 150L             // ms
        const val DEFAULT_HOLD_MAX_MS = 5000L             // ms
        const val DEFAULT_DEBOUNCE_MS = 2000L             // ms
    }
}

/**
 * Pure-data snapshot of [ShakeDetectorConfig] for injection into
 * pure-logic classes ([OscillationDetector], [GestureStateMachine], [DebounceGate])
 * that must not depend on Android [SharedPreferences] directly.
 *
 * Constructed via [ShakeDetectorConfig.snapshot] or directly in tests.
 */
data class ShakeDetectorConfigSnapshot(
    val accelThreshold: Float = ShakeDetectorConfig.DEFAULT_ACCEL_THRESHOLD,
    val tWindowMs: Long = ShakeDetectorConfig.DEFAULT_T_WINDOW_MS,
    val minReversals: Int = ShakeDetectorConfig.DEFAULT_MIN_REVERSALS,
    val holdThresholdMs: Long = ShakeDetectorConfig.DEFAULT_HOLD_THRESHOLD_MS,
    val stabilizeMs: Long = ShakeDetectorConfig.DEFAULT_STABILIZE_MS,
    val holdMaxMs: Long = ShakeDetectorConfig.DEFAULT_HOLD_MAX_MS,
    val debounceMs: Long = ShakeDetectorConfig.DEFAULT_DEBOUNCE_MS,
)

/** Creates a pure-data snapshot of the current config values. */
fun ShakeDetectorConfig.snapshot(): ShakeDetectorConfigSnapshot =
    ShakeDetectorConfigSnapshot(
        accelThreshold = accelThreshold,
        tWindowMs = tWindowMs,
        minReversals = minReversals,
        holdThresholdMs = holdThresholdMs,
        stabilizeMs = stabilizeMs,
        holdMaxMs = holdMaxMs,
        debounceMs = debounceMs,
    )
