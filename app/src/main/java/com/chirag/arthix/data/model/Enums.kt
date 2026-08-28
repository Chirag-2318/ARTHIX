package com.chirag.arthix.data.model

/**
 * Shared enums referenced by multiple entities across the Arthix data layer.
 * These are persisted as String values via [EnumConverters].
 *
 * FROZEN as of Phase 0 — adding a new enum value is a schema change request,
 * not a unilateral edit, because report generation (Phase 3) and UI (Phase 4)
 * both switch/match exhaustively over these enums.
 */

/** Whether a transaction represents money coming in or going out. */
enum class Direction { INFLOW, OUTFLOW }

/** How the transaction was originally captured. */
enum class CaptureSource { SHAKE, VOICE, CAMERA, MANUAL }

/**
 * Lifecycle status of a transaction.
 *
 * - CONFIRMED: fully resolved, amount + category known.
 * - AWAITING_MATCH: shake committed, waiting for a notification match (EC-53).
 * - AWAITING_CATEGORY: amount known, category not yet chosen.
 * - AWAITING_AMOUNT: reconciliation timed out with no match (EC-17).
 * - DISCARDED: explicit "not a transaction" action (EC-16/21/25).
 */
enum class TransactionStatus {
    CONFIRMED,
    AWAITING_MATCH,
    AWAITING_CATEGORY,
    AWAITING_AMOUNT,
    DISCARDED
}

/**
 * Confidence level of the transaction data.
 *
 * - CLEAN: no ambiguity, no fallback path touched it.
 * - AUTO_RESOLVED: disambiguation prompt was ignored and auto-picked (EC-15).
 * - NEEDS_REVIEW: low-confidence OCR/notification parse (EC-22, EC-30, EC-32).
 */
enum class ConfidenceFlag {
    CLEAN,
    AUTO_RESOLVED,
    NEEDS_REVIEW
}

/** How a split was confirmed by the user. */
enum class SplitConfirmedVia { TAP, VOICE }

/**
 * Whether a split's amount tracks the parent transaction live or was
 * locked at creation time (EC-40). Default is LIVE — documented as the
 * explicit chosen behavior, not an oversight.
 */
enum class AmountLock { LIVE, LOCKED_AT_CREATION }
