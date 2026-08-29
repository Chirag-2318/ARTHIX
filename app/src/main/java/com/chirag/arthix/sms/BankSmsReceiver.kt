package com.chirag.arthix.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for incoming SMS messages (Phase 2.1).
 *
 * Receives SMS_RECEIVED broadcasts from the Android telephony system.
 * NOT from Google Messages — directly from the telephony layer, which
 * provides the originating sender address from the SMS PDU.
 *
 * Security pipeline:
 * 1. Extract sender address from SMS PDU
 * 2. Check [BankSenderAllowList.isTrustedSender] — if NO → return (no body access)
 * 3. Check [BankSenderAllowList.isOtpSender] — if YES → return (no body access)
 * 4. Extract SMS body
 * 5. Parse via [BankSmsParser]
 * 6. Route to [TransactionIngestionRouter] via singleton reference
 *
 * Privacy: SMS body is NEVER logged, stored, or cached. Only the structured
 * parse result (amount, payee, direction) and a SHA-256 fingerprint enter
 * the pipeline.
 *
 * The receiver is declared in AndroidManifest with normal priority (0),
 * meaning we observe SMS but don't consume or block them.
 */
class BankSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BankSmsReceiver"

        /**
         * Singleton router reference — set by the service coordinator.
         *
         * This is a conscious architectural choice: BroadcastReceivers are
         * instantiated by Android with no-arg constructors. DI is not available.
         * The router is set once at app startup and never changes.
         */
        @Volatile
        var router: com.chirag.arthix.notification.TransactionIngestionRouter? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive fired")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val pendingResult = goAsync()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                for (smsMessage in messages) {
                    val sender = smsMessage.originatingAddress ?: continue
                    Log.d(TAG, "raw_sender=$sender")

                    // ──── SECURITY BOUNDARY — BEFORE any body access ────
                    if (!BankSenderAllowList.isTrustedSender(sender)) {
                        Log.d(TAG, "sender_trusted=false raw_sender=$sender")
                        continue  // HARD STOP — no body access, no logging
                    }
                    Log.d(TAG, "sender_trusted=true")

                    if (BankSenderAllowList.isOtpSender(sender)) {
                        continue  // OTP sender from a bank — reject
                    }
                    // ────────────────────────────────────────────────────

                    val body = smsMessage.messageBody ?: continue
                    val timestampMs = smsMessage.timestampMillis

                    // Structured log — no SMS content
                    Log.d(TAG, "BANK_SMS_RECEIVED sender_suffix=${sender.substringAfterLast("-")}")

                    // Parse and route
                    val candidate = BankSmsParser.parse(body, sender, timestampMs) ?: continue

                    val currentRouter = router
                    if (currentRouter != null) {
                        currentRouter.ingest(candidate)
                        Log.d(TAG, "Transaction candidate successfully handed to router")
                    } else {
                        Log.w(TAG, "Router not initialized — dropping bank SMS candidate")
                    }
                }
            } finally {
                Log.d(TAG, "SMS processing complete, finishing PendingResult")
                pendingResult.finish()
            }
        }
    }
}
