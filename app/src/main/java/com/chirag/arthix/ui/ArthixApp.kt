package com.chirag.arthix.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chirag.arthix.ui.components.ArthixBottomNavBar
import com.chirag.arthix.ui.navigation.ArthixRoute
import com.chirag.arthix.ui.screen.account.AccountHomeScreen
import com.chirag.arthix.ui.screen.edit.TransactionEditScreen
import com.chirag.arthix.ui.screen.history.TransactionHistoryScreen
import com.chirag.arthix.ui.screen.home.HomeScreen
import com.chirag.arthix.ui.screen.insights.InsightsScreen
import com.chirag.arthix.ui.screen.manual.ManualEntryScreen
import com.chirag.arthix.ui.screen.onboarding.OnboardingScreen
import com.chirag.arthix.ui.screen.report.ReportScreen
import com.chirag.arthix.ui.screen.splash.SplashScreen
import com.chirag.arthix.ui.theme.ArthixTheme

/**
 * Top-level Arthix composable — theme, scaffold, bottom nav, and NavHost.
 *
 * 4-tab bottom nav: Home / Activity / Insights / Account
 * Splash → Onboarding → Home flow for first-time users.
 * FAB (manual entry, FR-5) visible on all top-level screens (EC-35).
 */
@Composable
fun ArthixApp(
    onboardingCompleted: Boolean = false,
    deepLinkTxnId: Long? = null,
    onRequestSmsPermission: () -> Unit = {},
) {
    ArthixTheme {
        val colors = ArthixTheme.colors
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        LaunchedEffect(deepLinkTxnId) {
            if (deepLinkTxnId != null) {
                navController.navigate(ArthixRoute.Edit.withId(deepLinkTxnId))
            }
        }

        var currentPrefill by remember {
            mutableStateOf<com.chirag.arthix.ui.screen.manual.ManualEntryPrefill?>(null)
        }

        // Top-level routes that show bottom nav
        val topLevelRoutes = listOf(
            ArthixRoute.Home.route,
            ArthixRoute.Activity.route,
            ArthixRoute.Insights.route,
            ArthixRoute.Account.route,
        )
        val showBottomBar = currentRoute in topLevelRoutes

        // Start destination logic
        val startDestination = if (onboardingCompleted) {
            ArthixRoute.Splash.route
        } else {
            ArthixRoute.Splash.route
        }

        Scaffold(
            containerColor = colors.bg,
            bottomBar = {
                if (showBottomBar) {
                    ArthixBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route.route) {
                                popUpTo(ArthixRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (showBottomBar) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(ArthixRoute.ManualEntry.route)
                        },
                        shape = CircleShape,
                        containerColor = colors.accent,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add transaction manually")
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    // ── Splash ───────────────────────────────────────
                    composable(ArthixRoute.Splash.route) {
                        SplashScreen(
                            onSplashComplete = {
                                val destination = if (onboardingCompleted) {
                                    ArthixRoute.Home.route
                                } else {
                                    ArthixRoute.Onboarding.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(ArthixRoute.Splash.route) { inclusive = true }
                                }
                            },
                        )
                    }

                    // ── Onboarding ───────────────────────────────────
                    composable(ArthixRoute.Onboarding.route) {
                        val context = LocalContext.current
                        OnboardingScreen(
                            onComplete = {
                                val prefs = context.getSharedPreferences(
                                    "arthix_prefs", Context.MODE_PRIVATE
                                )
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                navController.navigate(ArthixRoute.Home.route) {
                                    popUpTo(0)
                                }
                            },
                            onRequestSmsPermission = onRequestSmsPermission,
                        )
                    }

                    // ── Home (dashboard) ─────────────────────────────
                    composable(ArthixRoute.Home.route) {
                        HomeScreen(
                            onNavigateToActivity = {
                                navController.navigate(ArthixRoute.Activity.route) {
                                    popUpTo(ArthixRoute.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToEdit = { txnId ->
                                navController.navigate(ArthixRoute.Edit.withId(txnId))
                            },
                        )
                    }

                    // ── Activity (transaction history) ───────────────
                    composable(ArthixRoute.Activity.route) {
                        TransactionHistoryScreen(
                            onNavigateToEdit = { txnId ->
                                navController.navigate(ArthixRoute.Edit.withId(txnId))
                            },
                            onNavigateToOnboarding = {
                                navController.navigate(ArthixRoute.Onboarding.route)
                            },
                            onNavigateToManualEntry = { prefill ->
                                currentPrefill = prefill
                                navController.navigate(ArthixRoute.ManualEntry.route)
                            },
                        )
                    }

                    // ── Insights ─────────────────────────────────────
                    composable(ArthixRoute.Insights.route) {
                        InsightsScreen()
                    }

                    // ── Account ──────────────────────────────────────
                    composable(ArthixRoute.Account.route) {
                        AccountHomeScreen()
                    }

                    // ── Edit transaction ─────────────────────────────
                    composable(
                        route = ArthixRoute.Edit.route,
                        arguments = listOf(
                            navArgument(ArthixRoute.Edit.ARG_TXN_ID) { type = NavType.LongType }
                        ),
                    ) {
                        val splitTriggerViewModel: com.chirag.arthix.ui.screen.split.SplitTriggerViewModel =
                            hiltViewModel()
                        TransactionEditScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onTriggerSplit = { splitTriggerViewModel.triggerManualPrompt(it) },
                        )
                    }

                    // ── Manual entry ─────────────────────────────────
                    composable(ArthixRoute.ManualEntry.route) {
                        val manualViewModel: com.chirag.arthix.ui.screen.manual.ManualEntryViewModel =
                            hiltViewModel()
                        LaunchedEffect(currentPrefill) {
                            currentPrefill?.let {
                                manualViewModel.openWithPrefill(it)
                                currentPrefill = null
                            }
                        }
                        ManualEntryScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = manualViewModel,
                        )
                    }
                }
            }

            // Disambiguation drawer overlay (PRD §6.6)
            com.chirag.arthix.ui.screen.disambiguation.DisambiguationBottomSheet()

            // Split drawer overlay (PRD §6, FR-6.1)
            com.chirag.arthix.ui.screen.split.SplitBottomSheet()
        }
    }
}
