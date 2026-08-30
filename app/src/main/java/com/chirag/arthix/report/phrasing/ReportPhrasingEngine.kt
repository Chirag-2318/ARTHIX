package com.chirag.arthix.report.phrasing

import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.GroundingWhitelist

/**
 * Pluggable strategy interface for report text phrasing (PRD §6, EC-47).
 *
 * Provides interchangeable engines:
 * - [TemplatePhrasingEngine]: 100% deterministic, instant, zero-LLM template generator.
 * - [OnDeviceMediaPipeEngine]: On-device LLM (Gemma 3 1B / MediaPipe) with strict prompt grounding.
 * - [OfficeKitBridgeEngine]: Optional Green Light compute bridge (additive only, never load-bearing).
 */
interface ReportPhrasingEngine {
    /**
     * Produce a list of phrased report insight sentences given verified [data] and [whitelist].
     */
    suspend fun phraseReport(
        data: ComputedReportData,
        whitelist: GroundingWhitelist,
    ): List<String>
}
