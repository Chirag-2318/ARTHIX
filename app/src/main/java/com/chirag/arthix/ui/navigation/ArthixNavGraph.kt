package com.chirag.arthix.ui.navigation

/**
 * Navigation route definitions for the Arthix app.
 *
 * Sealed interface provides compile-time exhaustiveness for when() checks
 * and type-safe route construction.
 */
sealed class ArthixRoute(val route: String) {
    data object History : ArthixRoute("history")
    data object Edit : ArthixRoute("edit/{txnId}") {
        fun withId(txnId: Long) = "edit/$txnId"
        const val ARG_TXN_ID = "txnId"
    }
    data object ManualEntry : ArthixRoute("manual_entry")
    data object Report : ArthixRoute("report")
    data object Onboarding : ArthixRoute("onboarding")
}
