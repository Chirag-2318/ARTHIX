package com.chirag.arthix.notification

/**
 * Package allow-list — the actual security boundary of Phase 2 (PRD §3).
 *
 * Per EC-08 (Critical) and EC-56 (Critical): text-only pattern matching
 * with no source-app check means any app posting text resembling "₹123 paid
 * to X" could be misread as a real payment. The allow-list is the real
 * security/correctness boundary, not the regex layer.
 *
 * Hard rule (§3.3): for any packageName NOT in this set, the
 * NotificationListenerService returns immediately. No field of that
 * notification's text content is read, logged, cached, or retained —
 * even transiently, even for troubleshooting.
 *
 * Action item (PRD §3.2): verify these package names against the actual
 * apps installed on the demo device during Phase 6/7 pre-demo checklist.
 */
object PackageAllowList {

    /**
     * Starting set — the three UPI apps required for the hackathon demo.
     */
    val UPI_APP_PACKAGES: Set<String> = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                         // PhonePe
        "net.one97.paytm",                         // Paytm
    )

    /**
     * The single entry point for the allow-list check.
     * Called at the very top of [onNotificationPosted], before any
     * text access (PRD §3.3 non-negotiable requirement).
     */
    fun isAllowed(packageName: String): Boolean =
        packageName in UPI_APP_PACKAGES
}
