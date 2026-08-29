package com.chirag.arthix.sms

/**
 * Security boundary for bank SMS ingestion (Phase 2.1).
 *
 * Equivalent to [PackageAllowList] for notifications — the SMS sender
 * address is checked BEFORE any SMS body content is read, logged,
 * cached, or retained.
 *
 * Indian bank transactional SMS senders use alphanumeric sender IDs
 * registered with TRAI. The format is typically:
 *   [prefix]-[BANKID]  e.g. "VM-HDFCBK", "AD-SBIINB", "JD-ICICIB"
 *
 * The suffix (after the hyphen) is the bank identifier. We match on
 * that suffix to be resilient to carrier-prefix variations.
 *
 * Hard rule: for any sender NOT matching a trusted bank pattern,
 * the receiver returns immediately. No SMS body field is read,
 * logged, cached, or retained — not even for troubleshooting.
 */
object BankSenderAllowList {

    /**
     * Known Indian bank transactional SMS sender suffixes.
     * These are the 6-char TRAI-registered entity IDs.
     *
     * Maintained in assets/bank_sms_patterns.json for runtime updates.
     * This hardcoded set is the compile-time fallback.
     */
    private val DEFAULT_TRUSTED_SUFFIXES: Set<String> = setOf(
        // Private banks
        "HDFCBK", "ICICIB", "AXISBK", "KOTAKB", "YESBNK",
        "INDBNK", "FEDBNK", "RBLBNK", "IABORB",
        // Public banks
        "SBIINB", "SBMSMS", "PNBSMS", "BOIIND", "CANBNK",
        "UCOBNK", "IDBIBN",
        // Payment banks / Fintech
        "PAYTMB", "JIOBNK", "AUSFNB", "ABORIG",
    )

    /** Runtime-overridable set (loaded from JSON config). */
    private var trustedSuffixes: Set<String> = DEFAULT_TRUSTED_SUFFIXES

    /**
     * Update the trusted suffix set from parsed config.
     * Called once at app/service startup after loading bank_sms_patterns.json.
     */
    fun updateFromConfig(suffixes: List<String>) {
        if (suffixes.isNotEmpty()) {
            trustedSuffixes = suffixes.map { it.uppercase() }.toSet()
        }
    }

    /**
     * The single entry point for the SMS sender security check.
     *
     * Called at the very top of [BankSmsReceiver.onReceive], BEFORE
     * any SMS body content is accessed.
     *
     * @param senderAddress the originating address from the SMS PDU
     *        (e.g. "VM-HDFCBK", "AD-SBIINB", "+919876543210")
     * @return true if the sender is a trusted bank sender.
     */
    fun isTrustedSender(senderAddress: String): Boolean {
        if (senderAddress.isBlank()) return false

        val upper = senderAddress.uppercase().trim()

        // Extract the suffix after the last hyphen: "VM-HDFCBK" → "HDFCBK"
        val suffix = if (upper.contains("-")) {
            upper.substringAfterLast("-")
        } else {
            upper
        }

        // Must be alphanumeric (not a phone number like +91...)
        if (suffix.any { !it.isLetterOrDigit() }) return false

        // Minimum 4 chars to avoid accidental matches
        if (suffix.length < 4) return false

        return suffix in trustedSuffixes
    }

    /**
     * Check if the sender address matches known OTP sender patterns.
     * OTP senders must be rejected even if they come from a trusted bank.
     *
     * This is a second-layer defense — even if a bank sender sends an OTP,
     * the SMS body will also be checked by [BankSmsParser] for OTP keywords.
     *
     * @param senderAddress the originating address.
     * @return true if this looks like an OTP sender.
     */
    fun isOtpSender(senderAddress: String): Boolean {
        val upper = senderAddress.uppercase()
        return "OTP" in upper || "VERIFY" in upper
    }
}
