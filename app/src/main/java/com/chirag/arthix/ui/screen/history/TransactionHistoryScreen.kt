package com.chirag.arthix.ui.screen.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.ui.components.EmptyState
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.StatusTag
import com.chirag.arthix.ui.components.confidenceTagConfig
import com.chirag.arthix.ui.components.statusTagConfig
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.SectionHeader
import com.chirag.arthix.ui.theme.LabelCaps
import com.chirag.arthix.ui.components.DeleteTxnDialog
import com.chirag.arthix.ui.components.SwipeableTxnRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reuse the reference UI orange accent
private val ChartOrange = Color(0xFFF97316)
private val ChartDark = Color(0xFF2A2A2A)
private val CardBg = Color(0xFF141414)
private val ScreenBg = Color(0xFF0A0A0A)

/**
 * Transaction history / Activity screen - redesigned to match the reference UI.
 *
 * Layout:
 * - "Transaction Overview" header
 * - Account Statement bar chart (outflow vs inflow)
 * - Time filter pills (W / M / 6M / Y / All)
 * - Recent transaction list with category icons
 */
@Composable
fun TransactionHistoryScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToManualEntry: (com.chirag.arthix.ui.screen.manual.ManualEntryPrefill?) -> Unit = {},
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current

    var showVoiceCapture by remember { mutableStateOf(false) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val amount = data?.getStringExtra(com.chirag.arthix.ocr.ReceiptCaptureActivity.EXTRA_PREFILL_AMOUNT)
            val payee = data?.getStringExtra(com.chirag.arthix.ocr.ReceiptCaptureActivity.EXTRA_PREFILL_PAYEE)
            onNavigateToManualEntry(com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amount, payee = payee))
        }
    }

    if (showVoiceCapture) {
        com.chirag.arthix.ui.components.VoiceCaptureBottomSheet(
            sttEngine = viewModel.sttEngine,
            onDismiss = { showVoiceCapture = false },
            onResult = { prefill ->
                onNavigateToManualEntry(prefill)
            }
        )
    }

    uiState.transactionToDelete?.let { txn ->
        DeleteTxnDialog(
            amountPaise = txn.amountPaise,
            payee = txn.payee,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDelete() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(ScreenBg),
    ) {
        Spacer(Modifier.height(16.dp))

        // ══════════════════════════════════════════════════════════════
        // 1. HEADER
        // ══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Transaction Overview",
                style = SectionHeader.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardBg)
                    .clickable { /* filter options */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Filter",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ══════════════════════════════════════════════════════════════
        // 2. ACCOUNT STATEMENT CHART CARD
        // ══════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
        ) {
            Text(
                text = "Account Statement",
                style = SectionHeader.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.height(20.dp))

            // Bar chart
            if (uiState.chartBars.isNotEmpty()) {
                SpendingBarChart(
                    bars = uiState.chartBars,
                    maxPaise = uiState.chartMaxPaise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            } else {
                // Placeholder when no data
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No spending data yet",
                        style = BodySecondary,
                        color = Color(0xFF6E6E73),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LegendDot(color = ChartOrange, label = "Outflow")
                LegendDot(color = Color(0xFF4ADE80), label = "Inflow")
            }

            Spacer(Modifier.height(16.dp))

            // Time filter pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChartTimeFilter.entries.forEach { filter ->
                    val isSelected = uiState.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ChartOrange else Color(0xFF1E1E1E))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = filter.label,
                            style = LabelCaps.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) Color.White else Color(0xFF6E6E73),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ══════════════════════════════════════════════════════════════
        // 3. QUICK ACTIONS ROW — #5: Styled as filled orange pills to match Home screen
        // ══════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val pillGradient = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFFFF9142), Color(0xFFFF5B3D))
            )
            // Voice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(pillGradient)
                    .clickable { showVoiceCapture = true }
                    .padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Voice", style = BodySecondary.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
            // Camera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(pillGradient)
                    .clickable {
                        cameraLauncher.launch(com.chirag.arthix.ocr.ReceiptCaptureActivity.createIntent(context))
                    }
                    .padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Camera", style = BodySecondary.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
            // Manual
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(pillGradient)
                    .clickable { onNavigateToManualEntry(null) }
                    .padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Outlined.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manual", style = BodySecondary.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ══════════════════════════════════════════════════════════════
        // 4. TRANSACTION SECTION HEADER
        // ══════════════════════════════════════════════════════════════
        Text(
            text = "Recent Transactions",
            style = SectionHeader.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))

        // ══════════════════════════════════════════════════════════════
        // 5. TRANSACTION LIST
        // ══════════════════════════════════════════════════════════════
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.chirag.arthix.ui.components.SkeletonLoader()
                }
            }
            uiState.transactions.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    headline = "No transactions yet",
                    subtext = "Shake your phone after a payment, or tap + to log manually.",
                    actionButton = {
                        PrimaryButton(
                            text = "Set up permissions",
                            onClick = onNavigateToOnboarding,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(
                        items = uiState.transactions,
                        key = { it.id },
                    ) { txn ->
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(txn.timestamp))

                        val amountPaise = txn.amountPaise ?: 0L
                        val rupees = amountPaise / 100
                        val remainder = amountPaise % 100
                        val sign = if (txn.direction == Direction.INFLOW) "+" else "-"
                        val amountStr = "$sign\u20b9$rupees.%02d".format(remainder)

                        val amountColor = when {
                            txn.direction == Direction.INFLOW -> Color(0xFF22C55E)
                            txn.status == TransactionStatus.DISCARDED -> Color(0xFF6E6E73)
                            else -> Color(0xFFEF4444)
                        }

                        val categoryColor = getCategoryColor(txn.category)

                        // Tag logic
                        val confidenceConfig = confidenceTagConfig(txn.confidenceFlag)
                        val statusConfig = if (confidenceConfig == null && txn.status != TransactionStatus.CONFIRMED) {
                            statusTagConfig(txn.status)
                        } else null

                        SwipeableTxnRow(
                            onEdit = { onNavigateToEdit(txn.id) },
                            onDelete = { viewModel.requestDelete(txn) }
                        ) {
                            DarkTransactionRow(
                                payee = txn.payee ?: txn.category?.replaceFirstChar { it.uppercase() }
                                    ?: txn.source.name.lowercase().replaceFirstChar { it.uppercase() },
                                subtitle = "${txn.source.name.lowercase().replaceFirstChar { it.uppercase() }} \u00b7 $dateStr",
                                amount = amountStr,
                                amountColor = amountColor,
                                categoryColor = categoryColor,
                                isInflow = txn.direction == Direction.INFLOW,
                                statusTag = when {
                                    confidenceConfig != null -> {{ StatusTag(config = confidenceConfig) }}
                                    statusConfig != null -> {{ StatusTag(config = statusConfig) }}
                                    else -> null
                                },
                                onClick = { onNavigateToEdit(txn.id) },
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFF1E1E1E),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    // Bottom spacing for FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// BAR CHART
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SpendingBarChart(
    bars: List<ChartBarData>,
    maxPaise: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // The chart itself
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    // Draw bars
                    val maxBarHeight = 100f
                    val outflowHeight = if (maxPaise > 0) (bar.outflowPaise.toFloat() / maxPaise * maxBarHeight) else 0f
                    val inflowHeight = if (maxPaise > 0) (bar.inflowPaise.toFloat() / maxPaise * maxBarHeight) else 0f

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(100.dp)
                    ) {
                        val barWidth = size.width * 0.35f
                        val spacing = size.width * 0.1f
                        val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                        // Outflow bar (orange) - left
                        val outH = (outflowHeight / maxBarHeight * size.height).coerceAtLeast(if (bar.outflowPaise > 0) 4f else 0f)
                        drawRoundRect(
                            color = ChartOrange,
                            topLeft = Offset(
                                x = (size.width / 2 - barWidth - spacing / 2),
                                y = size.height - outH
                            ),
                            size = Size(barWidth, outH),
                            cornerRadius = cornerRadius,
                        )

                        // Inflow bar (dark/green) - right
                        val inH = (inflowHeight / maxBarHeight * size.height).coerceAtLeast(if (bar.inflowPaise > 0) 4f else 0f)
                        drawRoundRect(
                            color = Color(0xFF2A2A2A),
                            topLeft = Offset(
                                x = (size.width / 2 + spacing / 2),
                                y = size.height - inH
                            ),
                            size = Size(barWidth, inH),
                            cornerRadius = cornerRadius,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Day label
                    Text(
                        text = bar.label,
                        style = LabelCaps.copy(fontSize = 10.sp),
                        color = Color(0xFF6E6E73),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = BodySecondary.copy(fontSize = 12.sp),
            color = Color(0xFFA8A8A8),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// TRANSACTION ROW (Dark theme)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DarkTransactionRow(
    payee: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    categoryColor: Color,
    isInflow: Boolean,
    statusTag: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isInflow) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = payee,
                    style = BodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (statusTag != null) {
                    Spacer(Modifier.width(6.dp))
                    statusTag()
                }
            }
            Text(
                text = subtitle,
                style = BodySecondary.copy(fontSize = 12.sp),
                color = Color(0xFF6E6E73),
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = amount,
            style = BodyPrimary.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            color = amountColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════

private fun getCategoryColor(category: String?): Color {
    return when (category?.lowercase()) {
        "food", "food & dining", "dining" -> Color(0xFFFF8A65)
        "travel", "transport" -> Color(0xFF4FC3F7)
        "shopping" -> Color(0xFFBA68C8)
        "grocery", "groceries" -> Color(0xFF81C784)
        "entertainment" -> Color(0xFFFFD54F)
        "bills", "utilities" -> Color(0xFF90A4AE)
        else -> Color(0xFFF97316)
    }
}
