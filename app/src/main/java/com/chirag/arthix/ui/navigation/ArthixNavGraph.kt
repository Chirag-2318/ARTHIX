package com.chirag.arthix.ui.navigation

/**
 * Navigation route definitions for the Arthix app.
 *
 * Sealed class provides compile-time exhaustiveness for when() checks
 * and type-safe route construction.
 *
 * 4-tab bottom nav: Home / Activity / Insights / Account
 * Plus: Splash, Onboarding, Create Profile, App Lock, Manual Entry,
 *       Edit, OCR Camera, and Report (nested under Insights).
 */
sealed class ArthixRoute(val route: String) {

    // ── Bottom nav top-level destinations ──────────────────────────
    data object Home : ArthixRoute("home")
    data object Activity : ArthixRoute("activity")
    data object Insights : ArthixRoute("insights")
    data object Account : ArthixRoute("account")

    // ── Splash & Onboarding ───────────────────────────────────────
    data object Splash : ArthixRoute("splash")
    data object Onboarding : ArthixRoute("onboarding")

    // ── Post-onboarding setup ─────────────────────────────────────
    data object CreateProfile : ArthixRoute("create_profile")
    data object AppLockSetup : ArthixRoute("app_lock_setup")

    // ── Transaction flows ─────────────────────────────────────────
    data object ManualEntry : ArthixRoute("manual_entry")

    data object Edit : ArthixRoute("edit/{txnId}") {
        fun withId(txnId: Long) = "edit/$txnId"
        const val ARG_TXN_ID = "txnId"
    }

    // ── Legacy aliases for backward compat ─────────────────────────
    /** @deprecated Use [Activity] instead */
    data object History : ArthixRoute("activity")
    /** @deprecated Use [Insights] instead */
    data object Report : ArthixRoute("insights")
}
