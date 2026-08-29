package com.chirag.arthix.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.notification.model.ExtractedText
import com.chirag.arthix.notification.model.MatchResult
import com.chirag.arthix.notification.model.PatternConfig
import com.chirag.arthix.notification.model.TextSource
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.notification.model.ConfidenceLevel
import com.chirag.arthix.notification.model.NotificationOutcome
import com.chirag.arthix.notification.model.TransactionCandidate
import com.chirag.arthix.notification.model.TransactionSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * NotificationListenerService that captures UPI payment notifications (PRD §2).
 *
 * Lifecycle:
 * - Permission granted via system Settings (not runtime requestPermissions)
 * - [onListenerConnected]: binding confirmed, start receiving notifications
 * - [onNotificationPosted]: allow-list → extract → parse → route to engine
 * - [onListenerDisconnected]: OS unbound us (rebinding, kill, etc.)
 *
 * Security boundary (PRD §3 / EC-08 / EC-56):
 * Notifications from non-allow-listed packages are NEVER read, logged,
 * cached, or retained — the `if (!isAllowed) return` check is the very
 * first line of [onNotificationPosted].
 */
class UpiNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "UpiNotificationListener"

        // ── Observable connection state (PRD §2.3) ─────────────────────
        private val _connectionState = MutableStateFlow(ListenerConnectionState.DISCONNECTED)

        /**
         * Observable connection state for Phase 3 UI consumption.
         * Shows CONNECTING during the binding lag after grant/restart (EC-13).
         */
        val connectionState: StateFlow<ListenerConnectionState> = _connectionState.asStateFlow()

        /**
         * Singleton router reference — set by the service coordinator.
         */
        @Volatile
        var transactionRouter: TransactionIngestionRouter? = null
    }

    private var patternConfig: PatternConfig? = null

    // ── Lifecycle ──────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate — loading pattern config")
        _connectionState.value = ListenerConnectionState.CONNECTING

        // Load pattern config from assets
        patternConfig = loadPatternConfig()
        if (patternConfig == null) {
            Log.e(TAG, "Failed to load upi_patterns.json — notification parsing disabled!")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected — notifications are now being received")
        _connectionState.value = ListenerConnectionState.CONNECTED
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected — OS unbound the listener")
        _connectionState.value = ListenerConnectionState.DISCONNECTED
    }

    override fun onDestroy() {
        super.onDestroy()
        _connectionState.value = ListenerConnectionState.DISCONNECTED
    }

    // ── Notification handling ──────────────────────────────────────────

    /**
     * Entry point for every notification posted on the device.
     *
     * PRD §3.3 non-negotiable: for any packageName NOT on the allow-list,
     * this method returns IMMEDIATELY. No text field is read, logged, cached,
     * or retained — not even for debug/verbose logging.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // ──── SECURITY BOUNDARY — the very first line (PRD §3.3) ────
        if (!PackageAllowList.isAllowed(sbn.packageName)) {
            return  // HARD STOP — no logging, no text access
        }
        // ────────────────────────────────────────────────────────────

        val config = patternConfig ?: return
        val router = transactionRouter ?: return

        // Handle grouped/bundled notifications (EC-12)
        val entries = extractChildEntries(sbn)

        for (entry in entries) {
            processNotificationText(entry, sbn.packageName, config, router)
        }
    }

    /**
     * Process a single notification text entry through the full pipeline:
     * extract → pattern match → outcome filter → dedup → route to engine.
     */
    private fun processNotificationText(
        extractedText: ExtractedText,
        packageName: String,
        config: PatternConfig,
        router: TransactionIngestionRouter,
    ) {
        val result = NotificationPatternMatcher.match(extractedText, packageName, config)

        when (result) {
            is MatchResult.OutflowMatch -> {
                router.ingest(
                    TransactionCandidate(
                        sourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
                        sourcePackage = packageName,
                        senderAddress = null,
                        amountPaise = result.parsed.amountPaise,
                        payee = result.parsed.payee,
                        direction = Direction.OUTFLOW,
                        referenceId = null,
                        rawFingerprint = com.chirag.arthix.sms.BankSmsParser.sha256(result.parsed.rawText),
                        confidence = if (result.parsed.confidenceFlag == ConfidenceFlag.CLEAN) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM,
                        timestampMs = android.os.SystemClock.elapsedRealtime(),
                        outcome = NotificationOutcome.COMPLETED
                    )
                )
            }
            is MatchResult.InflowMatch -> {
                router.ingest(
                    TransactionCandidate(
                        sourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
                        sourcePackage = packageName,
                        senderAddress = null,
                        amountPaise = result.parsed.amountPaise,
                        payee = result.parsed.payee,
                        direction = Direction.INFLOW,
                        referenceId = null,
                        rawFingerprint = com.chirag.arthix.sms.BankSmsParser.sha256(extractedText.raw),
                        confidence = ConfidenceLevel.HIGH,
                        timestampMs = android.os.SystemClock.elapsedRealtime(),
                        outcome = NotificationOutcome.COMPLETED
                    )
                )
            }
            is MatchResult.RefundMatch -> {
                router.ingest(
                    TransactionCandidate(
                        sourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
                        sourcePackage = packageName,
                        senderAddress = null,
                        amountPaise = result.amountPaise,
                        payee = result.payee,
                        direction = Direction.INFLOW,
                        referenceId = null,
                        rawFingerprint = com.chirag.arthix.sms.BankSmsParser.sha256(extractedText.raw),
                        confidence = ConfidenceLevel.HIGH,
                        timestampMs = android.os.SystemClock.elapsedRealtime(),
                        outcome = NotificationOutcome.REFUND
                    )
                )
            }
            is MatchResult.Rejected -> {
                // Silently ignore rejected outcomes
            }
            is MatchResult.NoMatch -> {
                // Not a payment notification — silently ignore
            }
        }
    }

    // ── Text extraction (PRD §4) ───────────────────────────────────────

    /**
     * Extract text from a notification with priority chain (PRD §4.1):
     * EXTRA_BIG_TEXT > EXTRA_TEXT_LINES > EXTRA_TEXT.
     */
    private fun extractText(notification: Notification): ExtractedText {
        val extras = notification.extras

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        if (bigText != null) {
            return ExtractedText(bigText.toString(), TextSource.BIG_TEXT)
        }

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            return ExtractedText(
                textLines.joinToString("\n") { it.toString() },
                TextSource.TEXT_LINES
            )
        }

        val fallback = extras.getCharSequence(Notification.EXTRA_TEXT)
        if (fallback != null) {
            return ExtractedText(fallback.toString(), TextSource.TEXT_FALLBACK)
        }

        return ExtractedText("", TextSource.NONE)
    }

    /**
     * Handle grouped/bundled notifications (PRD §4.3 / EC-12).
     *
     * Android may collapse rapid same-app notifications into a single summary
     * using InboxStyle. Each line in EXTRA_TEXT_LINES represents one bundled
     * notification's text — each is independently pattern-matched.
     */
    private fun extractChildEntries(sbn: StatusBarNotification): List<ExtractedText> {
        val extras = sbn.notification.extras
        val inboxLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        return if (!inboxLines.isNullOrEmpty()) {
            // Each inbox line = one bundled notification
            inboxLines.map { ExtractedText(it.toString(), TextSource.TEXT_LINES) }
        } else {
            // Single, ungrouped notification
            listOf(extractText(sbn.notification))
        }
    }

    // ── Config loading ─────────────────────────────────────────────────

    /**
     * Load pattern config from assets/upi_patterns.json.
     */
    private fun loadPatternConfig(): PatternConfig? {
        return try {
            val inputStream = assets.open("upi_patterns.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val json = reader.readText()
            reader.close()
            NotificationPatternMatcher.parseConfig(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pattern config", e)
            null
        }
    }

    // ── Engine injection (removed setter, handled via companion object) ──
}
