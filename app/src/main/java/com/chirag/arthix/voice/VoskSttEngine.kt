package com.chirag.arthix.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Speech-to-text engine powered by OpenAI Whisper (tiny.en quantized int8 ONNX)
 * with automatic fallback to Android's platform [SpeechRecognizer].
 */
@Singleton
class WhisperSttEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var recognizer: OfflineRecognizer? = null
    private var isModelInitialized = false
    private val modelMutex = Mutex()

    companion object {
        private const val TAG = "WhisperSttEngine"

        const val CONFIDENCE_THRESHOLD = 0.3f

        private const val SAMPLE_RATE = 16000
        private const val RECORD_TIMEOUT_MS = 8_000L  // 8s max per utterance
        private const val BUFFER_SIZE_FACTOR = 4

        private const val SILENCE_THRESHOLD = 800
        private const val MAX_SILENCE_FRAMES = 40
        private const val MIN_RECORD_FRAMES = 8

        private const val MODEL_ZIP = "whisper-tiny-model.zip"
        private const val MODEL_DIR_NAME = "whisper-tiny-en"
    }

    private fun extractModelFromAssets(context: Context): File? {
        val targetDir = File(context.filesDir, MODEL_DIR_NAME)
        val markerFile = File(targetDir, ".extracted")

        if (markerFile.exists() && targetDir.isDirectory) {
            val encoder = File(targetDir, "tiny.en-encoder.int8.onnx")
            val decoder = File(targetDir, "tiny.en-decoder.int8.onnx")
            val tokens = File(targetDir, "tiny.en-tokens.txt")
            if (encoder.exists() && decoder.exists() && tokens.exists()) {
                Log.d(TAG, "Whisper model already extracted at ${targetDir.absolutePath}")
                return targetDir
            }
        }

        return try {
            Log.d(TAG, "Extracting Whisper model from assets $MODEL_ZIP...")
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            val assetStream = context.assets.open(MODEL_ZIP)
            ZipInputStream(assetStream).use { zis ->
                val buffer = ByteArray(8192)
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.removePrefix("$MODEL_DIR_NAME/").removePrefix("/")
                    if (entryName.isNotEmpty()) {
                        val outFile = File(targetDir, entryName)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            markerFile.createNewFile()
            Log.d(TAG, "Whisper model successfully extracted to ${targetDir.absolutePath}")
            targetDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract Whisper model from assets", e)
            null
        }
    }

    private suspend fun getRecognizerLazily(): OfflineRecognizer? = modelMutex.withLock {
        if (isModelInitialized) return recognizer

        recognizer = withContext(Dispatchers.IO) {
            try {
                val modelDir = extractModelFromAssets(context)
                if (modelDir != null && modelDir.exists()) {
                    Log.d(TAG, "Initializing Whisper OfflineRecognizer...")
                    val config = OfflineRecognizerConfig().apply {
                        featConfig.sampleRate = SAMPLE_RATE
                        featConfig.featureDim = 80
                        modelConfig.whisper.encoder = File(modelDir, "tiny.en-encoder.int8.onnx").absolutePath
                        modelConfig.whisper.decoder = File(modelDir, "tiny.en-decoder.int8.onnx").absolutePath
                        modelConfig.whisper.language = "en"
                        modelConfig.whisper.task = "transcribe"
                        modelConfig.tokens = File(modelDir, "tiny.en-tokens.txt").absolutePath
                        modelConfig.numThreads = 2
                        modelConfig.debug = false
                        modelConfig.provider = "cpu"
                        modelConfig.modelType = "whisper"
                        decodingMethod = "greedy_search"
                    }
                    OfflineRecognizer(assetManager = null, config = config)
                } else {
                    Log.w(TAG, "Whisper model directory not found")
                    null
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Exception initializing Whisper OfflineRecognizer", e)
                null
            }
        }
        isModelInitialized = true
        return recognizer
    }

    fun isModelReady(): Boolean {
        return (isModelInitialized && recognizer != null) || SpeechRecognizer.isRecognitionAvailable(context)
    }

    suspend fun warmUp(): Boolean {
        val loaded = getRecognizerLazily() != null
        if (loaded) return true
        return withContext(Dispatchers.Main) {
            SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    suspend fun recognize(): SttResult {
        // Try Android's hardware-accelerated platform SpeechRecognizer first for instant response
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val systemResult = recognizeWithSystemSpeechRecognizer()
            if (systemResult is SttResult.Recognized) {
                return systemResult
            }
            Log.d(TAG, "System SpeechRecognizer yielded $systemResult. Falling back to on-device Whisper.")
        }

        // Fallback to on-device OpenAI Whisper model
        val currentRecognizer = getRecognizerLazily()
        if (currentRecognizer != null) {
            val whisperResult = recognizeWithWhisper(currentRecognizer)
            if (whisperResult is SttResult.Recognized || whisperResult is SttResult.LowConfidence || whisperResult is SttResult.Timeout) {
                return whisperResult
            }
            Log.w(TAG, "Whisper returned error: $whisperResult")
        }

        return SttResult.Timeout
    }

    private suspend fun recognizeWithWhisper(offlineRecognizer: OfflineRecognizer): SttResult = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ) * BUFFER_SIZE_FACTOR

        if (bufferSize <= 0) {
            return@withContext SttResult.Error("Invalid buffer size for audio recording")
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
            return@withContext SttResult.Error("RECORD_AUDIO permission not granted")
        } catch (e: Throwable) {
            return@withContext SttResult.Error("AudioRecord init failed: ${e.message}")
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return@withContext SttResult.Error("AudioRecord failed to initialize")
        }

        return@withContext try {
            recordAndTranscribe(audioRecord, offlineRecognizer, bufferSize)
        } catch (e: Throwable) {
            Log.e(TAG, "Exception in Whisper recording loop", e)
            SttResult.Error("Whisper error: ${e.message}")
        } finally {
            try {
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                    audioRecord.release()
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun recordAndTranscribe(
        audioRecord: AudioRecord,
        offlineRecognizer: OfflineRecognizer,
        bufferSize: Int,
    ): SttResult {
        audioRecord.startRecording()
        val shortBuffer = ShortArray(bufferSize / 2)
        val audioSamples = ArrayList<Float>()
        var silenceFrames = 0
        var totalFrames = 0

        while (true) {
            val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
            if (read <= 0) break

            totalFrames++

            var sumSquares = 0.0
            for (i in 0 until read) {
                val sample = shortBuffer[i].toDouble()
                sumSquares += sample * sample
                audioSamples.add(shortBuffer[i] / 32768.0f)
            }

            val rms = Math.sqrt(sumSquares / read)
            val isSilent = rms < 350.0

            if (isSilent && totalFrames >= MIN_RECORD_FRAMES) {
                silenceFrames++
            } else {
                silenceFrames = 0
            }

            // Stop recording after ~1.2s of continuous silence (15 frames) or 6 seconds max (75 frames)
            if (silenceFrames >= 15 || totalFrames >= 75) {
                break
            }
        }

        if (audioSamples.isEmpty() || totalFrames < MIN_RECORD_FRAMES) {
            return SttResult.Timeout
        }

        val stream = offlineRecognizer.createStream() ?: return SttResult.Error("Failed to create Whisper stream")
        try {
            stream.acceptWaveform(audioSamples.toFloatArray(), SAMPLE_RATE)
            offlineRecognizer.decode(stream)
            val result = offlineRecognizer.getResult(stream)
            val text = result.text.trim().lowercase()
            Log.d(TAG, "Whisper result: '$text'")

            if (text.isEmpty()) {
                return SttResult.Timeout
            }

            return SttResult.Recognized(text, 0.95f)
        } finally {
            try {
                stream.release()
            } catch (ignored: Exception) {}
        }
    }

    private suspend fun recognizeWithSystemSpeechRecognizer(): SttResult = withContext(Dispatchers.Main) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return@withContext SttResult.Error("Speech recognition not available on this device")
        }

        suspendCancellableCoroutine<SttResult> { cont ->
            val recognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
                if (cont.isActive) cont.resume(SttResult.Error("SpeechRecognizer create failed: ${e.message}"))
                return@suspendCancellableCoroutine
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            var resumed = false
            fun safeResume(result: SttResult) {
                if (!resumed && cont.isActive) {
                    resumed = true
                    cont.resume(result)
                }
                try {
                    recognizer.destroy()
                } catch (ignored: Exception) {}
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()?.lowercase().orEmpty()
                    Log.d(TAG, "System SpeechRecognizer result: '$text'")
                    if (text.isNotEmpty()) {
                        safeResume(SttResult.Recognized(text, 0.95f))
                    } else {
                        safeResume(SttResult.Timeout)
                    }
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "System SpeechRecognizer error code: $error")
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> safeResume(SttResult.Timeout)
                        else -> safeResume(SttResult.Error("Speech recognition error ($error)"))
                    }
                }
            })

            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start system speech recognizer", e)
                safeResume(SttResult.Error("Failed to start speech recognizer: ${e.message}"))
            }

            cont.invokeOnCancellation {
                try {
                    recognizer.cancel()
                    recognizer.destroy()
                } catch (ignored: Exception) {}
            }
        }
    }
}

typealias VoskSttEngine = WhisperSttEngine
typealias SherpaSttEngine = WhisperSttEngine


