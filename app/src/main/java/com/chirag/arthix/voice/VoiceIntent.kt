package com.chirag.arthix.voice

/**
 * Structured intent parsed from a voice utterance by [VoiceIntentParser].
 *
 * Each subtype is mutually exclusive — the parser picks the FIRST matching
 * intent in priority order: Discard > Amount > Category > CategoryAndAmount > Split > Unclear.
 */
sealed class VoiceIntent {

    /**
     * User wants to discard the pending record (EC-25).
     * Triggered by: "skip", "not real", "ignore", "cancel", "discard", "nope", "none", "no"
     */
    object Discard : VoiceIntent()

    /**
     * User stated a monetary amount (for AWAITING_AMOUNT records, EC-24).
     *
     * @param amountPaise resolved amount in integer paise.
     */
    data class Amount(val amountPaise: Long) : VoiceIntent()

    /**
     * User stated a category (for AWAITING_CATEGORY records).
     *
     * @param category normalized to the fixed taxonomy: Food / Travel / Shopping / Other.
     * @param originalPhrase the raw spoken phrase, stored as a sub-tag (EC-28).
     */
    data class Category(val category: String, val originalPhrase: String) : VoiceIntent()

    /**
     * User stated both an amount and a category in one utterance
     * (e.g. "four fifty for food", "shopping two hundred").
     */
    data class CategoryAndAmount(
        val category: String,
        val amountPaise: Long,
        val originalPhrase: String,
    ) : VoiceIntent()

    /**
     * User wants to split this transaction (FR-6 voice alt-flow contract).
     *
     * @param names raw spoken name candidates — Phase 6 resolves against contacts
     *   and handles ambiguity (EC-36: multiple candidates → Phase 6 tap-to-pick UI).
     */
    data class Split(val names: List<String>) : VoiceIntent()

    /** No recognizable intent — triggers re-prompt or manual fallback. */
    object Unclear : VoiceIntent()
}
