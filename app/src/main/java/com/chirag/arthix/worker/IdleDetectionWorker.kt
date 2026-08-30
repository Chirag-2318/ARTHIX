package com.chirag.arthix.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chirag.arthix.voice.IdleDetector
import com.chirag.arthix.voice.VoiceFollowUpSession
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Worker for Phase 4's idle-detection polling and voice follow-up triggering (FR-3, PRD §11).
 *
 * Checks all conditions via [IdleDetector]:
 * - Screen recently unlocked / active in last 30 min (EC-26)
 * - Phone is NOT in DND / silenced (EC-26)
 * - Pending voice-resolvable transactions exist (AWAITING_AMOUNT or AWAITING_CATEGORY)
 *
 * If triggered, launches [VoiceFollowUpSession].
 * Self-reschedules via [WorkScheduler] on each run.
 */
class IdleDetectionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface IdleDetectionWorkerEntryPoint {
        fun idleDetector(): IdleDetector
        fun voiceFollowUpSession(): VoiceFollowUpSession
    }

    companion object {
        private const val TAG = "IdleDetectionWorker"
        const val PREFS_NAME = "arthix_user_activity"
        const val KEY_LAST_SCREEN_ACTIVE_MS = "last_screen_active_ms"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "IdleDetectionWorker executing idle check")

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                IdleDetectionWorkerEntryPoint::class.java,
            )
            val idleDetector = entryPoint.idleDetector()
            val voiceFollowUpSession = entryPoint.voiceFollowUpSession()

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastActive = prefs.getLong(
                KEY_LAST_SCREEN_ACTIVE_MS,
                android.os.SystemClock.elapsedRealtime(),
            )

            if (idleDetector.shouldTrigger(lastActive)) {
                Log.i(TAG, "Idle condition met & pending transactions found — starting VoiceFollowUpSession")
                voiceFollowUpSession.run()
            } else {
                Log.d(TAG, "Idle trigger conditions not met — skipping voice follow-up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during idle detection check", e)
        } finally {
            // Self-reschedule for next poll cycle (chained one-shot pattern)
            WorkScheduler.scheduleIdleDetection(applicationContext)
        }

        return Result.success()
    }
}
