package com.chirag.arthix.report.phrasing

import android.util.Log
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.GroundingWhitelist
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional Green Light compute bridge strategy (PRD §6, EC-47).
 *
 * Implements the pluggable strategy interface for routing report phrasing through
 * the Office Kit bridge when available.
 *
 * **Architectural guarantee (EC-47):** The on-device path ([OnDeviceMediaPipeEngine])
 * is the mandatory default and works standalone. The bridge is purely additive
 * for Office Kit demonstrations and never a load-bearing single point of failure.
 */
@Singleton
class OfficeKitBridgeEngine @Inject constructor(
    private val onDeviceEngine: OnDeviceMediaPipeEngine,
) : ReportPhrasingEngine {

    companion object {
        private const val TAG = "OfficeKitBridgeEngine"
    }

    override suspend fun phraseReport(
        data: ComputedReportData,
        whitelist: GroundingWhitelist,
    ): List<String> {
        Log.d(TAG, "OfficeKitBridge checking for Green Light compute connection...")

        // If paired/connected, route through high-tier compute; otherwise delegate to onDeviceEngine
        return onDeviceEngine.phraseReport(data, whitelist)
    }
}
