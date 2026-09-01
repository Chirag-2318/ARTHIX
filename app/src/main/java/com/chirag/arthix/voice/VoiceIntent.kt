package com.chirag.arthix.voice

/**
 * Structured intent parsed from a voice utterance by [VoiceIntentParser].
 *
 * Each subtype is mutually exclusive — the parser picks the FIRST matching
 * intent in priority order: Discard > Split > CategoryAndAmount > Amount > Category > Unclear.
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
     * @param payee optional recognized recipient, merchant, or place name.
     */
    data class Amount(
        val amountPaise: Long,
        val payee: String? = null,
    ) : VoiceIntent()

    /**
     * User stated a category (for AWAITING_CATEGORY records).
     *
     * @param category normalized to the fixed taxonomy: Food / Travel / Shopping / Other.
     * @param originalPhrase the raw spoken phrase, stored as a sub-tag (EC-28).
     * @param payee optional recognized recipient, merchant, or place name.
     */
    data class Category(
        val category: String,
        val originalPhrase: String,
        val payee: String? = null,
    ) : VoiceIntent()

    /**
     * User stated both an amount and a category in one utterance
     * (e.g. "four fifty for food", "shopping two hundred").
     *
     * @param payee optional recognized recipient, merchant, or place name (e.g. "Metro", "Starbucks").
     */
    data class CategoryAndAmount(
        val category: String,
        val amountPaise: Long,
        val originalPhrase: String,
        val payee: String? = null,
    ) : VoiceIntent()

    /**
     * User wants to split this transaction (FR-6 voice alt-flow contract).
     *
     * @param names raw spoken name candidates — Phase 6 resolves against contacts
     *   and handles ambiguity (EC-36: multiple candidates → Phase 6 tap-to-pick UI).
     * @param amountPaise optional amount to split.
     * @param category optional category.
     */
    data class Split(
        val names: List<String>,
        val amountPaise: Long? = null,
        val category: String? = null,
        val payee: String? = null,
    ) : VoiceIntent()

    /** No recognizable intent — triggers re-prompt or manual fallback. */
    object Unclear : VoiceIntent()
}

