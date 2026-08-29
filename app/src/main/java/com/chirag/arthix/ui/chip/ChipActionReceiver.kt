package com.chirag.arthix.ui.chip

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.sensor.HeadsUpChipTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles category-selection taps from the
 * heads-up chip notification (PRD §3).
 *
 * Receives ACTION_CATEGORY_SELECTED from HeadsUpChipTrigger's PendingIntents.
 * On tap:
 * 1. Extracts correlationId and selected category from the intent
 * 2. Updates the PendingCapture's category in the database
 * 3. Dismisses the notification
 *
 * Uses a no-arg constructor (Android requirement for manifest-registered
 * BroadcastReceivers). Database access is via the singleton companion
 * reference set at app startup.
 */
class ChipActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ChipActionReceiver"

        /**
         * Singleton database reference — set once at app startup.
         * BroadcastReceivers cannot use Hilt injection (no-arg constructor).
         */
        @Volatile
        var database: ArthixDatabase? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HeadsUpChipTrigger.ACTION_CATEGORY_SELECTED) return

        val correlationId = intent.getStringExtra(HeadsUpChipTrigger.EXTRA_CORRELATION_ID) ?: return
        val category = intent.getStringExtra(HeadsUpChipTrigger.EXTRA_CATEGORY) ?: return
        val notificationId = intent.getIntExtra(HeadsUpChipTrigger.EXTRA_NOTIFICATION_ID, -1)

        Log.d(TAG, "Category selected: $category for capture $correlationId")

        // Dismiss the notification
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }

        // Update the PendingCapture's category in the database
        val db = database ?: run {
            Log.w(TAG, "Database not initialized — cannot save category")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val capture = db.pendingQueueDao().findCaptureById(correlationId)
                if (capture != null) {
                    db.pendingQueueDao().updateCapture(capture.copy(category = category))
                    Log.d(TAG, "Category '$category' saved for capture $correlationId")

                    // If there's already a matched transaction, update its category too
                    val txn = db.transactionDao().findBySourceCaptureId(correlationId)
                    if (txn != null) {
                        val newStatus = if (txn.amountPaise != null)
                            TransactionStatus.CONFIRMED
                        else
                            txn.status
                        db.transactionDao().update(
                            txn.copy(category = category, status = newStatus)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save category", e)
            }
        }
    }
}
