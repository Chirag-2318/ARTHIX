package com.chirag.arthix.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the idle-detection polling infrastructure (PRD §11).
 *
 * Uses a chained one-shot pattern (rather than PeriodicWorkRequest) because
 * PeriodicWorkRequest's minimum interval is 15 minutes — idle-detection
 * likely needs finer granularity.
 *
 * The work is registered under a stable unique name so app restarts
 * don't stack duplicate schedules.
 */
object WorkScheduler {

    private const val TAG = "WorkScheduler"
    private const val WORK_NAME = "idle_detection_poll"

    /** Default poll interval — Phase 4 may tune this. */
    private const val POLL_INTERVAL_MINUTES = 10L

    /**
     * Register the idle-detection polling schedule.
     *
     * Call once at app startup. Uses REPLACE policy so duplicate
     * registrations are harmless.
     */
    fun scheduleIdleDetection(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<IdleDetectionWorker>()
            .setInitialDelay(POLL_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )

        Log.d(TAG, "Idle detection poll scheduled (${POLL_INTERVAL_MINUTES}min delay)")
    }
}
