package com.chirag.arthix.sensor

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.chirag.arthix.ui.overlay.FloatingChipOverlayService

/**
 * Hybrid [ChipTrigger] implementation:
 *
 * 1. Checks if SYSTEM_ALERT_WINDOW ("Display over other apps") is granted.
 * 2. If granted: launches the isolated [FloatingChipOverlayService] to display
 *    the floating on-screen pop-up (completely immune to GPay/Bank notification interference).
 * 3. If not granted: gracefully falls back to [HeadsUpChipTrigger] notification.
 */
class OverlayChipTrigger(
    private val context: Context,
    private val fallbackTrigger: ChipTrigger = HeadsUpChipTrigger(context),
) : ChipTrigger {

    companion object {
        private const val TAG = "OverlayChipTrigger"
        const val DEFAULT_POPUP_DURATION_MS = 7000L
    }

    override fun fire(
        correlationId: String,
        categories: List<String>,
        autoDismissMs: Long,
    ) {
        val duration = if (autoDismissMs < 5000L) DEFAULT_POPUP_DURATION_MS else autoDismissMs

        if (Settings.canDrawOverlays(context)) {
            Log.d(TAG, "SYSTEM_ALERT_WINDOW granted — firing Floating Pop-up Window ($duration ms)")
            FloatingChipOverlayService.show(
                context = context,
                correlationId = correlationId,
                categories = categories,
                autoDismissMs = duration
            )
        } else {
            Log.d(TAG, "SYSTEM_ALERT_WINDOW not granted — falling back to Heads-Up Notification")
            fallbackTrigger.fire(
                correlationId = correlationId,
                categories = categories,
                autoDismissMs = duration
            )
        }
    }
}
