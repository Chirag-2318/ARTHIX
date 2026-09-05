package com.chirag.arthix.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.gestures.detectTapGestures
import com.chirag.arthix.ocr.ReceiptCaptureActivity
import com.chirag.arthix.ui.nav.ArthixBottomNavBar
import com.chirag.arthix.ui.nav.ArthixDestination
import com.chirag.arthix.ui.nav.PlusOption
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.ui.navigation.ArthixRoute
import com.chirag.arthix.ui.screen.account.AccountHomeScreen
import com.chirag.arthix.ui.screen.edit.TransactionEditScreen
import com.chirag.arthix.ui.screen.history.TransactionHistoryScreen
import com.chirag.arthix.ui.screen.home.HomeScreen
import com.chirag.arthix.ui.screen.insights.InsightsScreen
import com.chirag.arthix.ui.screen.manual.ManualEntryScreen
import com.chirag.arthix.ui.screen.onboarding.OnboardingScreen
import com.chirag.arthix.ui.screen.splash.SplashScreen
import com.chirag.arthix.ui.theme.ArthixTheme

/**
 * Top-level Arthix composable — theme, scaffold, bottom nav, and NavHost.
 *
 * 5-slot floating pill nav: Home / Activity / Voice / Insights / Plus
 * Plus opens radial drag-to-select menu (Account, Streaks, Camera).
 * Voice opens VoiceCaptureBottomSheet as overlay.
 * Splash → Onboarding → Home flow for first-time users.
 */
@Composable
fun ArthixApp(
    isAccountCreated: Boolean,
    onboardingCompleted: Boolean = false,
    deepLinkTxnId: Long? = null,
    onRequestSmsPermission: () -> Unit = {},
    homeViewModel: com.chirag.arthix.ui.screen.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    ArthixTheme {
        val colors = ArthixTheme.colors
        val navController = rememberNavController()
        val splitTriggerViewModel: com.chirag.arthix.ui.screen.split.SplitTriggerViewModel = hiltViewModel()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val context = LocalContext.current

        LaunchedEffect(deepLinkTxnId) {
            if (deepLinkTxnId != null) {
                navController.navigate(ArthixRoute.Edit.withId(deepLinkTxnId))
            }
        }

        var currentPrefill by remember {
            mutableStateOf<com.chirag.arthix.ui.screen.manual.ManualEntryPrefill?>(null)
        }
        var currentSplitPrefill by remember {
            mutableStateOf<com.chirag.arthix.ui.screen.split.SplitPrefill?>(null)
        }

        // Voice capture state (triggered from nav bar Voice tab)
        var showVoiceCapture by remember { mutableStateOf(false) }

        // Camera launcher for Plus -> Camera
        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val amount = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_AMOUNT)
                val payee = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_PAYEE)
                currentPrefill = com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amount, payee = payee)
                navController.navigate(ArthixRoute.ManualEntry.route)
            }
        }

        // Top-level routes that show bottom nav
        val topLevelRoutes = listOf(
            ArthixRoute.Home.route,
            ArthixRoute.Activity.route,
            ArthixRoute.Insights.route,
        )
        val showBottomBar = currentRoute in topLevelRoutes

        // Start destination logic
        val startDestination = ArthixRoute.Splash.route
        var plusMenuExpanded by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = Color(0xFFFAF7F2), // cream background
            // removed contentWindowInsets to allow content to flow fully to bottom behind nav bar
            ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF7F2))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    // ── Splash ───────────────────────────────────────
                    composable(ArthixRoute.Splash.route) {
                        SplashScreen(
                            onSplashComplete = {
                                val destination = when {
                                    !isAccountCreated -> ArthixRoute.CreateAccount.route
                                    onboardingCompleted -> ArthixRoute.Home.route
                                    else -> ArthixRoute.Onboarding.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(ArthixRoute.Splash.route) { inclusive = true }
                                }
                            },
                        )
                    }

                    // ── Create Account ────────────────────────────────
                    composable(ArthixRoute.CreateAccount.route) {
                        com.chirag.arthix.ui.screen.account.CreateAccountScreen(
                            onAccountCreated = {
                                navController.navigate(ArthixRoute.CreateProfile.route) {
                                    popUpTo(ArthixRoute.CreateAccount.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Create Profile Picture Selection ──────────────
                    composable(ArthixRoute.CreateProfile.route) {
                        com.chirag.arthix.ui.screen.profile.CreateProfileScreen(
                            onComplete = {
                                navController.navigate(ArthixRoute.Onboarding.route) {
                                    popUpTo(ArthixRoute.CreateProfile.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Onboarding ───────────────────────────────────
                    composable(ArthixRoute.Onboarding.route) {
                        val onboardingContext = LocalContext.current
                        OnboardingScreen(
                            onComplete = {
                                val prefs = onboardingContext.getSharedPreferences(
                                    "arthix_prefs", Context.MODE_PRIVATE
                                )
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                com.chirag.arthix.sensor.ShakeDetectionService.start(onboardingContext)
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
                            onNavigateToManualEntry = { prefill ->
                                currentPrefill = prefill
                                navController.navigate(ArthixRoute.ManualEntry.route)
                            },
                            onNavigateToStreak = {
                                navController.navigate(ArthixRoute.StreakList.route)
                            },
                            onNavigateToGoals = {
                                navController.navigate(ArthixRoute.GoalList.route)
                            },
                            onAddGoal = {
                                navController.navigate(ArthixRoute.AddGoal.route)
                            },
                            onNavigateToSplit = { txnId ->
                                navController.navigate(ArthixRoute.SplitBill.withId(txnId))
                            },
                            onNavigateToSplitList = {
                                navController.navigate(ArthixRoute.Split.route)
                            },
                            onNavigateToSplitWithPrefill = { splitPrefill ->
                                currentSplitPrefill = splitPrefill
                                navController.navigate(ArthixRoute.SplitBill.withId(0L))
                            }
                        )
                    }

                    // ── Goals (AI Goal Planner) ───────────────────────
                    composable(ArthixRoute.GoalList.route) {
                        com.chirag.arthix.ui.screen.goal.GoalPlannerScreen(
                            onBack = { navController.popBackStack() },
                            onAddGoal = { navController.navigate(ArthixRoute.AddGoal.route) }
                        )
                    }

                    composable(ArthixRoute.AddGoal.route) {
                        com.chirag.arthix.ui.screen.goal.AddGoalScreen(
                            onBack = { navController.popBackStack() },
                            onGoalCreated = { navController.popBackStack() }
                        )
                    }

                    // ── Streaks List ─────────────────────────────────
                    composable(ArthixRoute.StreakList.route) {
                        com.chirag.arthix.ui.screen.streak.StreakListScreen(
                            onNavigateToStreak = { streakId ->
                                navController.navigate(ArthixRoute.BudgetStreak.withId(streakId))
                            },
                            onAddStreak = { navController.navigate(ArthixRoute.AddBudgetStreak.route) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── Streak Dashboard ───────────────────────
                    composable(
                        route = ArthixRoute.BudgetStreak.route,
                        arguments = listOf(
                            navArgument(ArthixRoute.BudgetStreak.ARG_STREAK_ID) { type = NavType.LongType }
                        )
                    ) {
                        com.chirag.arthix.ui.screen.streak.BudgetStreakScreen(
                            onBack = { navController.popBackStack() },
                            onAddStreak = { navController.navigate(ArthixRoute.AddBudgetStreak.route) },
                            onSettingsTap = { navController.navigate(ArthixRoute.Account.route) }
                        )
                    }

                    // ── Add Budget Streak ────────────────────────────
                    composable(ArthixRoute.AddBudgetStreak.route) {
                        com.chirag.arthix.ui.screen.streak.AddBudgetStreakScreen(
                            onBack = { navController.popBackStack() }
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
                            onNavigateToSplit = { txnId ->
                                navController.navigate(ArthixRoute.SplitBill.withId(txnId))
                            },
                            onNavigateToSplitWithPrefill = { splitPrefill ->
                                currentSplitPrefill = splitPrefill
                                navController.navigate(ArthixRoute.SplitBill.withId(0L))
                            }
                        )
                    }

                    // ── Insights ─────────────────────────────────────
                    composable(ArthixRoute.Insights.route) {
                        InsightsScreen()
                    }

                    // ── Account ──────────────────────────────────────
                    composable(ArthixRoute.Account.route) {
                        AccountHomeScreen(
                            onRequestSmsPermission = onRequestSmsPermission,
                            onSignOut = {
                                navController.navigate(ArthixRoute.CreateAccount.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onClearAllData = {
                                navController.navigate(ArthixRoute.CreateAccount.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Edit transaction ─────────────────────────────
                    composable(
                        route = ArthixRoute.Edit.route,
                        arguments = listOf(
                            navArgument(ArthixRoute.Edit.ARG_TXN_ID) { type = NavType.LongType }
                        ),
                    ) {
                        TransactionEditScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onTriggerSplit = { txnId -> navController.navigate(ArthixRoute.SplitBill.withId(txnId)) },
                        )
                    }

                    // ── Manual entry ─────────────────────────────────
                    composable(ArthixRoute.ManualEntry.route) {
                        val manualViewModel: com.chirag.arthix.ui.screen.manual.ManualEntryViewModel =
                            hiltViewModel()
                        LaunchedEffect(currentPrefill) {
                            currentPrefill?.let { prefill ->
                                manualViewModel.reset(prefill)
                                currentPrefill = null
                            }
                        }
                        ManualEntryScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onTriggerSplit = { txnId -> splitTriggerViewModel.triggerManualPrompt(txnId) },
                            viewModel = manualViewModel,
                        )
                    }

                    // ── Split Tab (List) — accessible via nav but not in bottom bar ──
                    composable(ArthixRoute.Split.route) {
                        com.chirag.arthix.ui.screen.split.SplitListScreen(
                            onNavigateToSplit = { txnId ->
                                navController.navigate(ArthixRoute.SplitBill.withId(txnId))
                            }
                        )
                    }

                    // ── Split Bill ───────────────────────────────────
                    composable(
                        route = ArthixRoute.SplitBill.route,
                        arguments = listOf(
                            navArgument(ArthixRoute.SplitBill.ARG_TXN_ID) { type = NavType.LongType }
                        )
                    ) {
                        val splitViewModel: com.chirag.arthix.ui.screen.split.SplitBillViewModel =
                            hiltViewModel()
                        LaunchedEffect(currentSplitPrefill) {
                            currentSplitPrefill?.let {
                                splitViewModel.applyPrefill(it)
                                currentSplitPrefill = null
                            }
                        }
                        com.chirag.arthix.ui.screen.split.SplitBillScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = splitViewModel
                        )
                    }
                }
            }

            // Disambiguation drawer overlay (PRD §6.6)
            com.chirag.arthix.ui.screen.disambiguation.DisambiguationBottomSheet()

            // Split drawer overlay (PRD §6, FR-6.1)
            com.chirag.arthix.ui.screen.split.SplitBottomSheet()

            // Full screen scrim when Plus Menu is expanded to dismiss it when clicking outside
            if (plusMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(plusMenuExpanded) {
                            detectTapGestures(onTap = { plusMenuExpanded = false })
                        }
                )
            }

            if (showVoiceCapture) {
                VoiceCaptureBottomSheet(
                    sttEngine = homeViewModel.sttEngine,
                    onDismiss = { showVoiceCapture = false },
                    onVoiceIntent = { intent, transcript ->
                        val prefill = when (intent) {
                            is com.chirag.arthix.voice.VoiceIntent.CategoryAndAmount -> {
                                val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                                com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amountStr, category = intent.category, payee = intent.payee, direction = intent.direction)
                            }
                            is com.chirag.arthix.voice.VoiceIntent.Amount -> {
                                val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                                com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amountStr, payee = intent.payee, direction = intent.direction)
                            }
                            is com.chirag.arthix.voice.VoiceIntent.Category -> com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(category = intent.category, payee = intent.payee, direction = intent.direction)
                            is com.chirag.arthix.voice.VoiceIntent.Split -> {
                                val amountStr = intent.amountPaise?.let { paise ->
                                    if (paise % 100 == 0L) "${paise / 100}" else String.format(java.util.Locale.US, "%.2f", paise / 100.0)
                                }
                                com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amountStr, category = intent.category, payee = intent.payee ?: intent.names.firstOrNull(), splitNames = intent.names, direction = intent.direction)
                            }
                            else -> com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(payee = transcript)
                        }
                        if (!prefill.splitNames.isNullOrEmpty()) {
                            val paise = prefill.amount?.toDoubleOrNull()?.let { (it * 100).toLong() }
                            currentSplitPrefill = com.chirag.arthix.ui.screen.split.SplitPrefill(amountPaise = paise, payee = prefill.payee, category = prefill.category, participantNames = prefill.splitNames)
                            navController.navigate(ArthixRoute.SplitBill.withId(0L))
                        } else {
                            currentPrefill = prefill
                            navController.navigate(ArthixRoute.ManualEntry.route)
                        }
                        showVoiceCapture = false
                    }
                )
            }

            if (showBottomBar) {
                val selectedTab = when (currentRoute) {
                    ArthixRoute.Activity.route -> ArthixDestination.ACTIVITY
                    ArthixRoute.Insights.route -> ArthixDestination.INSIGHTS
                    else -> ArthixDestination.HOME
                }
                
                ArthixBottomNavBar(
                    selectedDestination = selectedTab,
                    onDestinationSelected = { dest ->
                        val route = when (dest) {
                            ArthixDestination.HOME -> ArthixRoute.Home
                            ArthixDestination.ACTIVITY -> ArthixRoute.Activity
                            ArthixDestination.INSIGHTS -> ArthixRoute.Insights
                        }
                        navController.navigate(route.route) {
                            popUpTo(ArthixRoute.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onVoiceClick = { showVoiceCapture = true },
                    onPlusOptionSelected = { option ->
                        when (option) {
                            PlusOption.ACCOUNT -> navController.navigate(ArthixRoute.Account.route)
                            PlusOption.GOALS -> navController.navigate(ArthixRoute.GoalList.route)
                            PlusOption.STREAKS -> navController.navigate(ArthixRoute.StreakList.route)
                            PlusOption.CAMERA -> {
                                com.chirag.arthix.MainActivity.isLaunchingInternalActivity = true
                                cameraLauncher.launch(ReceiptCaptureActivity.createIntent(context))
                            }
                        }
                    },
                    onPlusExpandedChange = { plusMenuExpanded = it },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }
            }
        }
    }
}
