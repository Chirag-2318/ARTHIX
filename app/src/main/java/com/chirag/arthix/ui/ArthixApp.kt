package com.chirag.arthix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chirag.arthix.ui.navigation.ArthixRoute
import com.chirag.arthix.ui.screen.edit.TransactionEditScreen
import com.chirag.arthix.ui.screen.history.TransactionHistoryScreen
import com.chirag.arthix.ui.screen.manual.ManualEntryScreen
import com.chirag.arthix.ui.screen.onboarding.OnboardingScreen
import com.chirag.arthix.ui.screen.report.ReportScreen
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Label

/**
 * Top-level Arthix composable — theme, scaffold, bottom nav, and NavHost.
 *
 * Bottom nav mirrors Uber's 4-tab bar: icon + label, accent-colored
 * selected state, true-black background. FAB (manual fallback, FR-5)
 * is visible on every top-level screen (EC-35).
 */
@Composable
fun ArthixApp(
    onboardingCompleted: Boolean = false,
    onRequestSmsPermission: () -> Unit = {}
) {
    ArthixTheme {
        val colors = ArthixTheme.colors
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Bottom nav items
        val topLevelRoutes = listOf(
            Triple(ArthixRoute.History, Icons.Outlined.History, "History"),
            Triple(ArthixRoute.Report, Icons.Outlined.Assessment, "Report"),
        )

        val showBottomBar = currentRoute in listOf(
            ArthixRoute.History.route,
            ArthixRoute.Report.route,
        )

        Scaffold(
            containerColor = colors.bg,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp),
                    ) {
                        topLevelRoutes.forEach { (route, icon, label) ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == route.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = Label,
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(route.route) {
                                        popUpTo(ArthixRoute.History.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.textPrimary,
                                    selectedTextColor = colors.textPrimary,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                    indicatorColor = Color.Transparent,
                                ),
                            )
                        }
                    }
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
                    startDestination = if (onboardingCompleted) ArthixRoute.History.route else ArthixRoute.Onboarding.route,
                ) {
                    composable(ArthixRoute.History.route) {
                        TransactionHistoryScreen(
                            onNavigateToEdit = { txnId ->
                                navController.navigate(ArthixRoute.Edit.withId(txnId))
                            },
                            onNavigateToOnboarding = {
                                navController.navigate(ArthixRoute.Onboarding.route)
                            },
                        )
                    }

                    composable(
                        route = ArthixRoute.Edit.route,
                        arguments = listOf(
                            navArgument(ArthixRoute.Edit.ARG_TXN_ID) { type = NavType.LongType }
                        ),
                    ) {
                        TransactionEditScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(ArthixRoute.ManualEntry.route) {
                        ManualEntryScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(ArthixRoute.Report.route) {
                        ReportScreen()
                    }

                    composable(ArthixRoute.Onboarding.route) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        OnboardingScreen(
                            onComplete = {
                                val prefs = context.getSharedPreferences("arthix_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                android.util.Log.d("Onboarding", "step=onboarding_completed=true completed")
                                navController.navigate(ArthixRoute.History.route) {
                                    popUpTo(0)
                                }
                            },
                            onRequestSmsPermission = onRequestSmsPermission
                        )
                    }
                }
            }
            
            // Disambiguation drawer overlay (PRD §6.6)
            com.chirag.arthix.ui.screen.disambiguation.DisambiguationBottomSheet()
        }
    }
}
