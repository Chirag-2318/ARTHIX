package com.chirag.arthix.voice

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing voice utilities and audio system services.
 */
@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    private const val TAG = "VoiceModule"
    // Whisper model is lazily loaded inside WhisperSttEngine

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

