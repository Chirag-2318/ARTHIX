package com.chirag.arthix.sensor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Utility for requesting battery optimization exemption (PRD §7.3).
 *
 * Mitigates both EC-05 (general Doze throttling) and EC-59 (OriginOS
 * aggressive background-kill). This is a mitigation, not a guarantee —
 * per the edge-case doc's Fundamental Limits §2, no app-level code can
 * force an OEM not to kill a background service.
 *
 * Intended for use during first-run onboarding, before or alongside
 * the NotificationListenerService explainer (techstack §3.2/EC-58).
 */
object BatteryOptimizationHelper {

    /**
     * Check whether the app is already exempt from battery optimizations.
     *
     * @param context application context.
     * @return true if the app is on the battery-optimization whitelist.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Launch the system dialog requesting battery optimization exemption.
     *
     * Uses [Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] with the
     * app's package URI. This shows a one-tap confirmation dialog, not the
     * full battery settings page.
     *
     * Requires `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in
     * the manifest.
     *
     * @param context activity or application context. If an Activity context,
     *        the dialog appears in front of it; otherwise it starts a new task.
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
