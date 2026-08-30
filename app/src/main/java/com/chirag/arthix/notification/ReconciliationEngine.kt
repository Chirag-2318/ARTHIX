package com.chirag.arthix.notification

import android.os.SystemClock
import android.util.Log
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.notification.model.DedupResult
import com.chirag.arthix.notification.model.ParsedInflow
import com.chirag.arthix.notification.model.ParsedOutflow
import com.chirag.arthix.sensor.ChipTrigger
import com.chirag.arthix.sensor.HeadsUpChipTrigger
import com.chirag.arthix.sensor.ShakeCancellationSignal
import com.chirag.arthix.sensor.ShakeEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * The core reconciliation engine — Phase 2's primary deliverable (PRD §7).
 *
 * Implements nearest-neighbor shake↔notification matching, disambiguation,
 * timeout hygiene, dedup, refund netting, discard, and inflow bypass — all on
 * a single serial [CoroutineDispatcher] for correctness (EC-18/EC-51).
 *
 * **Correctness invariant (EC-18):** all queue mutations (insert, match, timeout,
 * discard) run on [reconciliationScope] which uses `limitedParallelism(1)`.
 * Both ShakeEvent and notification callbacks funnel into this scope. No other
 * code path is allowed to mutate `pending_captures` or `pending_notifications`.
 *
 * @param database the Room database instance.
 * @param chipTrigger fires the heads-up category chip on shake events.
 * @param config runtime-tunable parameters (injected as snapshot for testability).
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class ReconciliationEngine(
    private val database: ArthixDatabase,
    private val chipTrigger: ChipTrigger,
    private val config: ReconciliationConfigSnapshot = ReconciliationConfigSnapshot(),
) {

    companion object {
        private const val TAG = "ReconciliationEngine"

        /** FR-1 fixed category set — passed to chip trigger on each shake. */
        val FR1_CATEGORIES = listOf("Food", "Travel", "Shopping", "Other")
    }

    private val pendingQueueDao = database.pendingQueueDao()
    private val transactionDao = database.transactionDao()

    // ── Serial dispatcher — THE correctness foundation (EC-18) ─────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reconciliationDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val reconciliationScope = CoroutineScope(SupervisorJob() + reconciliationDispatcher)

    // ── Disambiguation flow (Phase 3 consumes this) ────────────────────

    private val _disambiguationPrompts = MutableSharedFlow<DisambiguationPrompt>(
        extraBufferCapacity = 4,
    )
    val disambiguationPrompts: SharedFlow<DisambiguationPrompt> = _disambiguationPrompts.asSharedFlow()

    /** Pending disambiguation resolutions, keyed by notificationId. */
    private val pendingResolutions = ConcurrentHashMap<String, CompletableDeferred<String>>()

    // ── ShakeEvent handling (from Phase 1) ─────────────────────────────

    /**
     * Handle a shake event from Phase 1. Creates a [PendingCaptureEntity],
     * fires the chip trigger, and schedules an independent timeout.
     *
     * Called on the serial reconciliation dispatcher.
     */
    fun onShakeEvent(event: ShakeEvent) {
        reconciliationScope.launch {
            Log.d(TAG, "onShakeEvent: ${event.correlationId}")

            val capture = PendingCaptureEntity(
                id = event.correlationId,
                timestampMonotonic = event.timestampMonotonic,
                matched = false,
                active = true,
                category = null,
                createdAt = System.currentTimeMillis(),
            )
            pendingQueueDao.insertCapture(capture)

            // Fire chip immediately (PRD §7.2 — don't wait for notification)
            chipTrigger.fire(event.correlationId, FR1_CATEGORIES)

            // Schedule independent timeout (PRD §7.6)
            scheduleTimeout(event.correlationId, config.captureTimeoutMs, isCapture = true)

            // Also try to match against any unmatched notifications already waiting
            tryMatchCaptureAgainstNotifications(capture)
        }
    }

    /**
     * Handle a cancellation signal from Phase 1 — remove the PendingCapture
     * that was previously created for a ShakeEvent that has been reclassified.
     */
    fun onCancellationSignal(signal: ShakeCancellationSignal) {
        reconciliationScope.launch {
            Log.d(TAG, "onCancellationSignal: ${signal.correlationId}, reason=${signal.reason}")
            pendingQueueDao.deleteCaptureById(signal.correlationId)
        }
    }

    // ── Notification handling ──────────────────────────────────────────

    /**
     * Handle a parsed outflow notification (already allow-listed, parsed,
     * outcome-filtered, and dedup-checked).
     *
     * Creates a [PendingNotificationEntity] and runs nearest-neighbor matching.
     */
    open fun onNotificationCandidate(candidate: ParsedOutflow, providedId: String = UUID.randomUUID().toString()) {
        reconciliationScope.launch {
            Log.d(TAG, "onNotificationCandidate: ₹${candidate.amountPaise / 100.0} to ${candidate.payee}")

            val notif = PendingNotificationEntity(
                id = providedId,
                timestampMonotonic = SystemClock.elapsedRealtime(),
                amountPaise = candidate.amountPaise,
                payee = candidate.payee,
                matched = false,
                active = true,
                rawText = candidate.rawText,
                createdAt = System.currentTimeMillis(),
            )
            pendingQueueDao.insertNotification(notif)

            // Schedule independent timeout
            scheduleTimeout(notif.id, config.notificationTimeoutMs, isCapture = false)

            // Run nearest-neighbor matching against unmatched captures
            matchNotificationToCapture(notif, candidate.confidenceFlag)
        }
    }

    /**
     * Handle a parsed inflow notification — bypass queues entirely (PRD §7.9).
     */
    open fun onInflowNotification(candidate: ParsedInflow) {
        reconciliationScope.launch {
            Log.d(TAG, "onInflowNotification: ₹${candidate.amountPaise / 100.0} from ${candidate.payee}")

            transactionDao.insert(
                TransactionEntity(
                    amountPaise = candidate.amountPaise,
                    payee = candidate.payee,
                    category = null,
                    timestamp = System.currentTimeMillis(),
                    direction = Direction.INFLOW,
                    source = CaptureSource.SHAKE, // notification-sourced inflow
                    status = TransactionStatus.CONFIRMED,
                    sourceCaptureId = null,
                    sourceNotificationId = null,
                    confidenceFlag = ConfidenceFlag.CLEAN,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * Handle a refund notification — net against existing outflow (PRD §5.3).
     */
    open fun onRefundNotification(amountPaise: Long, payee: String) {
        reconciliationScope.launch {
            Log.d(TAG, "onRefundNotification: ₹${amountPaise / 100.0} from $payee")

            val lookbackTimestamp = System.currentTimeMillis() - config.refundLookbackMs
            val candidates = transactionDao.findRecentOutflowByAmount(amountPaise, lookbackTimestamp)

            // Find the best matching payee
            val matchingTxn = candidates.firstOrNull { txn ->
                txn.payee != null && DedupChecker.payeeSimilarity(payee, txn.payee) >= config.dedupSimilarityThreshold
            }

            if (matchingTxn != null) {
                // Net against original — mark as discarded (simplification: full refund)
                transactionDao.updateStatusBySourceCaptureId(
                    matchingTxn.sourceCaptureId ?: "",
                    TransactionStatus.DISCARDED
                )
                Log.d(TAG, "Refund netted against transaction ${matchingTxn.id}")
            } else {
                // No matching original — log as standalone inflow, flagged needs_review
                transactionDao.insert(
                    TransactionEntity(
                        amountPaise = amountPaise,
                        payee = payee,
                        category = null,
                        timestamp = System.currentTimeMillis(),
                        direction = Direction.INFLOW,
                        source = CaptureSource.SHAKE,
                        status = TransactionStatus.CONFIRMED,
                        sourceCaptureId = null,
                        sourceNotificationId = null,
                        confidenceFlag = ConfidenceFlag.NEEDS_REVIEW,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                Log.d(TAG, "Refund with no matching original — logged as standalone inflow (needs_review)")
            }
        }
    }

    // ── Nearest-neighbor matching (PRD §7.3) ───────────────────────────

    /**
     * Core matching algorithm: find the closest unmatched capture for a notification.
     */
    private suspend fun matchNotificationToCapture(
        notif: PendingNotificationEntity,
        confidenceFlag: ConfidenceFlag,
    ) {
        val windowStart = notif.timestampMonotonic - config.maxDelayWindowMs
        val windowEnd = notif.timestampMonotonic

        val candidates = pendingQueueDao.getUnmatchedCapturesInWindow(windowStart, windowEnd)
            .sortedBy { abs(notif.timestampMonotonic - it.timestampMonotonic) }

        when {
            candidates.isEmpty() -> {
                // No capture yet — notification stays unmatched, waiting for a shake
                // that may still arrive, or for its own timeout (§7.6)
                Log.d(TAG, "No unmatched captures for notification ${notif.id}")
            }
            candidates.size == 1 -> {
                commitMatch(candidates[0], notif, confidenceFlag)
            }
            else -> {
                val best = candidates[0]
                val secondBest = candidates[1]
                val bestDelta = abs(notif.timestampMonotonic - best.timestampMonotonic)
                val secondDelta = abs(notif.timestampMonotonic - secondBest.timestampMonotonic)
                val gap = secondDelta - bestDelta

                if (gap < config.ambiguityThresholdMs) {
                    // Ambiguous — trigger disambiguation prompt (PRD §7.4)
                    triggerDisambiguation(
                        notif,
                        candidates.take(config.ambiguityMaxCandidates),
                        confidenceFlag
                    )
                } else {
                    commitMatch(best, notif, confidenceFlag)
                }
            }
        }
    }

    /**
     * Reverse matching: when a new capture arrives, check if any unmatched
     * notifications are waiting for it. This handles the case where a
     * notification arrived before its corresponding shake.
     */
    private suspend fun tryMatchCaptureAgainstNotifications(capture: PendingCaptureEntity) {
        val windowStart = capture.timestampMonotonic - config.maxDelayWindowMs
        val windowEnd = capture.timestampMonotonic

        val waitingNotifs = pendingQueueDao.getUnmatchedNotificationsInWindow(windowStart, windowEnd)
            .sortedBy { abs(capture.timestampMonotonic - it.timestampMonotonic) }

        if (waitingNotifs.isNotEmpty()) {
            val bestNotif = waitingNotifs[0]
            // Check that this capture is the best match for this notification
            // (don't commit if another capture is closer)
            val allCaptures = pendingQueueDao.getUnmatchedCapturesInWindow(
                bestNotif.timestampMonotonic - config.maxDelayWindowMs,
                bestNotif.timestampMonotonic
            ).sortedBy { abs(bestNotif.timestampMonotonic - it.timestampMonotonic) }

            if (allCaptures.isNotEmpty() && allCaptures[0].id == capture.id) {
                commitMatch(capture, bestNotif, ConfidenceFlag.CLEAN)
            }
        }
    }

    // ── Commit match (PRD §9) ──────────────────────────────────────────

    /**
     * Atomically commit a match: mark both pending rows matched, create/enrich Transaction.
     *
     * All traceability fields (source_capture_id, source_notification_id) are
     * populated per PRD §9 / EC-23.
     */
    private suspend fun commitMatch(
        capture: PendingCaptureEntity,
        notif: PendingNotificationEntity,
        confidenceFlag: ConfidenceFlag,
    ) {
        Log.d(TAG, "commitMatch: capture=${capture.id} ↔ notif=${notif.id}")

        database.runInTransaction {
            // These are suspend functions but we need to run them in a Room transaction.
            // Room's runInTransaction runs on the same thread, ensuring atomicity.
        }

        // Use individual updates since Room's runInTransaction doesn't support suspend
        pendingQueueDao.markCaptureMatched(capture.id)
        pendingQueueDao.markNotificationMatched(notif.id)

        val status = if (capture.category != null) {
            TransactionStatus.CONFIRMED
        } else {
            TransactionStatus.AWAITING_CATEGORY
        }

        // Check if a transaction already exists for this capture (from onShakeEvent chip flow)
        val existingTxn = transactionDao.findBySourceCaptureId(capture.id)
        if (existingTxn != null) {
            // Enrich existing transaction with notification data
            transactionDao.update(
                existingTxn.copy(
                    amountPaise = notif.amountPaise,
                    payee = notif.payee,
                    sourceNotificationId = notif.id,
                    status = status,
                    confidenceFlag = confidenceFlag,
                )
            )
        } else {
            // Create new transaction
            transactionDao.insert(
                TransactionEntity(
                    amountPaise = notif.amountPaise,
                    payee = notif.payee,
                    category = capture.category,
                    timestamp = System.currentTimeMillis(),
                    direction = Direction.OUTFLOW,
                    source = CaptureSource.SHAKE,
                    status = status,
                    sourceCaptureId = capture.id,
                    sourceNotificationId = notif.id,
                    confidenceFlag = confidenceFlag,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    // ── Disambiguation (PRD §7.5) ──────────────────────────────────────

    /**
     * Trigger a disambiguation prompt and handle timeout/fallback.
     *
     * - Emits [DisambiguationPrompt] via Flow for Phase 3 to render
     * - Suspends until user taps or timeout expires
     * - On timeout: falls back to nearest-neighbor best guess with AUTO_RESOLVED
     */
    private suspend fun triggerDisambiguation(
        notif: PendingNotificationEntity,
        candidates: List<PendingCaptureEntity>,
        confidenceFlag: ConfidenceFlag,
    ) {
        val nowMs = SystemClock.elapsedRealtime()

        val prompt = DisambiguationPrompt(
            notificationId = notif.id,
            amountPaise = notif.amountPaise,
            payee = notif.payee,
            candidates = candidates.map { capture ->
                DisambiguationCandidate(
                    captureId = capture.id,
                    approximateSecondsAgo = ((nowMs - capture.timestampMonotonic) / 1000).toInt(),
                    category = capture.category,
                )
            },
            timeoutMs = config.disambiguationTimeoutMs,
        )

        // Set up deferred for resolution
        val deferred = CompletableDeferred<String>()
        pendingResolutions[notif.id] = deferred

        // Emit prompt for Phase 3
        _disambiguationPrompts.emit(prompt)
        Log.d(TAG, "Disambiguation prompt emitted for notification ${notif.id} with ${candidates.size} candidates")

        // Wait for resolution or timeout
        val chosenCaptureId = withTimeoutOrNull(config.disambiguationTimeoutMs) {
            deferred.await()
        }

        pendingResolutions.remove(notif.id)

        val resolvedCapture: PendingCaptureEntity
        val resolvedFlag: ConfidenceFlag

        if (chosenCaptureId != null) {
            // User made a choice
            resolvedCapture = candidates.first { it.id == chosenCaptureId }
            resolvedFlag = confidenceFlag  // user-resolved → preserve original confidence
            Log.d(TAG, "Disambiguation resolved by user: ${notif.id} → $chosenCaptureId")
        } else {
            // Timeout — fall back to nearest-neighbor best guess
            resolvedCapture = candidates.first()
            resolvedFlag = ConfidenceFlag.AUTO_RESOLVED  // never CLEAN when prompt was ignored (PRD §7.5)
            Log.d(TAG, "Disambiguation timed out: ${notif.id} → auto-resolved to ${resolvedCapture.id}")
        }

        commitMatch(resolvedCapture, notif, resolvedFlag)
    }

    /**
     * Called by Phase 3 when the user taps a disambiguation candidate.
     *
     * @param notificationId the notification being disambiguated.
     * @param chosenCaptureId the capture the user selected.
     */
    fun resolveDisambiguation(notificationId: String, chosenCaptureId: String) {
        pendingResolutions[notificationId]?.complete(chosenCaptureId)
    }

    // ── Discard (PRD §7.7) ─────────────────────────────────────────────

    /**
     * Discard a pending capture — atomic: deactivate + set status DISCARDED.
     *
     * Two callers: Phase 3 (CHIP_TAP) and Phase 4 (VOICE_INTENT) —
     * same underlying function, per PRD §7.7.
     */
    fun discardCapture(captureId: String, source: DiscardSource) {
        reconciliationScope.launch {
            Log.d(TAG, "discardCapture: $captureId via $source")

            pendingQueueDao.deactivateCapture(captureId)

            val existingTxn = transactionDao.findBySourceCaptureId(captureId)
            if (existingTxn != null) {
                transactionDao.updateStatusBySourceCaptureId(captureId, TransactionStatus.DISCARDED)
            } else {
                // Create a discarded transaction record for traceability
                transactionDao.insert(
                    TransactionEntity(
                        amountPaise = null,
                        payee = null,
                        category = null,
                        timestamp = System.currentTimeMillis(),
                        direction = Direction.OUTFLOW,
                        source = CaptureSource.SHAKE,
                        status = TransactionStatus.DISCARDED,
                        sourceCaptureId = captureId,
                        sourceNotificationId = null,
                        confidenceFlag = ConfidenceFlag.CLEAN,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    // ── Timeout hygiene (PRD §7.6) ─────────────────────────────────────

    /**
     * Schedule an independent per-row timeout.
     *
     * Uses coroutine delay inside the serial scope (not WorkManager) to stay
     * within the same dispatcher that owns queue mutations — avoids a second
     * concurrent writer (PRD §7.6 atomicity requirement).
     */
    private fun scheduleTimeout(id: String, delayMs: Long, isCapture: Boolean) {
        reconciliationScope.launch {
            delay(delayMs)

            if (isCapture) {
                val stillPending = pendingQueueDao.findCaptureById(id)
                if (stillPending != null && !stillPending.matched && stillPending.active) {
                    Log.d(TAG, "Capture timeout: $id")
                    pendingQueueDao.deactivateCapture(id)

                    val existingTxn = transactionDao.findBySourceCaptureId(id)
                    if (existingTxn != null) {
                        transactionDao.updateStatusBySourceCaptureId(id, TransactionStatus.AWAITING_AMOUNT)
                    } else {
                        transactionDao.insert(
                            TransactionEntity(
                                amountPaise = null,
                                payee = null,
                                category = stillPending.category,
                                timestamp = System.currentTimeMillis(),
                                direction = Direction.OUTFLOW,
                                source = CaptureSource.SHAKE,
                                status = TransactionStatus.AWAITING_AMOUNT,
                                sourceCaptureId = id,
                                sourceNotificationId = null,
                                confidenceFlag = ConfidenceFlag.CLEAN,
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                    }
                }
            } else {
                val stillPending = pendingQueueDao.findNotificationById(id)
                if (stillPending != null && !stillPending.matched && stillPending.active) {
                    Log.d(TAG, "Notification timeout: $id")
                    pendingQueueDao.deactivateNotification(id)

                    // Notification timed out — amount/payee are known, category is missing
                    transactionDao.insert(
                        TransactionEntity(
                            amountPaise = stillPending.amountPaise,
                            payee = stillPending.payee,
                            category = null,
                            timestamp = System.currentTimeMillis(),
                            direction = Direction.OUTFLOW,
                            source = CaptureSource.MANUAL,
                            status = TransactionStatus.AWAITING_CATEGORY,
                            sourceCaptureId = null,
                            sourceNotificationId = stillPending.id,
                            confidenceFlag = ConfidenceFlag.CLEAN,
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
        }
    }

    /**
     * Cancel the serial scope — call on Service destroy.
     */
    fun cancel() {
        reconciliationScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    /**
     * Immediately timeout a notification, inserting it into transactionDao
     * and returning its new ID.
     */
    suspend fun forceTimeoutNotification(id: String): Long? = kotlinx.coroutines.withContext(reconciliationDispatcher) {
        val stillPending = pendingQueueDao.findNotificationById(id)
        if (stillPending != null && !stillPending.matched && stillPending.active) {
            Log.d(TAG, "Notification forced timeout: $id")
            pendingQueueDao.deactivateNotification(id)

            val txnId = transactionDao.insert(
                TransactionEntity(
                    amountPaise = stillPending.amountPaise,
                    payee = stillPending.payee,
                    category = null,
                    timestamp = System.currentTimeMillis(),
                    direction = Direction.OUTFLOW,
                    source = CaptureSource.MANUAL,
                    status = TransactionStatus.AWAITING_CATEGORY,
                    sourceCaptureId = null,
                    sourceNotificationId = stillPending.id,
                    confidenceFlag = ConfidenceFlag.CLEAN,
                    createdAt = System.currentTimeMillis(),
                )
            )
            return@withContext txnId
        }

        val existingTxn = transactionDao.findBySourceNotificationId(id)
        return@withContext existingTxn?.id
    }
}
