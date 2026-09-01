package com.chirag.arthix.ui.overlay

import android.content.Context

/**
 * Controller singleton interface for managing the ARTHIX System Overlay.
 *
 * Dispatches commands to [FloatingChipOverlayService] cleanly and coordinates
 * expanded/collapsed states.
 */
object CaptureOverlayManager {

    /**
     * Show the expanded category selector overlay.
     */
    fun show(
        context: Context,
        correlationId: String,
        categories: List<String> = listOf("Food", "Travel", "Shopping", "Other"),
        durationMs: Long = 5000L,
    ) {
        FloatingChipOverlayService.show(
            context = context,
            correlationId = correlationId,
            categories = categories,
            autoDismissMs = durationMs,
        )
    }

    /**
     * Expand the overlay from collapsed badge state into the full category selector.
     */
    fun expand(context: Context, correlationId: String? = null) {
        FloatingChipOverlayService.expand(context, correlationId)
    }

    /**
     * Collapse the overlay from full bar into the compact persistent floating pill badge.
     */
    fun collapse(context: Context) {
        FloatingChipOverlayService.collapse(context)
    }

    /**
     * Dismiss and remove the overlay completely from the screen.
     */
    fun hide(context: Context) {
        FloatingChipOverlayService.hide(context)
    }
}
