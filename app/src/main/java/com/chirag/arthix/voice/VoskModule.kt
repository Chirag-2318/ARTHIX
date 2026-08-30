package com.chirag.arthix.voice

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vosk.Model
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Singleton

/**
 * Hilt module providing the Vosk [Model] singleton.
 *
 * The model zip (vosk-model-small-en-in-0.4.zip, ~37MB) is bundled in
 * `src/main/assets/`. On first run [StorageService.unpack] extracts it
 * to the app's internal files directory — subsequent runs reuse the
 * already-extracted directory.
 *
 * Choosing the small Indian-English model (vosk-model-small-en-in-0.4):
 * - Offline by construction (NFR-1, addresses EC-59's OriginOS dependency risk)
 * - ~37MB APK addition — acceptable for a hackathon device
 * - Exposes per-utterance `conf` scores needed for EC-27 gating
 * - Indian-English accent tuning matches the persona's expected speech
 *
 * @Singleton so the model (JNI resource) is loaded only once per process.
 */
@Module
@InstallIn(SingletonComponent::class)
object VoskModule {

    private const val TAG = "VoskModule"
    private const val MODEL_ASSET = "vosk-model-small-en-in-0.4"

    @Provides
    @Singleton
    fun provideVoskModel(@ApplicationContext context: Context): Model {
        return try {
            val modelPath = StorageService.sync(context, MODEL_ASSET, MODEL_ASSET)
            Model(modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Vosk model from assets", e)
            // Fallback: attempt to load from filesDir if already extracted
            try {
                val fallbackPath = java.io.File(context.getExternalFilesDir(null) ?: context.filesDir, MODEL_ASSET).absolutePath
                Model(fallbackPath)
            } catch (fallbackEx: Exception) {
                throw RuntimeException("Vosk model unavailable — check assets/$MODEL_ASSET.zip", e)
            }
        }
    }

    @Provides
    @Singleton
    fun provideMonotonicClock(): MonotonicClock {
        return object : MonotonicClock {
            override fun elapsedRealtimeMs(): Long = android.os.SystemClock.elapsedRealtime()
        }
    }

    @Provides
    @Singleton
    fun provideAudioSilenceChecker(@ApplicationContext context: Context): AudioSilenceChecker {
        return object : AudioSilenceChecker {
            override fun isSilenced(): Boolean {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    ?: return false
                val ringerMode = audioManager.ringerMode
                return ringerMode == android.media.AudioManager.RINGER_MODE_SILENT ||
                        ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE
            }
        }
    }

    @Provides
    @Singleton
    fun provideScreenInteractivityChecker(@ApplicationContext context: Context): ScreenInteractivityChecker {
        return object : ScreenInteractivityChecker {
            override fun isInteractive(): Boolean {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    ?: return false
                return powerManager.isInteractive
            }
        }
    }
}

