package com.chirag.arthix.ui.screen.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.ui.screen.split.SplitPrefill
import com.chirag.arthix.ocr.ReceiptCaptureActivity
import com.chirag.arthix.ui.components.DeleteTxnDialog
import com.chirag.arthix.ui.components.SwipeableTxnRow
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.ui.screen.manual.ManualEntryPrefill
import com.chirag.arthix.voice.VoiceIntent
import java.text.SimpleDateFormat
import java.util.*

/* ─────────────────────────────────────────────────────────────────────────
   LIGHT THEME COLOR TOKENS — warm cream/coral system matching
   the account-creation and permission-flow redesigns.
   ───────────────────────────────────────────────────────────────────── */

private object HomeColors {
    val Background = Color(0xFFFAF7F2)       // soft cream
    val Surface = Color(0xFFFFFFFF)           // white cards
    val SurfaceWarm = Color(0xFFFFF8F5)       // warm tinted card
    val TextPrimary = Color(0xFF1A1A1C)       // near-black
    val TextSecondary = Color(0xFF6B6B75)     // muted gray
    val TextMuted = Color(0xFF9A9AA5)         // lighter muted

    val Brand = Color(0xFFE4463A)             // coral-red accent
    val BrandLight = Color(0xFFFFF0EE)        // very light coral
    val Positive = Color(0xFF34A853)          // green inflow
    val Negative = Color(0xFFE4463A)          // red outflow
    val Pending = Color(0xFFB0A090)           // warm pending

    val PastelBlush = Color(0xFFFFE8E5)
    val PastelSage = Color(0xFFE5F5E0)
    val PastelSky = Color(0xFFE5F0FF)
    val PastelCream = Color(0xFFFFF5E5)
    val PastelLavender = Color(0xFFF0E8FF)

    val CardBorder = Color(0xFFF0EDE8)        // soft border
    val ChartFill = Color(0xFFE4463A).copy(alpha = 0.12f)
    val ChartStroke = Color(0xFFE4463A)
}

private data class QuickCategory(val label: String, val icon: ImageVector, val tint: Color, val bg: Color)

private val quickCategories = listOf(
    QuickCategory("Food", Icons.Filled.Restaurant, Color(0xFFE4463A), HomeColors.PastelBlush),
    QuickCategory("Travel", Icons.Filled.Flight, Color(0xFF3A7BE4), HomeColors.PastelSky),
    QuickCategory("Shopping", Icons.Filled.ShoppingBag, Color(0xFF8B5CF6), HomeColors.PastelLavender),
    QuickCategory("Groceries", Icons.Filled.ShoppingCart, Color(0xFF34A853), HomeColors.PastelSage),
)

private data class TxnRow(
    val id: Long,
    val payee: String,
    val category: String,
    val time: String,
    val amountLabel: String,
    val isInflow: Boolean,
    val status: TransactionStatus,
    val icon: ImageVector,
    val tint: Color,
    val bgTint: Color,
    val splitCount: Int = 0,
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
    onNavigateToGoals: () -> Unit = {},
    onAddGoal: () -> Unit = {},
    onNavigateToSplit: (Long) -> Unit = {},
    onNavigateToSplitList: () -> Unit = {},
    onNavigateToSplitWithPrefill: (SplitPrefill) -> Unit = {},
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
            onVoiceIntent = { intent, transcript ->
                if (intent is VoiceIntent.Split) {
                    onNavigateToSplitWithPrefill(
                        SplitPrefill(
                            amountPaise = intent.amountPaise,
                            payee = intent.payee ?: intent.names.firstOrNull(),
                            category = intent.category,
                            participantNames = intent.names
                        )
                    )
                } else {
                    val prefill = when (intent) {
                        is VoiceIntent.CategoryAndAmount -> {
                            val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                            ManualEntryPrefill(
                                amount = amountStr,
                                category = intent.category,
                                payee = intent.payee,
                                direction = intent.direction,
                            )
                        }
                        is VoiceIntent.Amount -> {
                            val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                            ManualEntryPrefill(
                                amount = amountStr,
                                payee = intent.payee,
                                direction = intent.direction,
                            )
                        }
                        is VoiceIntent.Category -> ManualEntryPrefill(
                            category = intent.category,
                            payee = intent.payee,
                            direction = intent.direction,
                        )
                        else -> ManualEntryPrefill(payee = transcript)
                    }
                    onNavigateToManualEntry(prefill)
                }
            },
            onResult = { prefill ->
                if (!prefill.splitNames.isNullOrEmpty()) {
                    val paise = prefill.amount?.toDoubleOrNull()?.let { (it * 100).toLong() }
                    onNavigateToSplitWithPrefill(
                        SplitPrefill(
                            amountPaise = paise,
                            payee = prefill.payee,
                            category = prefill.category,
                            participantNames = prefill.splitNames
                        )
                    )
                } else {
                    onNavigateToManualEntry(prefill)
                }
            }
        )
    }

    val mappedTxns = uiState.recentTransactions.map { txn ->
        val isPending = txn.status == TransactionStatus.AWAITING_MATCH || txn.status == TransactionStatus.AWAITING_CATEGORY || txn.status == TransactionStatus.AWAITING_AMOUNT

        val categoryLabel = txn.category ?: ""
        val matchedCategory = quickCategories.find { it.label.equals(categoryLabel, ignoreCase = true) }
        val categoryIcon = matchedCategory?.icon ?: Icons.Filled.ReceiptLong
        val categoryTint = matchedCategory?.tint ?: HomeColors.TextMuted
        val categoryBg = matchedCategory?.bg ?: HomeColors.PastelCream

        TxnRow(
            id = txn.id,
            payee = txn.payee ?: txn.category?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            category = txn.category?.replaceFirstChar { it.uppercase() } ?: "Uncategorized",
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(txn.timestamp)),
            amountLabel = if (txn.status == TransactionStatus.AWAITING_AMOUNT) "" else if (isPending) "—" else (if (txn.direction == Direction.OUTFLOW) "- " else "+ ") + formatPaise(txn.amountPaise ?: 0L),
            isInflow = txn.direction == Direction.INFLOW,
            status = txn.status,
            icon = categoryIcon,
            tint = categoryTint,
            bgTint = categoryBg,
            splitCount = uiState.splitParticipantCounts[txn.id] ?: 0,
            entity = txn
        )
    }

    // Main Screen Layout
    Scaffold(
        containerColor = HomeColors.Background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeColors.Background)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 16.dp, 
                bottom = 100.dp
            )
        ) {
            // 1. Greeting Header
            item {
                GreetingHeader(
                    userName = uiState.userName,
                    profileAvatar = uiState.profileAvatar,
                    onNotificationsTap = { showNotifications = true },
                    unreadAlertsCount = uiState.unreadAlertsCount
                )
                Spacer(Modifier.height(24.dp))
            }


            // 2. Balance/Spend Hero Card
            item {
                SpendHeroCard(
                    balanceLabel = formatPaise(uiState.todaySpendPaise),
                    weekChangePercent = uiState.weekChangePercent,
                    streakDays = uiState.streakDays,
                    txnsLoggedThisWeek = uiState.txnsLoggedThisWeek,
                    onAddExpense = { onNavigateToManualEntry(null) },
                    onNavigateToSplitList = { onNavigateToSplitList() },
                    onStreakTap = onNavigateToStreak,
                )
                Spacer(Modifier.height(20.dp))
            }

            // 3. Insight Card
            item {
                InsightCard(
                    headline = uiState.insightHeadline,
                    body = uiState.insightBody
                )
                Spacer(Modifier.height(20.dp))
            }

            // 4. Compact Spend Chart
            if (uiState.dailySpendData.isNotEmpty()) {
                item {
                    CompactSpendChart(dailySpend = uiState.dailySpendData)
                    Spacer(Modifier.height(24.dp))
                }
            }

            // 5. Savings Goals
            item {
                com.chirag.arthix.ui.components.GoalHomeCard(
                    activeGoals = uiState.activeGoals,
                    onNavigateToGoals = onNavigateToGoals,
                    onAddGoal = onAddGoal
                )
                Spacer(Modifier.height(20.dp))
            }

            // 6. Recent Transactions Header + Discarded Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        color = HomeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "View All →",
                        color = HomeColors.Brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onNavigateToActivity)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Discarded chip
            if (uiState.discardedCount > 0) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(HomeColors.PastelCream)
                            .clickable(onClick = onNavigateToActivity)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Discarded (${uiState.discardedCount})",
                            color = HomeColors.Pending,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Transaction list
            if (mappedTxns.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = HomeColors.TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No transactions yet",
                            color = HomeColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Log one with your voice or shake your phone",
                            color = HomeColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(mappedTxns, key = { it.id }) { txn ->
                    SwipeableTxnRow(
                        onEdit = { onNavigateToEdit(txn.id) },
                        onDelete = { txn.entity?.let { viewModel.requestDelete(it) } }
                    ) {
                        TransactionCard(
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

    if (showNotifications) {
        NotificationsSheet(
            alerts = uiState.alerts,
            onDismiss = { showNotifications = false }
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   1. GREETING HEADER
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun GreetingHeader(userName: String, profileAvatar: String?, onNotificationsTap: () -> Unit, unreadAlertsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape)
                    .background(HomeColors.BrandLight),
                contentAlignment = Alignment.Center
            ) {
                if (!profileAvatar.isNullOrBlank()) {
                    val context = LocalContext.current
                    val resId = context.resources.getIdentifier(profileAvatar.removeSuffix(".png"), "drawable", context.packageName)
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = resId),
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        val imageModel = when {
                            profileAvatar.startsWith("content://") -> android.net.Uri.parse(profileAvatar)
                            profileAvatar.startsWith("/") -> java.io.File(profileAvatar)
                            else -> profileAvatar
                        }
                        coil.compose.AsyncImage(
                            model = imageModel,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        userName.take(1).uppercase(),
                        color = HomeColors.Brand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    getGreeting(),
                    color = HomeColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    userName,
                    color = HomeColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }

        // Notification bell
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onNotificationsTap),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = HomeColors.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            if (unreadAlertsCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HomeColors.Brand)
                )
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning,"
        hour < 17 -> "Good Afternoon,"
        else -> "Good Evening,"
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   2. SPEND HERO CARD — balance, change badge, quick actions
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun SpendHeroCard(
    balanceLabel: String,
    weekChangePercent: Double,
    streakDays: Int,
    txnsLoggedThisWeek: Int,
    onAddExpense: () -> Unit,
    onNavigateToSplitList: () -> Unit,
    onStreakTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = HomeColors.Brand.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Your Spend Today", color = HomeColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    balanceLabel,
                    color = HomeColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
                Spacer(Modifier.width(10.dp))
                ChangeBadge(percent = weekChangePercent)
            }

            Spacer(Modifier.height(12.dp))

            // Streak + logged this week
            Row(
                modifier = Modifier.clickable(onClick = onStreakTap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$streakDays day streak",
                    color = HomeColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "$txnsLoggedThisWeek logged this week",
                    color = HomeColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Quick action pills inside the card
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Add Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(HomeColors.TextPrimary)
                        .clickable(onClick = onAddExpense),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Expense", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                // Splits
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, HomeColors.CardBorder, RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToSplitList),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Group, contentDescription = null, tint = HomeColors.TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Splits", color = HomeColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeBadge(percent: Double) {
    val positive = percent >= 0
    val color = if (positive) HomeColors.Positive else HomeColors.Negative
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
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
   3. INSIGHT CARD — soft pastel gradient, not flat dark box
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun InsightCard(headline: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(HomeColors.PastelCream, HomeColors.PastelSage),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 400f)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "This week's insight",
                    color = HomeColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                headline,
                color = HomeColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                color = HomeColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   4. COMPACT SPEND CHART — soft area chart in coral palette
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun CompactSpendChart(dailySpend: List<Pair<String, Long>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HomeColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "This Week",
                color = HomeColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            val maxSpend = dailySpend.maxOfOrNull { it.second } ?: 1L

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val w = size.width
                val h = size.height
                val stepX = w / (dailySpend.size - 1).coerceAtLeast(1)
                val padding = 4.dp.toPx()

                if (dailySpend.size < 2) return@Canvas

                // Build path
                val linePath = Path()
                val fillPath = Path()

                dailySpend.forEachIndexed { index, (_, spend) ->
                    val x = index * stepX
                    val y = h - padding - ((spend.toFloat() / maxSpend.toFloat()) * (h - padding * 2))

                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                // Close fill path
                fillPath.lineTo((dailySpend.size - 1) * stepX, h)
                fillPath.close()

                // Draw fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HomeColors.ChartFill,
                            HomeColors.ChartFill.copy(alpha = 0.02f)
                        )
                    )
                )

                // Draw line
                drawPath(
                    path = linePath,
                    color = HomeColors.ChartStroke,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailySpend.forEach { (label, _) ->
                    Text(
                        label,
                        color = HomeColors.TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   5. TRANSACTION CARD — pastel icon circle, clean layout, pill tag
   ───────────────────────────────────────────────────────────────────── */

@Composable
private fun TransactionCard(txn: TxnRow, onClick: () -> Unit, onSplitRequest: () -> Unit = {}) {
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(16.dp))
            .background(HomeColors.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon in pastel circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(txn.bgTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(txn.icon, contentDescription = null, tint = txn.tint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.payee,
                color = HomeColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${txn.time} · ${txn.category}",
                color = HomeColors.TextMuted,
                fontSize = 12.sp
            )
            if (txn.splitCount > 0) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBE8))
                        .clickable(onClick = onSplitRequest)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Split: ${txn.splitCount}",
                        color = Color(0xFFE4463A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (txn.status == TransactionStatus.AWAITING_AMOUNT || txn.status == TransactionStatus.AWAITING_CATEGORY) {
                // "Needs review" pill tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE5E5EA))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (txn.status == TransactionStatus.AWAITING_AMOUNT) "Add amount" else "Add category",
                        color = Color(0xFF6E6E73),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    txn.amountLabel,
                    color = if (txn.isInflow) HomeColors.Positive else HomeColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────
   NOTIFICATIONS SHEET
   ───────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsSheet(alerts: List<AppAlert>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HomeColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = HomeColors.CardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Notifications",
                color = HomeColors.TextPrimary,
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
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "You're all caught up",
                            color = HomeColors.TextSecondary,
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(HomeColors.SurfaceWarm)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(HomeColors.BrandLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    alert.icon,
                                    contentDescription = null,
                                    tint = HomeColors.Brand,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    alert.title,
                                    color = HomeColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    alert.message,
                                    color = HomeColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCategorizePill(
    pendingCount: Int = 1,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFFFFF),
        modifier = Modifier
            .scale(scalePulse)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(50),
                ambientColor = Color(0x33000000),
                spotColor = Color(0x11000000)
            )
            .border(
                width = 1.5.dp,
                color = Color(0xFFE4463A).copy(alpha = glowAlpha),
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // Bolt Icon in coral
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = "Needs Categorization",
                tint = Color(0xFFE4463A),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "Categorize",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1C)
            )

            if (pendingCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4463A))
                ) {
                    Text(
                        text = pendingCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF6B6B75),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
