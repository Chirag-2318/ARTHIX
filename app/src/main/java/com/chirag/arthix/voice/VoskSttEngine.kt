package com.chirag.arthix.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Vosk's [Recognizer] in a suspend function suitable for use from coroutines.
 *
 * ## Confidence threshold (EC-27)
 * If Vosk's final-result JSON has `conf` < [CONFIDENCE_THRESHOLD], we return
 * [SttResult.LowConfidence] so the caller can re-prompt once before routing
 * to manual fallback.
 *
 * ## Audio format
 * Vosk requires 16-bit PCM, mono, 16000 Hz. This is the de-facto standard for
 * on-device ASR — no resampling step needed.
 *
 * ## Language scope (EC-29)
 * Model: vosk-model-small-en-in-0.4 (Indian English, ~37MB, bundled in assets).
 * Hindi-only utterances will be poorly recognized — stated limitation.
 */
@Singleton
class VoskSttEngine @Inject constructor(
    private val model: Model,
) {

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

        val recognizer = try {
            Recognizer(model, SAMPLE_RATE.toFloat())
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
        val buffer = ShortArray(bufferSize / 2)
        var silenceFrames = 0
        val maxSilenceFrames = 20  // ~2.5s of silence ends recording

        while (true) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read <= 0) break

            val isSilent = buffer.take(read).all { it.toInt() in -300..300 }
            if (isSilent) silenceFrames++ else silenceFrames = 0
            if (silenceFrames >= maxSilenceFrames) break

            if (recognizer.acceptWaveForm(buffer, read)) {
                // Final result available — parse it
                return parseResult(recognizer.result)
            }
        }

        // End of audio — get final result
        return parseResult(recognizer.finalResult)
    }

    private fun parseResult(json: String): SttResult {
        return try {
            val obj = JSONObject(json)
            val text = obj.optString("text", "").trim().lowercase()
            val confidence = obj.optDouble("conf", -1.0).toFloat()

            Log.d(TAG, "Vosk result: text='$text' conf=$confidence")

            if (text.isEmpty()) return SttResult.Timeout

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
