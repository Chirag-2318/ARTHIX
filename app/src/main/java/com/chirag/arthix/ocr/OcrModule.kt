package com.chirag.arthix.ocr

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the ML Kit [TextRecognizer] singleton.
 *
 * Uses the Latin-script bundled model ([TextRecognizerOptions.DEFAULT_OPTIONS]).
 * "Bundled" means the model ships inside the APK — no Play Services dependency,
 * no network download, fully offline (satisfies NFR-1).
 *
 * Scoped @Singleton because [TextRecognizer] is thread-safe and heavyweight to
 * create; one instance per process lifetime is correct.
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer {
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
}
