package com.chirag.arthix.notification.model

import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction

// ── Text Extraction ────────────────────────────────────────────────────

/**
 * Source of the extracted notification text (PRD §4.1).
 *
 * Priority order: BIG_TEXT > TEXT_LINES > TEXT_FALLBACK > NONE.
 * TEXT_FALLBACK triggers [ConfidenceFlag.NEEDS_REVIEW] on any match
 * derived from it (PRD §4.2).
 */
enum class TextSource {
    BIG_TEXT,
    TEXT_LINES,
    TEXT_FALLBACK,
    NONE,
}

/**
 * Result of notification text extraction.
 *
 * @param raw the extracted text content.
 * @param source which notification extra the text came from.
 */
data class ExtractedText(
    val raw: String,
    val source: TextSource,
)

// ── Outcome Classification ─────────────────────────────────────────────

/**
 * Classification of a notification's payment outcome (PRD §5.2).
 *
 * Applied BEFORE a match becomes a [PendingNotificationEntity]:
 * - COMPLETED → enters reconciliation pipeline
 * - REJECTED → silently dropped (declined/failed/pending/cancelled)
 * - REFUND → routed to refund netting (§5.3), not fresh outflow
 */
enum class NotificationOutcome {
    COMPLETED,
    REJECTED,
    REFUND,
}

// ── Pattern Config (loaded from assets/upi_patterns.json) ──────────────

/**
 * A single regex pattern entry from the JSON config.
 *
 * @param app package name this pattern applies to.
 * @param regex pattern with capture groups: group(1)=amount, group(2)=payee.
 */
data class PatternEntry(
    val app: String,
    val regex: String,
)

/**
 * Full pattern configuration loaded from `assets/upi_patterns.json`.
 *
 * Loaded once at Service start into memory (PRD §5.1 / EC-09).
 * A config change requires only shipping an updated asset file.
 */
data class PatternConfig(
    val outflowPatterns: List<PatternEntry>,
    val inflowPatterns: List<PatternEntry>,
    val outcomeRejectKeywords: List<String>,
)

// ── Parsed Notification Results ────────────────────────────────────────

/**
 * A successfully parsed outflow notification, ready for reconciliation.
 *
 * @param amountPaise amount in integer paise (never Float/Double, EC-46).
 * @param payee extracted payee string (raw, not normalized for display).
 * @param packageName source app's package name.
 * @param rawText original notification text for debug/audit.
 * @param confidenceFlag CLEAN if from BIG_TEXT/TEXT_LINES, NEEDS_REVIEW if fallback.
 */
data class ParsedOutflow(
    val amountPaise: Long,
    val payee: String,
    val packageName: String,
    val rawText: String,
    val confidenceFlag: ConfidenceFlag,
)

/**
 * A successfully parsed inflow notification.
 * Inflows bypass the capture queue entirely (PRD §7.9).
 */
data class ParsedInflow(
    val amountPaise: Long,
    val payee: String,
    val packageName: String,
)

// ── Match Result (output of pattern matching pipeline) ─────────────────

/**
 * Result of applying notification pattern matching (PRD §5).
 */
sealed class MatchResult {
    data class OutflowMatch(val parsed: ParsedOutflow) : MatchResult()
    data class InflowMatch(val parsed: ParsedInflow) : MatchResult()
    data class RefundMatch(val amountPaise: Long, val payee: String) : MatchResult()
    data object Rejected : MatchResult()
    data object NoMatch : MatchResult()
}

// ── Dedup Result ───────────────────────────────────────────────────────

/**
 * Result of the deduplication check (PRD §6).
 */
sealed class DedupResult {
    /** This notification is a duplicate of an already-processed payment. */
    data class Duplicate(val existingTransactionId: Long) : DedupResult()
    /** This is a new, unseen payment notification. */
    data object NewRecord : DedupResult()
}

// ── Multi-Source Ingestion (Phase 2.1) ──────────────────────────────────

/**
 * Provenance of a transaction candidate — tracks HOW the financial event
 * was captured. Each source type has its own independent security boundary.
 *
 * - UPI_APP_NOTIFICATION: from PackageAllowList-verified notification
 * - BANK_SMS: from BankSenderAllowList-verified SMS
 */
enum class TransactionSourceType {
    UPI_APP_NOTIFICATION,
    BANK_SMS,
}

/**
 * Confidence level of parsed transaction data.
 *
 * - HIGH: trusted source, clean parse, no fallback path
 * - MEDIUM: trusted source but text extraction used fallback, or SMS parsing had partial info
 * - LOW: ambiguous parse, missing fields, or cross-source conflict
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
}

/**
 * Source-agnostic, normalized transaction candidate — the single unit
 * that enters the reconciliation pipeline regardless of origin.
 *
 * Both UPI app notifications and bank SMS produce this same type.
 * The [sourceType] + [sourcePackage] / [senderAddress] fields provide
 * full provenance without the reconciliation engine needing to know
 * which ingestion path produced the candidate.
 *
 * Privacy: [rawFingerprint] is a SHA-256 hash of the raw text for
 * dedup. The raw text itself is NOT stored.
 */
data class TransactionCandidate(
    val sourceType: TransactionSourceType,
    val sourcePackage: String?,       // e.g. "com.phonepe.app" — null for SMS
    val senderAddress: String?,       // e.g. "VM-HDFCBK" — null for notifications
    val amountPaise: Long,
    val payee: String?,
    val direction: Direction,
    val referenceId: String?,         // UPI ref/UTR when parseable
    val rawFingerprint: String,       // SHA-256 of raw text (text NOT stored)
    val confidence: ConfidenceLevel,
    val timestampMs: Long,
    val outcome: NotificationOutcome, // COMPLETED / REJECTED / REFUND
)

