package com.chirag.arthix.ui.screen.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.ocr.ReceiptCaptureActivity
import com.chirag.arthix.ui.components.DeleteTxnDialog
import com.chirag.arthix.ui.components.SwipeableTxnRow
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.ui.screen.manual.ManualEntryPrefill
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.*
import com.chirag.arthix.R

/* ─────────────────────────────────────────────────────────────────────────
   TOKENS — shared dark base with AddTransactionScreen; orange stays the
   brand accent (matches your existing Voice/Camera buttons + streak ring),
   green/red reserved strictly for inflow/outflow like the reference's
   price-change green.
   ───────────────────────────────────────────────────────────────────── */

private object ArthixColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceRaised = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)

    val Brand = Color(0xFFFF7A1A)          // existing Voice/Camera orange
    val BrandGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9142), Color(0xFFFF5B3D)),
        start = Offset(0f, 0f), end = Offset(1000f, 400f)
    )
    val Positive = Color(0xFF34D399)
    val Negative = Color(0xFFFF5B5B)
    val Pending = Color(0xFF6B6B75)

    // reward/insight banner — echoes the reference's yellow, shifted warmer
    // so it still reads as "money app good news", not literally identical
    val InsightGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFC24B), Color(0xFFFF9142)),
        start = Offset(0f, 0f), end = Offset(1000f, 300f)
    )
}

private enum class HomeTab { QUICK_LOG, RECENT }

private data class QuickCategory(val label: String, val icon: ImageVector, val tint: Color)

private val quickCategories = listOf(
    QuickCategory("Food", Icons.Filled.Restaurant, Color(0xFFFF6B5B)),
    QuickCategory("Travel", Icons.Filled.Flight, Color(0xFF4C8CFF)),
    QuickCategory("Shopping", Icons.Filled.ShoppingBag, Color(0xFFB56BFF)),
    QuickCategory("Groceries", Icons.Filled.ShoppingCart, Color(0xFF34D399)),
)

private data class TxnRow(
    val id: Long,
    val payee: String,
    val category: String,
    val time: String,
    val amountLabel: String,      // e.g. "+₹1000" / "-₹100" / "—"
    val isInflow: Boolean,
    val status: TransactionStatus,
    val icon: ImageVector,
    val tint: Color,
    val entity: TransactionEntity? = null
)

private fun formatPaise(paise: Long): String {
    val rupees = paise / 100.0
    return if (rupees == rupees.toLong().toDouble()) {
        "₹${rupees.toLong()}"
    } else {
        "₹${"%.2f".format(rupees)}"
    }
}

@Composable
fun HomeScreen(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToManualEntry: (ManualEntryPrefill?) -> Unit = {},
    onNavigateToStreak: () -> Unit = {},
    onNavigateToSplit: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showVoiceCapture by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    uiState.transactionToDelete?.let { txn ->
        DeleteTxnDialog(
            amountPaise = txn.amountPaise,
            payee = txn.payee,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDelete() }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val amount = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_AMOUNT)
            val payee = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_PAYEE)
            onNavigateToManualEntry(ManualEntryPrefill(amount = amount, payee = payee))
        }
    }

    if (showVoiceCapture) {
        VoiceCaptureBottomSheet(
            sttEngine = viewModel.sttEngine,
            onDismiss = { showVoiceCapture = false },
            onResult = { prefill ->
                onNavigateToManualEntry(prefill)
            }
        )
    }

    val mappedTxns = uiState.recentTransactions.map { txn ->
        val isPending = txn.status == TransactionStatus.AWAITING_MATCH || txn.status == TransactionStatus.AWAITING_CATEGORY || txn.status == TransactionStatus.AWAITING_AMOUNT
        
        val categoryLabel = txn.category ?: ""
        val matchedCategory = quickCategories.find { it.label.equals(categoryLabel, ignoreCase = true) }
        val categoryIcon = matchedCategory?.icon ?: Icons.Filled.ReceiptLong
        val categoryTint = matchedCategory?.tint ?: Color(0xFF6B6B75)

        TxnRow(
            id = txn.id,
            payee = txn.payee ?: txn.category?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            category = txn.category?.replaceFirstChar { it.uppercase() } ?: "Uncategorized",
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(txn.timestamp)),
            amountLabel = if (isPending) "—" else (if (txn.direction == Direction.OUTFLOW) "- " else "+ ") + formatPaise(txn.amountPaise ?: 0L),
            isInflow = txn.direction == Direction.INFLOW,
            status = txn.status,
            icon = categoryIcon,
            tint = categoryTint,
            entity = txn
        )
    }

    ArthixHomeScreen(
        userName = "Chirag",
        balanceLabel = formatPaise(uiState.todaySpendPaise),
        weekChangePercent = uiState.weekChangePercent,
        streakDays = uiState.streakDays,
        txnsLoggedThisWeek = uiState.txnsLoggedThisWeek,
        insightHeadline = uiState.insightHeadline,
        insightBody = uiState.insightBody,
        onVoiceTap = { showVoiceCapture = true },
        onCameraTap = { cameraLauncher.launch(ReceiptCaptureActivity.createIntent(context)) },
        onManualTap = { onNavigateToManualEntry(null) },
        onNotificationsTap = { showNotifications = true },
        unreadAlertsCount = uiState.unreadAlertsCount,
        onFabTap = { onNavigateToManualEntry(null) },
        onViewAllTxns = onNavigateToActivity,
        onQuickCategoryTap = { category ->
            onNavigateToManualEntry(ManualEntryPrefill(category = category, direction = Direction.OUTFLOW))
        },
        txns = mappedTxns,
        onNavigateToEdit = onNavigateToEdit,
        onDeleteTxn = { txn -> viewModel.requestDelete(txn) },
        onNavigateToStreak = onNavigateToStreak,
        onNavigateToSplit = onNavigateToSplit,
        coachMarkDismissed = uiState.coachMarkDismissed,
        onDismissCoachMark = { viewModel.dismissCoachMark() },
    )

    if (showNotifications) {
        NotificationsBottomSheet(
            alerts = uiState.alerts,
            onDismiss = { showNotifications = false }
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   SCREEN
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun ArthixHomeScreen(
    userName: String = "Chirag",
    balanceLabel: String = "₹150.00",
    weekChangePercent: Double = -12.4,
    streakDays: Int = 4,
    txnsLoggedThisWeek: Int = 11,
    insightHeadline: String = "You're 12% over your Food budget",
    insightBody: String = "Skip 2 more Swiggy orders this week to stay on track",
    txns: List<TxnRow> = emptyList(),
    onNavigateToEdit: (Long) -> Unit = {},
    onDeleteTxn: (TransactionEntity) -> Unit = {},
    onVoiceTap: () -> Unit = {},
    onCameraTap: () -> Unit = {},
    onManualTap: () -> Unit = {},
    onNotificationsTap: () -> Unit = {},
    unreadAlertsCount: Int = 0,
    onFabTap: () -> Unit = {},
    onViewAllTxns: () -> Unit = {},
    onQuickCategoryTap: (String) -> Unit = {},
    onNavigateToStreak: () -> Unit = {},
    onNavigateToSplit: (Long) -> Unit = {},
    coachMarkDismissed: Boolean = false,
    onDismissCoachMark: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(HomeTab.RECENT) }

    Scaffold(
        containerColor = ArthixColors.Background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ArthixColors.Background)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            item {
                HomeHeader(
                    userName = userName,
                    streakDays = streakDays,
                    onNotificationsTap = onNotificationsTap,
                    unreadAlertsCount = unreadAlertsCount
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                BalanceBlock(
                    balanceLabel = balanceLabel,
                    weekChangePercent = weekChangePercent,
                    streakDays = streakDays,
                    txnsLoggedThisWeek = txnsLoggedThisWeek,
                    onClick = onNavigateToStreak
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                CaptureActionRow(
                    onVoiceTap = onVoiceTap,
                    onCameraTap = onCameraTap,
                    onGridTap = onNavigateToStreak
                )
                Spacer(Modifier.height(12.dp))
            }

            // Slot 1: Coach mark — shown only when 0 transactions AND not yet dismissed
            if (txns.isEmpty() && !coachMarkDismissed) {
                item {
                    ShakeCoachMarkCard(onDismiss = onDismissCoachMark)
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                InsightBanner(headline = insightHeadline, body = insightBody)
                Spacer(Modifier.height(24.dp))
            }

            item {
                HomeTabRow(selected = tab, onSelect = { tab = it })
                Spacer(Modifier.height(16.dp))
            }

            when (tab) {
                HomeTab.QUICK_LOG -> item {
                    QuickLogGrid(categories = quickCategories, onTap = onQuickCategoryTap)
                }
                HomeTab.RECENT -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Transactions",
                                color = ArthixColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                            Text(
                                "View All →",
                                color = ArthixColors.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable(onClick = onViewAllTxns)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    if (txns.isEmpty()) {
                        item {
                            // Slot 2: Illustrated empty state replaces plain text
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(R.drawable.ill_voice_capture),
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No transactions yet",
                                    color = ArthixColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Log one with your voice or the camera",
                                    color = ArthixColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(txns, key = { it.id }) { txn ->
                            SwipeableTxnRow(
                                onEdit = { onNavigateToEdit(txn.id) },
                                onDelete = { txn.entity?.let { onDeleteTxn(it) } }
                            ) {
                                TxnListRow(
                                    txn = txn,
                                    onClick = { onNavigateToEdit(txn.id) },
                                    onSplitRequest = { onNavigateToSplit(txn.id) }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   HEADER — reference: avatar + "Welcome back" + ⋯ menu
   here: streak-ring avatar + greeting + notification bell
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun HomeHeader(userName: String, streakDays: Int, onNotificationsTap: () -> Unit, unreadAlertsCount: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StreakRingAvatar(initial = userName.take(1).uppercase(), streakDays = streakDays)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Good Morning,", color = ArthixColors.TextSecondary, fontSize = 13.sp)
                Text(userName, color = ArthixColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ArthixColors.Surface)
                .border(BorderStroke(1.dp, ArthixColors.Border), CircleShape)
                .clickable(onClick = onNotificationsTap),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = ArthixColors.TextPrimary, modifier = Modifier.size(20.dp))
            if (unreadAlertsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ArthixColors.Brand)
                )
            }
        }
    }
}

/** The "small four-box logo" streak indicator, now doubling as the
 *  avatar ring border — same idea as the reference's colored avatar ring,
 *  but built from your existing streak glyph instead of a random color. */
@Composable
private fun StreakRingAvatar(initial: String, streakDays: Int) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, ArthixColors.Brand), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(ArthixColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = ArthixColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        // four-box streak glyph, badge-style, bottom-right of the ring
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ArthixColors.Brand)
                .border(BorderStroke(2.dp, ArthixColors.Background), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            FourBoxGlyph()
        }
    }
}

@Composable
private fun FourBoxGlyph() {
    Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   BALANCE BLOCK — reference: $11,230.09 [+19.40%] / 104 Followers 326 Following
   here: ₹150.00 [week Δ%] / streak · txns logged this week
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun BalanceBlock(
    balanceLabel: String,
    weekChangePercent: Double,
    streakDays: Int,
    txnsLoggedThisWeek: Int,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text("Your Spend Today", color = ArthixColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                balanceLabel,
                color = ArthixColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )
            Spacer(Modifier.width(10.dp))
            ChangeBadge(percent = weekChangePercent)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    // #3: Flame vector icon replaces 🔥 emoji
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = ArthixColors.Brand,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$streakDays day streak",
                    color = ArthixColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "$txnsLoggedThisWeek logged this week",
                color = ArthixColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ChangeBadge(percent: Double) {
    val positive = percent >= 0
    val color = if (positive) ArthixColors.Positive else ArthixColors.Negative
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (positive) Icons.Filled.North else Icons.Filled.South,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            "${if (positive) "+" else ""}${"%.1f".format(percent)}% wk",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   CAPTURE ACTION ROW — reference: [+ Deposit] [↗ Withdraw] [⇄ Swap]
   here: [🎙 Voice] [📷 Camera] [⌨ Manual] — your three real capture modes
   (FR-3 voice, FR-4 camera OCR, FR-5 manual fallback), same 2-solid+1-icon
   pill layout as the reference row.
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun CaptureActionRow(onVoiceTap: () -> Unit, onCameraTap: () -> Unit, onGridTap: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionPill(
            label = "Voice",
            icon = Icons.Outlined.Mic,
            modifier = Modifier.weight(1f),
            filled = true,
            onClick = onVoiceTap
        )
        ActionPill(
            label = "Camera",
            icon = Icons.Outlined.CameraAlt,
            modifier = Modifier.weight(1f),
            filled = true,
            onClick = onCameraTap
        )
        ActionPill(
            label = null,
            icon = Icons.Outlined.GridView,
            modifier = Modifier.width(52.dp),
            filled = false,
            onClick = onGridTap
        )
    }
}

@Composable
private fun ActionPill(
    label: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (filled) ArthixColors.BrandGradient else Brush.linearGradient(listOf(ArthixColors.Surface, ArthixColors.Surface)))
            .then(if (!filled) Modifier.border(BorderStroke(1.dp, ArthixColors.Border), RoundedCornerShape(26.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = if (filled) Color.White else ArthixColors.TextSecondary, modifier = Modifier.size(18.dp))
            if (label != null) {
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   COACH MARK — Slot 1: ill_shake_gesture
   Shown only before the first transaction is logged AND not yet dismissed.
   Gates: txns.isEmpty() && !coachMarkDismissed (checked in LazyColumn).
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun ShakeCoachMarkCard(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArthixColors.SurfaceRaised)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ill_shake_gesture),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Try shaking after you pay",
                    color = ArthixColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "We'll catch the notification automatically.",
                    color = ArthixColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
        // Dismiss X
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(ArthixColors.Border)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = ArthixColors.TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   INSIGHT BANNER — reference: yellow "🏆 Rewards / You have 2 Airdrops /
   Available to claim" card with two character avatars on the right.
   here: same visual weight, but the copy is a REAL FR-7 agent sentence
   (grounded number + suggestion, per your report-generation spec) instead
   of decorative promo text — this is the one place a generic reward
   banner would be actively wrong for what this screen is showing.
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun InsightBanner(headline: String, body: String) {
    // #8: Flattened to solid surface color — the gradient was the only one in the app.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ArthixColors.SurfaceRaised)
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ArthixColors.Brand, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "This week's insight",
                    color = ArthixColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                headline,
                color = ArthixColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                color = ArthixColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   TAB ROW — reference: "Tokens  Collectibles"
   here: "Recent  Quick Log" — reordered so the list users check most
   (their own spend) is the default tab, Quick Log becomes the second one
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun HomeTabRow(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        TabLabel("Recent", selected == HomeTab.RECENT) { onSelect(HomeTab.RECENT) }
        TabLabel("Quick Log", selected == HomeTab.QUICK_LOG) { onSelect(HomeTab.QUICK_LOG) }
    }
}

@Composable
private fun TabLabel(label: String, isSelected: Boolean, onClick: () -> Unit) {
    // #4: Pill background for selected state — matches other selectable controls in the app
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected)
                    Modifier.background(ArthixColors.Brand.copy(alpha = 0.16f))
                else
                    Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 12.dp else 0.dp, vertical = if (isSelected) 5.dp else 0.dp)
    ) {
        Text(
            label,
            color = if (isSelected) ArthixColors.Brand else ArthixColors.TextMuted,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   QUICK LOG GRID — your existing 4 categories, restyled as tappable
   tinted-icon tiles rather than flat chips
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun QuickLogGrid(categories: List<QuickCategory>, onTap: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        categories.forEach { cat ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTap(cat.label) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cat.tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(cat.icon, contentDescription = cat.label, tint = cat.tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(cat.label, color = ArthixColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   TRANSACTION ROW — reference: [icon] Name TICKER / 24 VOL $X    $price ↗%
   here:               [icon] Payee / Category · time            ±₹amount
   Pending rows (status = awaiting_amount, per your data model) get a
   dashed treatment instead of a red/green amount, since "-₹0" reading as
   a real logged zero-rupee expense is misleading — this fixes that.
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun TxnListRow(txn: TxnRow, onClick: () -> Unit, onSplitRequest: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ArthixColors.Surface)
            .then(
                if (txn.status == TransactionStatus.AWAITING_AMOUNT || txn.status == TransactionStatus.AWAITING_CATEGORY)
                    Modifier.border(BorderStroke(1.dp, ArthixColors.Border), RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(txn.tint.copy(alpha = if (txn.status == TransactionStatus.AWAITING_AMOUNT || txn.status == TransactionStatus.AWAITING_CATEGORY) 0.10f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(txn.icon, contentDescription = null, tint = txn.tint, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.payee,
                color = if (txn.status == TransactionStatus.AWAITING_AMOUNT || txn.status == TransactionStatus.AWAITING_CATEGORY) ArthixColors.TextSecondary else ArthixColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${txn.time} · ${txn.category}",
                color = ArthixColors.TextMuted,
                fontSize = 12.sp
            )
        }

        com.chirag.arthix.ui.screen.split.SplitLauncherIcon(
            onClick = onSplitRequest
        )
        
        Spacer(Modifier.width(8.dp))

        if (txn.status == TransactionStatus.AWAITING_AMOUNT || txn.status == TransactionStatus.AWAITING_CATEGORY) {
            Text(
                "Needs review",
                color = ArthixColors.Pending,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                txn.amountLabel,
                color = if (txn.isInflow) ArthixColors.Positive else ArthixColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   NOTIFICATIONS BOTTOM SHEET
   ───────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsBottomSheet(alerts: List<AppAlert>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ArthixColors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ArthixColors.Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Notifications",
                color = ArthixColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = ArthixColors.Border,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "You're all caught up",
                            color = ArthixColors.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                alerts.forEach { alert ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ArthixColors.Surface)
                            .border(1.dp, ArthixColors.Border, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(alert.icon, contentDescription = null, tint = ArthixColors.Brand, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.title, color = ArthixColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(alert.message, color = ArthixColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
