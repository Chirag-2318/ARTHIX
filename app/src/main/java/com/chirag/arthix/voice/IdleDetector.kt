package com.chirag.arthix.voice

import com.chirag.arthix.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks all conditions required before triggering an idle-state voice follow-up.
 *
 * All four conditions must be true simultaneously (EC-26):
 * 1. The device has been stationary (low accelerometer variance — evaluated by caller;
 *    [IdleDetector] checks the *other* conditions only, since accelerometer readings
 *    are passed in from the WorkManager context that already has sensor data).
 * 2. Screen was recently interactive — guards against overnight-charging false fires.
 * 3. Do Not Disturb / ringer is NOT silenced.
 * 4. At least one AWAITING_AMOUNT or AWAITING_CATEGORY transaction exists.
 *
 * Zero-Android-business-logic: pure Kotlin with injected interface dependencies,
 * allowing 100% JVM unit testability without Robolectric.
 */
@Singleton
class IdleDetector @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val clock: MonotonicClock,
    private val screenChecker: ScreenInteractivityChecker,
    private val audioChecker: AudioSilenceChecker,
) {

    companion object {
        /**
         * How recently (in ms) the screen must have been interactive for the
         * prompt to fire. 30 minutes covers "I was just using my phone" scenarios.
         */
        const val SCREEN_ACTIVE_WINDOW_MS = 30L * 60 * 1000  // 30 min
    }

    /**
     * Returns true if the voice follow-up should fire.
     *
     * @param lastScreenActiveMs epoch millis of the last screen-unlock/interaction,
     *   stored in SharedPreferences and updated by the app when the user interacts.
     */
    suspend fun shouldTrigger(lastScreenActiveMs: Long): Boolean {
        val nowMs = clock.elapsedRealtimeMs()

        // Condition 2: screen recently active (EC-26)
        val screenRecentlyActive = (nowMs - lastScreenActiveMs) < SCREEN_ACTIVE_WINDOW_MS
        if (!screenRecentlyActive) {
            return false
        }

        // Condition 3: not silenced / DND (EC-26)
        if (audioChecker.isSilenced()) {
            return false
        }

        // Condition 4: pending records exist
        val hasPending = transactionRepository.hasPendingVoiceRecords()
        if (!hasPending) {
            return false
        }

        return true
    }
}

// ── Thin injectable interfaces (enable JVM unit testing) ──────────────────────

/** Abstraction over SystemClock.elapsedRealtime() so tests can inject a fake. */
interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

/** Abstraction over AudioManager ringer/DND state checks. */
interface AudioSilenceChecker {
    /** Returns true if the device is silenced or in Do Not Disturb mode. */
    fun isSilenced(): Boolean
}

/** Abstraction over PowerManager interactive state. */
interface ScreenInteractivityChecker {
    fun isInteractive(): Boolean
}
