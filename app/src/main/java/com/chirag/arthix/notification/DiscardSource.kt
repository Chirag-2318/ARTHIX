package com.chirag.arthix.notification

/**
 * How a capture was discarded (PRD §7.7).
 *
 * Two different callers — Phase 3 (chip tap) and Phase 4 (voice intent) —
 * both call the same underlying [ReconciliationEngine.discardCapture].
 * This enum exists solely for traceability, not differing logic paths.
 */
enum class DiscardSource {
    /** User tapped "not a transaction" on the chip UI (Phase 3). */
    CHIP_TAP,

    /** Voice discard-intent recognized ("skip"/"not real"/"ignore that one") (Phase 4). */
    VOICE_INTENT,
}
