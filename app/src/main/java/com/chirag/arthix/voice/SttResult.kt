package com.chirag.arthix.voice

/**
 * Result type produced by [WhisperSttEngine] for each recognition attempt.
 *
 * Sealed so every call site handles all outcomes — no silent drop.
 */
sealed class SttResult {

    /**
     * Speech was recognized with confidence at or above the threshold.
     *
     * @param text normalized, lower-cased transcript.
     * @param confidence Whisper's per-utterance confidence (0.0–1.0).
     */
    data class Recognized(val text: String, val confidence: Float) : SttResult()

    /**
     * Speech was recognized but confidence fell below [WhisperSttEngine.CONFIDENCE_THRESHOLD].
     * Caller should re-prompt once before routing to manual fallback (EC-27).
     *
     * @param text the low-confidence transcript (for debug / display).
     * @param confidence the actual confidence value.
     */
    data class LowConfidence(val text: String, val confidence: Float) : SttResult()

    /** No audio was received within the recording window. */
    object Timeout : SttResult()

    /** Audio recording or Whisper processing failed unexpectedly. */
    data class Error(val cause: String) : SttResult()
}
