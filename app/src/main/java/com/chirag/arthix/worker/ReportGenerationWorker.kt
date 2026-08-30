package com.chirag.arthix.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chirag.arthix.report.ReportGenerator
import com.chirag.arthix.report.model.ReportPeriod
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Background worker for scheduled report generation (PRD §2, FR-7, EC-50).
 */
class ReportGenerationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReportWorkerEntryPoint {
        fun reportGenerator(): ReportGenerator
    }

    companion object {
        private const val TAG = "ReportGenerationWorker"
        private const val PERIODIC_WORK_NAME = "arthix_periodic_report_gen"

        /**
         * Schedule daily/weekly periodic report generation.
         */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReportGenerationWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.d(TAG, "Scheduled periodic report generation")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Executing scheduled report generation worker")
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ReportWorkerEntryPoint::class.java,
            )
            val generator = entryPoint.reportGenerator()
            generator.generateAndSaveReport(ReportPeriod.currentWeek())
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReportGenerationWorker", e)
            Result.retry()
        }
    }
}
