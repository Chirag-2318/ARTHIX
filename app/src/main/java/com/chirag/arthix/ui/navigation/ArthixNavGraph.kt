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
    data object Split : ArthixRoute("split")
    data object Insights : ArthixRoute("insights")
    data object Account : ArthixRoute("account")

    // ── Splash & Onboarding ───────────────────────────────────────
    data object Splash : ArthixRoute("splash")
    data object CreateAccount : ArthixRoute("create_account")
    data object Onboarding : ArthixRoute("onboarding")

    // ── Post-onboarding setup ─────────────────────────────────────
    data object CreateProfile : ArthixRoute("create_profile")
    data object AppLockSetup : ArthixRoute("app_lock_setup")

    // ── Transaction flows ─────────────────────────────────────────
    data object ManualEntry : ArthixRoute("manual_entry")

    data object Report : ArthixRoute("report")

    // Streaks
    data object StreakList : ArthixRoute("streak_list")
    data object AddBudgetStreak : ArthixRoute("add_budget_streak")
    data object BudgetStreak : ArthixRoute("budget_streak/{streakId}") {
        const val ARG_STREAK_ID = "streakId"
        fun withId(id: Long) = "budget_streak/$id"
    }

    // Goals (AI Goal Planner)
    data object GoalList : ArthixRoute("goal_list")
    data object AddGoal : ArthixRoute("add_goal")

    data object Edit : ArthixRoute("edit/{txnId}") {
        fun withId(txnId: Long) = "edit/$txnId"
        const val ARG_TXN_ID = "txnId"
    }

    data object SplitBill : ArthixRoute("split_bill/{txnId}") {
        fun withId(txnId: Long) = "split_bill/$txnId"
        const val ARG_TXN_ID = "txnId"
    }

    // ── Legacy aliases for backward compat ─────────────────────────
    /** @deprecated Use [Activity] instead */
    data object History : ArthixRoute("activity")

}
