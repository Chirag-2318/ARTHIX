package com.chirag.arthix.sms

import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.notification.model.ConfidenceLevel
import com.chirag.arthix.notification.model.NotificationOutcome
import com.chirag.arthix.notification.model.TransactionCandidate
import com.chirag.arthix.notification.model.TransactionSourceType
import com.chirag.arthix.util.AmountParseResult
import com.chirag.arthix.util.AmountParser
import java.security.MessageDigest

/**
 * Pure-Kotlin parser for Indian bank transactional SMS messages.
 *
 * Handles the common SMS formats across HDFC, SBI, ICICI, Axis, Kotak,
 * and other major Indian banks.
 *
 * **Pipeline:**
 * 1. Reject keywords check (OTP, promo, balance, alerts) → REJECTED
 * 2. Refund detection → REFUND
 * 3. Debit/credit pattern matching → extract amount
 * 4. Payee extraction (best-effort)
 * 5. UPI reference extraction (best-effort)
 * 6. Build [TransactionCandidate]
 *
 * **Privacy:** the raw SMS body is NEVER stored. Only structured fields
 * (amount, payee, direction) and a SHA-256 fingerprint are persisted.
 *
 * No Android framework dependency — fully JVM-testable.
 */
object BankSmsParser {

    // ── Compiled patterns (loaded once) ────────────────────────────────

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "sent",
        "purchase", "withdrawn", "txn", "transaction",
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited",
    )

    private val REFUND_KEYWORDS = listOf(
        "refund", "reversal", "reversed",
    )

    private val REJECT_KEYWORDS = listOf(
        "otp", "one time password", "verification code",
        "offer", "cashback", "pre-approved", "apply now",
        "emi", "loan", "insurance", "mutual fund",
        "balance is", "avl bal", "available balance",
        "login alert", "password changed", "pin changed",
        "card blocked", "kyc", "link aadhaar", "update pan",
        "sweepstake", "lottery", "prize", "reward points",
    )

    /**
     * Amount extraction pattern — handles ₹, Rs., Rs, INR prefixes
     * and standard Indian number formatting.
     */
    private val AMOUNT_REGEX = Regex(
        """(?:Rs\.?|INR|₹)\s?(\d[\d,]*(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * UPI reference/UTR extraction — 12-digit UPI transaction reference.
     */
    private val UPI_REF_REGEX = Regex(
        """(?:UPI[\s/-]*(?:Ref|ref|REF)?[:\s]*)(\d{12})""",
    )

    /**
     * Payee extraction — best-effort, looks for patterns like
     * "to RAMESH CHAI" or "at SWIGGY" in SMS text.
     */
    private val PAYEE_REGEX = Regex(
        """(?:to|at|for)\s+([A-Za-z][A-Za-z0-9\s.&'-]{2,30}?)(?:\s+(?:on|via|UPI|VPA|Ref|A/c|Avl|thru|w\.e\.f)|[.;,]|$)""",
        RegexOption.IGNORE_CASE,
    )

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Parse a bank SMS body into a [TransactionCandidate].
     *
     * @param smsBody the full SMS text.
     * @param senderAddress the originating sender (e.g. "VM-HDFCBK").
     * @param timestampMs receipt time in epoch millis.
     * @return a [TransactionCandidate] if parsed, or null if rejected/unparseable.
     */
    fun parse(smsBody: String, senderAddress: String, timestampMs: Long): TransactionCandidate? {
        val lowerBody = smsBody.lowercase()

        // ── Step 1: reject OTP/promo/balance/alert ─────────────────
        if (shouldReject(lowerBody)) {
            return null
        }

        // ── Step 2: classify outcome ───────────────────────────────
        val outcome: NotificationOutcome
        val direction: Direction

        val isRefund = REFUND_KEYWORDS.any { it in lowerBody }
        val isDebit = DEBIT_KEYWORDS.any { it in lowerBody }
        val isCredit = CREDIT_KEYWORDS.any { it in lowerBody }

        when {
            isRefund -> {
                outcome = NotificationOutcome.REFUND
                direction = Direction.INFLOW
            }
            isDebit && !isCredit -> {
                outcome = NotificationOutcome.COMPLETED
                direction = Direction.OUTFLOW
            }
            isCredit && !isDebit -> {
                outcome = NotificationOutcome.COMPLETED
                direction = Direction.INFLOW
            }
            isDebit && isCredit -> {
                // Both keywords present — ambiguous. Prefer debit (safer: user reviews).
                outcome = NotificationOutcome.COMPLETED
                direction = Direction.OUTFLOW
            }
            else -> {
                // No direction keyword found — cannot determine
                return null
            }
        }

        // ── Step 3: extract amount ─────────────────────────────────
        val amountMatch = AMOUNT_REGEX.find(smsBody) ?: return null
        val amountRaw = amountMatch.groupValues[1]
        val parseResult = AmountParser.parse(amountRaw)
        val amountPaise = when (parseResult) {
            is AmountParseResult.Success -> parseResult.amountPaise
            is AmountParseResult.Failure -> return null
        }

        // Sanity: zero-amount SMS are not valid transactions
        if (amountPaise <= 0) return null

        // ── Step 4: extract payee (best-effort) ────────────────────
        val payee = extractPayee(smsBody)

        // ── Step 5: extract UPI reference (best-effort) ────────────
        val referenceId = UPI_REF_REGEX.find(smsBody)?.groupValues?.get(1)

        // ── Step 6: compute fingerprint (privacy: hash, don't store text) ──
        val rawFingerprint = sha256(smsBody)

        // ── Step 7: determine confidence ───────────────────────────
        val confidence = when {
            amountPaise > 0 && payee != null && referenceId != null -> ConfidenceLevel.HIGH
            amountPaise > 0 && (payee != null || referenceId != null) -> ConfidenceLevel.HIGH
            amountPaise > 0 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        return TransactionCandidate(
            sourceType = TransactionSourceType.BANK_SMS,
            sourcePackage = null,
            senderAddress = senderAddress,
            amountPaise = amountPaise,
            payee = payee,
            direction = direction,
            referenceId = referenceId,
            rawFingerprint = rawFingerprint,
            confidence = confidence,
            timestampMs = timestampMs,
            outcome = outcome,
        )
    }

    // ── Internal helpers ───────────────────────────────────────────────

    /**
     * Check if the SMS should be rejected before any financial parsing.
     */
    fun shouldReject(lowerBody: String): Boolean {
        return REJECT_KEYWORDS.any { it in lowerBody }
    }

    /**
     * Best-effort payee extraction from SMS text.
     */
    private fun extractPayee(text: String): String? {
        val match = PAYEE_REGEX.find(text) ?: return null
        val payee = match.groupValues[1].trim()
        // Filter out likely non-payee matches (too short, all digits, etc.)
        if (payee.length < 3) return null
        if (payee.all { it.isDigit() }) return null
        return payee
    }

    /**
     * SHA-256 hash of SMS text for dedup fingerprinting.
     * Privacy: the raw text is NEVER stored — only this hash.
     */
    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
