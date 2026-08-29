package com.chirag.arthix.notification

/**
 * Data contract for Phase 3's disambiguation prompt UI (PRD §7.5).
 *
 * This phase emits [DisambiguationPrompt] via a Flow; Phase 3 collects it,
 * renders the candidates as tappable options, and calls
 * [ReconciliationEngine.resolveDisambiguation] on tap.
 *
 * If untapped within [timeoutMs], this phase handles the fallback autonomously
 * (nearest-neighbor best guess, confidence_flag = AUTO_RESOLVED).
 */
data class DisambiguationPrompt(
    /** ID of the notification being disambiguated. */
    val notificationId: String,

    /** Amount in paise — for display context. */
    val amountPaise: Long,

    /** Payee string — for display context. */
    val payee: String,

    /** 2–3 candidates, ordered best-first (closest in time). */
    val candidates: List<DisambiguationCandidate>,

    /** Timeout in ms before auto-fallback. Default 8000ms per PRD §7.5. */
    val timeoutMs: Long = 8_000L,
)

/**
 * A single candidate in a disambiguation prompt.
 */
data class DisambiguationCandidate(
    /** PendingCapture ID — the join key Phase 3 passes back on tap. */
    val captureId: String,

    /** Display-only: approximate seconds ago this shake occurred. Derived from monotonic delta. */
    val approximateSecondsAgo: Int,

    /** If the user already tapped a category on this capture's chip, it's shown here. */
    val category: String?,
)
