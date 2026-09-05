package com.chirag.arthix.voice

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.ui.screen.manual.ManualEntryPrefill
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Orchestrates the multi-turn idle-state voice follow-up (FR-3).
 *
 * ## Turn structure (EC-24)
 * For each pending record (up to [MAX_RECORDS_PER_SESSION] at a time):
 * 1. Speak a prompt (different based on status: AWAITING_AMOUNT vs AWAITING_CATEGORY).
 * 2. Record + recognize via [WhisperSttEngine].
 * 3. Parse intent via [VoiceIntentParser].
 * 4. Confidence gating (EC-27):
 *    - Below threshold → re-prompt once with a clarification ask.
 *    - Still low after retry → produce a [ManualEntryPrefill] for that record.
 * 5. Apply intent to the transaction record:
 *    - Discard → [TransactionRepository.discard] (EC-25)
 *    - Amount / Category / CategoryAndAmount → [TransactionRepository.update]
 *    - Unclear (after retry) → [ManualEntryPrefill] route
 */
class VoiceFollowUpSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sttEngine: WhisperSttEngine,
    private val transactionRepository: TransactionRepository,
) {

    companion object {
        private const val TAG = "VoiceFollowUpSession"

        /** Maximum records resolved per session to avoid fatiguing the user. */
        const val MAX_RECORDS_PER_SESSION = 3

        /** Pause between TTS prompt and recording start. */
        private const val TTS_TO_RECORD_DELAY_MS = 700L
    }

    private var tts: TextToSpeech? = null

    /**
     * Run the full voice follow-up flow.
     *
     * @return list of [ManualEntryPrefill] for records that couldn't be resolved via voice —
     *   the caller (WorkManager) can fire a notification deep-linking into the manual screen.
     */
    suspend fun run(): List<ManualEntryPrefill> {
        val manualFallbacks = mutableListOf<ManualEntryPrefill>()

        initTts()

        val pending = transactionRepository
            .getPendingVoiceRecords(limit = MAX_RECORDS_PER_SESSION)

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending records — nothing to do")
            shutdownTts()
            return emptyList()
        }

        speak("I have ${pending.size} pending transaction${if (pending.size > 1) "s" else ""} to resolve.")
        delay(800)

        for (record in pending) {
            val fallback = resolveRecord(record)
            if (fallback != null) manualFallbacks.add(fallback)
        }

        speak("Done. Thanks!")
        shutdownTts()
        return manualFallbacks
    }

    // ── Per-record resolution ──────────────────────────────────────────────────

    private suspend fun resolveRecord(record: TransactionEntity): ManualEntryPrefill? {
        val prompt = buildPrompt(record)
        speak(prompt)
        delay(TTS_TO_RECORD_DELAY_MS)

        // First attempt
        val result = sttEngine.recognize()
        var intent = processResult(result) ?: return manualFallbackFor(record)

        // Low-confidence re-prompt (EC-27)
        if (intent is VoiceIntent.Unclear) {
            speak("Didn't quite catch that. Please try again.")
            delay(TTS_TO_RECORD_DELAY_MS)
            val retry = sttEngine.recognize()
            intent = processResult(retry) ?: return manualFallbackFor(record)
        }

        return applyIntent(record, intent)
    }

    private fun buildPrompt(record: TransactionEntity): String {
        val payee = if (!record.payee.isNullOrBlank()) "at ${record.payee}" else ""
        return when (record.status) {
            TransactionStatus.AWAITING_AMOUNT ->
                "How much did you spend${if (payee.isNotEmpty()) " $payee" else ""}? Say the amount."
            TransactionStatus.AWAITING_CATEGORY ->
                "What category was this${if (payee.isNotEmpty()) " $payee" else ""} for? " +
                "Food, Travel, Shopping, or Other?"
            else -> "Can you tell me more about this transaction?"
        }
    }

    /** Map SttResult → VoiceIntent, handling low-confidence. Returns null if we should retry. */
    private fun processResult(result: SttResult): VoiceIntent? {
        return when (result) {
            is SttResult.Recognized -> VoiceIntentParser.parse(result.text)
            is SttResult.LowConfidence -> null  // signal: re-prompt
            is SttResult.Timeout, is SttResult.Error -> VoiceIntent.Unclear
        }
    }

    /**
     * Apply a resolved [intent] to [record] in the database.
     * Returns a [ManualEntryPrefill] if the intent couldn't be applied (Unclear).
     */
    private suspend fun applyIntent(
        record: TransactionEntity,
        intent: VoiceIntent,
    ): ManualEntryPrefill? {
        return when (intent) {
            is VoiceIntent.Discard -> {
                Log.d(TAG, "Discarding txn ${record.id} via voice (EC-25)")
                transactionRepository.discard(record.id)
                null
            }
            is VoiceIntent.Amount -> {
                Log.d(TAG, "Setting amount=${intent.amountPaise} on txn ${record.id}")
                transactionRepository.update(
                    record.copy(
                        amountPaise = intent.amountPaise,
                        status = if (record.category != null) TransactionStatus.CONFIRMED
                                 else TransactionStatus.AWAITING_CATEGORY,
                        source = CaptureSource.VOICE,
                        direction = intent.direction ?: record.direction
                    )
                )
                null
            }
            is VoiceIntent.Category -> {
                Log.d(TAG, "Setting category=${intent.category} on txn ${record.id}")
                transactionRepository.update(
                    record.copy(
                        category = intent.category,
                        status = if (record.amountPaise != null) TransactionStatus.CONFIRMED
                                 else TransactionStatus.AWAITING_AMOUNT,
                        source = CaptureSource.VOICE,
                        direction = intent.direction ?: record.direction
                    )
                )
                null
            }
            is VoiceIntent.CategoryAndAmount -> {
                Log.d(TAG, "Setting category+amount on txn ${record.id}")
                transactionRepository.update(
                    record.copy(
                        amountPaise = intent.amountPaise,
                        category = intent.category,
                        payee = intent.payee ?: record.payee,
                        status = TransactionStatus.CONFIRMED,
                        direction = intent.direction ?: record.direction
                    )
                )
                null
            }
            is VoiceIntent.Split -> {
                Log.d(TAG, "Split intent detected ${intent.names} for txn ${record.id}")
                val updated = record.copy(
                    category = intent.category ?: record.category,
                    amountPaise = intent.amountPaise ?: record.amountPaise,
                    direction = intent.direction ?: record.direction,
                )
                manualFallbackFor(updated).copy(splitNames = intent.names)
            }
            is VoiceIntent.Unclear -> {
                Log.d(TAG, "Unclear intent for txn ${record.id} — routing to manual (EC-27)")
                manualFallbackFor(record)
            }
        }
    }

    private fun manualFallbackFor(record: TransactionEntity): ManualEntryPrefill {
        val amountStr = record.amountPaise?.let { p ->
            "%d.%02d".format(p / 100, p % 100)
        }
        return ManualEntryPrefill(
            amount = amountStr,
            payee = record.payee,
            category = record.category,
            sourceTransactionId = record.id,
        )
    }

    // ── TTS helpers ────────────────────────────────────────────────────────────

    private suspend fun initTts() {
        suspendCancellableCoroutine { cont ->
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    cont.resume(Unit)
                } else {
                    Log.w(TAG, "TTS init failed (status=$status) — voice prompts will be silent")
                    cont.resume(Unit)
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun shutdownTts() {
        tts?.shutdown()
        tts = null
    }
}
