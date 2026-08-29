package com.chirag.arthix.notification

import android.content.Context
import android.content.SharedPreferences

/**
 * Runtime-tunable reconciliation engine parameters, backed by SharedPreferences.
 *
 * Same config-snapshot pattern as Phase 1's [ShakeDetectorConfig]:
 * the [ReconciliationConfigSnapshot] is a pure data class injectable
 * into the engine for testability; SharedPreferences provides persistence
 * for runtime tuning without rebuilds.
 *
 * All time values in milliseconds.
 */
class ReconciliationConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): ReconciliationConfigSnapshot = ReconciliationConfigSnapshot(
        maxDelayWindowMs = prefs.getLong(KEY_MAX_DELAY_WINDOW_MS, DEFAULT_MAX_DELAY_WINDOW_MS),
        ambiguityThresholdMs = prefs.getLong(KEY_AMBIGUITY_THRESHOLD_MS, DEFAULT_AMBIGUITY_THRESHOLD_MS),
        ambiguityMaxCandidates = prefs.getInt(KEY_AMBIGUITY_MAX_CANDIDATES, DEFAULT_AMBIGUITY_MAX_CANDIDATES),
        disambiguationTimeoutMs = prefs.getLong(KEY_DISAMBIGUATION_TIMEOUT_MS, DEFAULT_DISAMBIGUATION_TIMEOUT_MS),
        captureTimeoutMs = prefs.getLong(KEY_CAPTURE_TIMEOUT_MS, DEFAULT_CAPTURE_TIMEOUT_MS),
        notificationTimeoutMs = prefs.getLong(KEY_NOTIFICATION_TIMEOUT_MS, DEFAULT_NOTIFICATION_TIMEOUT_MS),
        dedupWindowMs = prefs.getLong(KEY_DEDUP_WINDOW_MS, DEFAULT_DEDUP_WINDOW_MS),
        dedupSimilarityThreshold = prefs.getFloat(KEY_DEDUP_SIMILARITY_THRESHOLD, DEFAULT_DEDUP_SIMILARITY_THRESHOLD.toFloat()).toDouble(),
        refundLookbackMs = prefs.getLong(KEY_REFUND_LOOKBACK_MS, DEFAULT_REFUND_LOOKBACK_MS),
    )

    companion object {
        const val PREFS_NAME = "reconciliation_config"

        // Keys
        private const val KEY_MAX_DELAY_WINDOW_MS = "max_delay_window_ms"
        private const val KEY_AMBIGUITY_THRESHOLD_MS = "ambiguity_threshold_ms"
        private const val KEY_AMBIGUITY_MAX_CANDIDATES = "ambiguity_max_candidates"
        private const val KEY_DISAMBIGUATION_TIMEOUT_MS = "disambiguation_timeout_ms"
        private const val KEY_CAPTURE_TIMEOUT_MS = "capture_timeout_ms"
        private const val KEY_NOTIFICATION_TIMEOUT_MS = "notification_timeout_ms"
        private const val KEY_DEDUP_WINDOW_MS = "dedup_window_ms"
        private const val KEY_DEDUP_SIMILARITY_THRESHOLD = "dedup_similarity_threshold"
        private const val KEY_REFUND_LOOKBACK_MS = "refund_lookback_ms"

        // Defaults (PRD §7.4, §7.5, §7.6, §6.2)
        const val DEFAULT_MAX_DELAY_WINDOW_MS = 120_000L       // 2 minutes
        const val DEFAULT_AMBIGUITY_THRESHOLD_MS = 3_000L      // 3s gap-between-candidates
        const val DEFAULT_AMBIGUITY_MAX_CANDIDATES = 3
        const val DEFAULT_DISAMBIGUATION_TIMEOUT_MS = 8_000L   // 8s user prompt timeout
        const val DEFAULT_CAPTURE_TIMEOUT_MS = 120_000L        // 2 minutes
        const val DEFAULT_NOTIFICATION_TIMEOUT_MS = 120_000L   // 2 minutes
        const val DEFAULT_DEDUP_WINDOW_MS = 10_000L            // 10s
        const val DEFAULT_DEDUP_SIMILARITY_THRESHOLD = 0.8
        const val DEFAULT_REFUND_LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}

/**
 * Immutable snapshot of reconciliation config — injected into pure-logic
 * engine for testability (no SharedPreferences dependency in tests).
 */
data class ReconciliationConfigSnapshot(
    val maxDelayWindowMs: Long = ReconciliationConfig.DEFAULT_MAX_DELAY_WINDOW_MS,
    val ambiguityThresholdMs: Long = ReconciliationConfig.DEFAULT_AMBIGUITY_THRESHOLD_MS,
    val ambiguityMaxCandidates: Int = ReconciliationConfig.DEFAULT_AMBIGUITY_MAX_CANDIDATES,
    val disambiguationTimeoutMs: Long = ReconciliationConfig.DEFAULT_DISAMBIGUATION_TIMEOUT_MS,
    val captureTimeoutMs: Long = ReconciliationConfig.DEFAULT_CAPTURE_TIMEOUT_MS,
    val notificationTimeoutMs: Long = ReconciliationConfig.DEFAULT_NOTIFICATION_TIMEOUT_MS,
    val dedupWindowMs: Long = ReconciliationConfig.DEFAULT_DEDUP_WINDOW_MS,
    val dedupSimilarityThreshold: Double = ReconciliationConfig.DEFAULT_DEDUP_SIMILARITY_THRESHOLD,
    val refundLookbackMs: Long = ReconciliationConfig.DEFAULT_REFUND_LOOKBACK_MS,
)
