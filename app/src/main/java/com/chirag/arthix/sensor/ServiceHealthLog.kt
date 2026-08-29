package com.chirag.arthix.sensor

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock

/**
 * SharedPreferences-backed health tracker for the shake detection foreground service.
 *
 * Makes service death **traceable, not invisible** (PRD §7.4):
 * - Records clean shutdowns and startups
 * - Detects unclean restarts (OS kills, OriginOS aggressive killing — EC-59)
 * - Tracks restart count and gap durations
 * - Exposes data for Phase 4's debug UI or demo narration
 *
 * This is explicitly NOT expected to recover missed shakes retroactively —
 * motion data during a service gap is gone. The goal is *visibility* of the gap.
 */
class ServiceHealthLog(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Lifecycle recording ────────────────────────────────────────────

    /**
     * Record that the service is starting up.
     * Checks whether the previous session ended cleanly.
     * Call this in Service.onCreate().
     *
     * @return true if the previous shutdown was unclean (OS kill detected)
     */
    fun recordStartup(): Boolean {
        val wasClean = prefs.getBoolean(KEY_CLEAN_SHUTDOWN, true)
        val now = SystemClock.elapsedRealtime()

        if (!wasClean) {
            // Unclean restart detected — increment counter and record gap
            val currentCount = prefs.getInt(KEY_RESTART_COUNT, 0)
            val lastAliveAt = prefs.getLong(KEY_LAST_ALIVE_AT, 0L)

            prefs.edit()
                .putInt(KEY_RESTART_COUNT, currentCount + 1)
                .putLong(KEY_LAST_GAP_START_MS, lastAliveAt)
                .putLong(KEY_LAST_GAP_END_MS, now)
                .putBoolean(KEY_CLEAN_SHUTDOWN, false) // still running
                .putLong(KEY_LAST_ALIVE_AT, now)
                .apply()
        } else {
            prefs.edit()
                .putBoolean(KEY_CLEAN_SHUTDOWN, false) // mark as running (not yet cleanly shut down)
                .putLong(KEY_LAST_ALIVE_AT, now)
                .apply()
        }

        return !wasClean
    }

    /**
     * Record a clean shutdown. Call this in Service.onDestroy()
     * (or wherever the service shuts down intentionally).
     */
    fun recordCleanShutdown() {
        prefs.edit()
            .putBoolean(KEY_CLEAN_SHUTDOWN, true)
            .putLong(KEY_LAST_ALIVE_AT, SystemClock.elapsedRealtime())
            .apply()
    }

    /**
     * Update the "last alive" timestamp — call periodically (e.g., on each
     * sensor event or every N seconds) so that gap duration estimation is
     * more accurate.
     */
    fun updateAliveTimestamp() {
        prefs.edit()
            .putLong(KEY_LAST_ALIVE_AT, SystemClock.elapsedRealtime())
            .apply()
    }

    // ── Health data (readable by Phase 4 debug UI) ─────────────────────

    /** Whether the last shutdown was clean (true) or an OS kill (false). */
    val wasLastShutdownClean: Boolean
        get() = prefs.getBoolean(KEY_CLEAN_SHUTDOWN, true)

    /** Total count of detected unclean restarts since install. */
    val restartCount: Int
        get() = prefs.getInt(KEY_RESTART_COUNT, 0)

    /** Monotonic timestamp (ms) when the last detected gap started. 0 if no gap recorded. */
    val lastGapStartMs: Long
        get() = prefs.getLong(KEY_LAST_GAP_START_MS, 0L)

    /** Monotonic timestamp (ms) when the last detected gap ended. 0 if no gap recorded. */
    val lastGapEndMs: Long
        get() = prefs.getLong(KEY_LAST_GAP_END_MS, 0L)

    /** Estimated duration (ms) of the last service gap. 0 if no gap recorded. */
    val lastGapDurationMs: Long
        get() {
            val start = lastGapStartMs
            val end = lastGapEndMs
            return if (start > 0 && end > start) end - start else 0L
        }

    /** Reset all health counters — for testing or debug use. */
    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "service_health_log"

        private const val KEY_CLEAN_SHUTDOWN = "clean_shutdown"
        private const val KEY_LAST_ALIVE_AT = "last_alive_at"
        private const val KEY_RESTART_COUNT = "restart_count"
        private const val KEY_LAST_GAP_START_MS = "last_gap_start_ms"
        private const val KEY_LAST_GAP_END_MS = "last_gap_end_ms"
    }
}
