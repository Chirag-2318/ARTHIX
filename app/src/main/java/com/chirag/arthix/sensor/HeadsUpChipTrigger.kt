package com.chirag.arthix.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Default [ChipTrigger] implementation — fires a heads-up notification
 * with category-selection action buttons.
 *
 * PRD §6.1–6.3:
 * - Uses high-importance notification channel (heads-up display, no SYSTEM_ALERT_WINDOW)
 * - Each category is a [Notification.Action] button carrying the correlationId
 *   in the PendingIntent extras
 * - Auto-dismisses via [NotificationCompat.Builder.setTimeoutAfter] (OS-managed)
 * - Visible over any foreground app (GPay, PhonePe, etc.)
 *
 * Phase 3 (Chirag) will replace or enhance this with the full chip rendering.
 * This implementation provides the minimum viable trigger.
 */
class HeadsUpChipTrigger(
    private val context: Context,
) : ChipTrigger {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChipNotificationChannel()
    }

    override fun fire(
        correlationId: String,
        categories: List<String>,
        autoDismissMs: Long,
    ) {
        val notificationId = correlationId.hashCode()

        val builder = NotificationCompat.Builder(context, CHIP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add) // Placeholder — Phase 3 replaces
            .setContentTitle("Arthix — Shake Detected!")
            .setContentText("Select category:")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setTimeoutAfter(autoDismissMs)

        // Add a Notification.Action for each category
        for ((index, category) in categories.withIndex()) {
            val actionIntent = Intent(ACTION_CATEGORY_SELECTED).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_CORRELATION_ID, correlationId)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + index + 1, // unique request code per action
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            builder.addAction(0, category, pendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Creates the high-importance notification channel for the heads-up chip.
     *
     * Separate from the low-importance foreground-service channel (PRD §7.2) —
     * two distinct channels, do not conflate.
     */
    private fun createChipNotificationChannel() {
        val channel = NotificationChannel(
            CHIP_CHANNEL_ID,
            "Shake category selection",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Heads-up chip for quick category selection after a shake"
            enableVibration(false) // Shake itself is the haptic cue
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHIP_CHANNEL_ID = "shake_chip_channel"

        /** Broadcast action fired when a category button is tapped. */
        const val ACTION_CATEGORY_SELECTED = "com.chirag.arthix.action.CATEGORY_SELECTED"

        /** Extra key: correlation ID linking back to the originating ShakeEvent. */
        const val EXTRA_CORRELATION_ID = "extra_correlation_id"

        /** Extra key: selected category string. */
        const val EXTRA_CATEGORY = "extra_category"

        /** Extra key: notification ID for dismissal. */
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        /** FR-1 fixed category set. */
        val FR1_CATEGORIES = listOf("Food", "Travel", "Shopping", "Other")
    }
}
