package com.chirag.arthix.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Vosk's [Recognizer] in a suspend function suitable for use from coroutines.
 *
 * ## Confidence threshold (EC-27)
 * If Vosk's final-result JSON has minimum word `conf` < [CONFIDENCE_THRESHOLD], we return
 * [SttResult.LowConfidence] so the caller can re-prompt once before routing
 * to manual fallback.
 *
 * ## Audio format
 * Vosk requires 16-bit PCM, mono, 16000 Hz. AudioRecord reads into ShortArray;
 * this class converts to little-endian ByteArray before feeding the recognizer.
 *
 * ## Language scope (EC-29)
 * Model: vosk-model-small-en-in-0.4 (Indian English, ~37MB, bundled in assets).
 * Hindi-only utterances will be poorly recognized — stated limitation.
 */
@Singleton
class VoskSttEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var model: Model? = null
    private var isModelInitialized = false
    private val modelMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getModelLazily(): Model? = modelMutex.withLock {
        if (isModelInitialized) return model

        model = withContext(Dispatchers.IO) {
            try {
                suspendCancellableCoroutine { cont ->
                    StorageService.unpack(
                        context,
                        "vosk-model-small-en-in-0.4",
                        "model",
                        { result: Model ->
                            if (cont.isActive) cont.resume(result) { result.close() }
                        },
                        { e: Exception ->
                            Log.e(TAG, "Failed to unpack Vosk model from assets", e)
                            if (cont.isActive) cont.resume(null) {}
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Vosk model unpack", e)
                null
            }
        }
        isModelInitialized = true
        return model
    }

    companion object {
        private const val TAG = "VoskSttEngine"

        /** Confidence below this → [SttResult.LowConfidence] (EC-27). Starting value = 0.5. */
        const val CONFIDENCE_THRESHOLD = 0.5f

        private const val SAMPLE_RATE = 16000
        private const val RECORD_TIMEOUT_MS = 8_000L  // 8s max per utterance
        private const val BUFFER_SIZE_FACTOR = 4
    }

    /**
     * Records a single utterance and returns its recognition result.
     *
     * Runs on [Dispatchers.IO]. Safe to call from any coroutine.
     * Requires RECORD_AUDIO permission to be granted before calling.
     *
     * @return [SttResult]
     */
    suspend fun recognize(): SttResult = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ) * BUFFER_SIZE_FACTOR

        val currentModel = getModelLazily()
            ?: return@withContext SttResult.Error("Vosk model unavailable — check assets")

        val recognizer = try {
            Recognizer(currentModel, SAMPLE_RATE.toFloat()).also {
                // Bug fix #2: Enable per-word confidence metadata in the result JSON (EC-27).
                // Without this call, the JSON only has {"text": "..."} — no "conf" fields at all.
                it.setWords(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Vosk Recognizer", e)
            return@withContext SttResult.Error("Recognizer init failed: ${e.message}")
        }

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (e: SecurityException) {
            recognizer.close()
            return@withContext SttResult.Error("RECORD_AUDIO permission not granted")
        } catch (e: Exception) {
            recognizer.close()
            return@withContext SttResult.Error("AudioRecord init failed: ${e.message}")
        }

        return@withContext try {
            val result = withTimeoutOrNull(RECORD_TIMEOUT_MS) {
                recognizeWithRecord(audioRecord, recognizer, bufferSize)
            } ?: SttResult.Timeout

            result
        } finally {
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
                audioRecord.release()
            }
            recognizer.close()
        }
    }

    private fun recognizeWithRecord(
        audioRecord: AudioRecord,
        recognizer: Recognizer,
        bufferSize: Int,
    ): SttResult {
        audioRecord.startRecording()

        // Bug fix #1: AudioRecord.read() fills a ShortArray (one short = one 16-bit PCM sample).
        // Vosk's acceptWaveForm() only accepts ByteArray (little-endian PCM bytes).
        // We allocate both arrays once and reuse them per read to avoid GC pressure.
        val shortBuffer = ShortArray(bufferSize / 2)
        val byteBuffer = ByteArray(bufferSize)          // shortBuffer.size * 2

        var silenceFrames = 0
        val maxSilenceFrames = 20  // ~2.5s of silence ends recording

        while (true) {
            val samplesRead = audioRecord.read(shortBuffer, 0, shortBuffer.size)
            if (samplesRead <= 0) break

            // Silence detection on the raw shorts (before byte conversion)
            val isSilent = shortBuffer.take(samplesRead).all { it.toInt() in -300..300 }
            if (isSilent) silenceFrames++ else silenceFrames = 0
            if (silenceFrames >= maxSilenceFrames) break

            // Convert ShortArray → little-endian ByteArray for Vosk
            val bytesProduced = samplesRead * 2
            ByteBuffer.wrap(byteBuffer)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .put(shortBuffer, 0, samplesRead)

            if (recognizer.acceptWaveForm(byteBuffer, bytesProduced)) {
                // Final result available — parse it
                return parseResult(recognizer.result)
            }
        }

        // End of audio — flush any buffered audio and get the final result
        return parseResult(recognizer.finalResult)
    }

    /**
     * Parses a Vosk JSON result string into an [SttResult].
     *
     * ## Confidence extraction (Bug fix #3)
     * When `setWords(true)` is enabled, Vosk embeds per-word confidence in a `result` array:
     * ```json
     * {
     *   "result": [
     *     {"conf": 0.95, "start": 0.2, "end": 0.5, "word": "food"},
     *     {"conf": 0.87, "start": 0.6, "end": 1.0, "word": "fifty"}
     *   ],
     *   "text": "food fifty"
     * }
     * ```
     * We use the **minimum** word confidence (most conservative) for EC-27 gating.
     * If no `result` array is present (empty/silence), confidence is treated as unknown (-1).
     */
    private fun parseResult(json: String): SttResult {
        return try {
            val obj = JSONObject(json)
            val text = obj.optString("text", "").trim().lowercase()

            Log.d(TAG, "Vosk result JSON: $json")

            if (text.isEmpty()) return SttResult.Timeout

            // Bug fix #3: Confidence lives in obj["result"][i]["conf"] per word, not top-level.
            val confidence: Float = run {
                val resultArray = obj.optJSONArray("result")
                if (resultArray != null && resultArray.length() > 0) {
                    var minConf = Float.MAX_VALUE
                    for (i in 0 until resultArray.length()) {
                        val wordObj = resultArray.optJSONObject(i) ?: continue
                        val wordConf = wordObj.optDouble("conf", -1.0).toFloat()
                        if (wordConf in 0f..1f && wordConf < minConf) {
                            minConf = wordConf
                        }
                    }
                    if (minConf == Float.MAX_VALUE) -1f else minConf
                } else {
                    -1f  // No word-level data — treat confidence as unknown
                }
            }

            Log.d(TAG, "Vosk result: text='$text' minWordConf=$confidence")

            if (confidence in 0f..1f && confidence < CONFIDENCE_THRESHOLD) {
                SttResult.LowConfidence(text, confidence)
            } else {
                SttResult.Recognized(text, confidence.coerceAtLeast(0f))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Vosk result JSON: $json", e)
            SttResult.Error("JSON parse failure")
        }
    }
}
