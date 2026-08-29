package com.chirag.arthix.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Stub worker for Phase 4's idle-detection polling (PRD §11).
 *
 * Phase 4 implements the actual accelerometer-variance check and the
 * trigger-into-voice-follow-up callback here. This phase only provides
 * the scheduling infrastructure and a stub that returns success.
 *
 * The periodic re-scheduling and reboot-survival plumbing is handled
 * by [WorkScheduler].
 */
class IdleDetectionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "IdleDetectionWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "IdleDetectionWorker fired — stub (Phase 4 implements real logic)")
        // TODO: Phase 4 — implement accelerometer-variance check
        //       and voice-follow-up trigger callback
        return Result.success()
    }
}
