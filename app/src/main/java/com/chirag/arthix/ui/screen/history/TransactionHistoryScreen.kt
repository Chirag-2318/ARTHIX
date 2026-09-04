package com.chirag.arthix.ui.screen.history

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.South
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.ui.components.DeleteTxnDialog
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

// Light theme colors
private val BrandCoral = Color(0xFFE4463A)
private val BrandSage = Color(0xFF8BA888)
private val CardBg = Color.White
private val ScreenBg = Color(0xFFFAF7F2)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6E6E73)

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
    onNavigateToSplit: (Long) -> Unit = {},
    onNavigateToSplitWithPrefill: (com.chirag.arthix.ui.screen.split.SplitPrefill) -> Unit = {},
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
            onVoiceIntent = { intent, transcript ->
                if (intent is com.chirag.arthix.voice.VoiceIntent.Split) {
                    onNavigateToSplitWithPrefill(
                        com.chirag.arthix.ui.screen.split.SplitPrefill(
                            amountPaise = intent.amountPaise,
                            payee = intent.payee ?: intent.names.firstOrNull(),
                            category = intent.category,
                            participantNames = intent.names
                        )
                    )
                } else {
                    val prefill = when (intent) {
                        is com.chirag.arthix.voice.VoiceIntent.CategoryAndAmount -> {
                            val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                            com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(
                                amount = amountStr,
                                category = intent.category,
                                payee = intent.payee,
                            )
                        }
                        is com.chirag.arthix.voice.VoiceIntent.Amount -> {
                            val amountStr = if (intent.amountPaise % 100 == 0L) "${intent.amountPaise / 100}" else String.format(java.util.Locale.US, "%.2f", intent.amountPaise / 100.0)
                            com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(
                                amount = amountStr,
                                payee = intent.payee,
                            )
                        }
                        is com.chirag.arthix.voice.VoiceIntent.Category -> com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(
                            category = intent.category,
                            payee = intent.payee,
                        )
                        else -> com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(payee = transcript)
                    }
                    onNavigateToManualEntry(prefill)
                }
            },
            onResult = { prefill ->
                if (!prefill.splitNames.isNullOrEmpty()) {
                    val paise = prefill.amount?.toDoubleOrNull()?.let { (it * 100).toLong() }
                    onNavigateToSplitWithPrefill(
                        com.chirag.arthix.ui.screen.split.SplitPrefill(
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

    uiState.transactionToDelete?.let { txn ->
        DeleteTxnDialog(
            amountPaise = txn.amountPaise,
            payee = txn.payee,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDelete() }
        )
    }

    androidx.compose.material3.Scaffold(
        containerColor = ScreenBg,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                color = TextPrimary,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0x1A000000))
                    .clip(CircleShape)
                    .background(CardBg)
                    .clickable { /* filter options */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Filter",
                    tint = TextPrimary,
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
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
        ) {
            Text(
                text = "Account Statement",
                style = SectionHeader.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
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
                LegendDot(color = BrandCoral, label = "Outgoing")
                LegendDot(color = BrandSage, label = "Incoming")
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
                            .background(if (isSelected) BrandCoral else Color(0xFFF0F0F0))
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
                            color = if (isSelected) Color.White else TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // (Quick Actions Row removed)


        // ══════════════════════════════════════════════════════════════
        // 4. TRANSACTION SECTION HEADER
        // ══════════════════════════════════════════════════════════════
        Text(
            text = "Recent Transactions",
            style = SectionHeader.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))

        // List Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TxnListFilter.entries.forEach { filter ->
                val isSelected = uiState.listFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) BrandCoral else Color(0xFFF0F0F0))
                        .clickable { viewModel.setListFilter(filter) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filter.label,
                        style = BodySecondary.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) Color.White else TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = uiState.transactions,
                        key = { it.id },
                    ) { txn ->
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(txn.timestamp))

                        val amountPaise = txn.amountPaise
                        val isAwaitingAmount = txn.status == TransactionStatus.AWAITING_AMOUNT
                        val amountStr = if (amountPaise == null || isAwaitingAmount) {
                            ""
                        } else {
                            val rupees = amountPaise / 100
                            val remainder = amountPaise % 100
                            val sign = if (txn.direction == Direction.INFLOW) "+" else "-"
                            "$sign\u20b9$rupees.%02d".format(remainder)
                        }

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
                            LightTransactionRow(
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
                                splitCount = uiState.splitParticipantCounts[txn.id] ?: 0,
                                onSplitRequest = { onNavigateToSplit(txn.id) },
                                onClick = { onNavigateToEdit(txn.id) },
                            )
                        }
                    }

                    // Bottom spacing for FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
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
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    
    // Find peak bar to label
    val maxBarValue = bars.maxOfOrNull { maxOf(it.outflowPaise, it.inflowPaise) } ?: 0L
    val peakBarIndex = if (maxBarValue > 0) bars.indexOfFirst { it.outflowPaise == maxBarValue || it.inflowPaise == maxBarValue } else -1

    Box(modifier = modifier) {
        // Gridlines & Y-axis labels
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), // space for x-axis labels
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val gridColor = Color(0xFFE5E5EA)
            val textColor = Color(0xFFA8A8A8)
            
            val levels = listOf(maxPaise, maxPaise / 2, 0L)
            levels.forEach { level ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (level == 0L) "₹0" else "₹${level / 100}",
                        color = textColor,
                        fontSize = 10.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(gridColor))
                }
            }
        }

        // Bars
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 44.dp, bottom = 24.dp), // Offset by y-axis width
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEachIndexed { index, bar ->
                val outflowAnim by animateFloatAsState(
                    targetValue = if (maxPaise > 0) (bar.outflowPaise.toFloat() / maxPaise) else 0f,
                    animationSpec = tween(durationMillis = 500)
                )
                val inflowAnim by animateFloatAsState(
                    targetValue = if (maxPaise > 0) (bar.inflowPaise.toFloat() / maxPaise) else 0f,
                    animationSpec = tween(durationMillis = 500)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Tooltip or Peak Label
                    if (selectedBarIndex == index) {
                        val total = bar.outflowPaise + bar.inflowPaise
                        val tooltipText = if (total == 0L) "No activity" else "₹${total / 100}"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tooltipText, color = Color.White, fontSize = 9.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    } else if (index == peakBarIndex && selectedBarIndex == null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrandCoral.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("₹${maxBarValue / 100}", color = BrandCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Spacer(Modifier.height(20.dp)) // Maintain height to prevent jumping
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .weight(1f, fill = false) // fill=false allows canvas to dictate height but it will fill parent height due to Box constraints. We should just let Canvas fillMaxSize
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { 
                                        selectedBarIndex = if (selectedBarIndex == index) null else index 
                                    }
                                )
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            val barWidth = size.width * 0.4f
                            val spacing = size.width * 0.1f
                            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            
                            val hasActivity = bar.outflowPaise > 0 || bar.inflowPaise > 0
                            
                            if (!hasActivity) {
                                // Zero-activity baseline mark
                                drawRoundRect(
                                    color = Color(0xFFD1D1D6),
                                    topLeft = Offset(size.width / 2 - barWidth - spacing / 2, size.height - 4f),
                                    size = Size(barWidth * 2 + spacing, 4f),
                                    cornerRadius = cornerRadius
                                )
                            } else {
                                val outH = (outflowAnim * size.height).coerceAtLeast(if (bar.outflowPaise > 0) 4f else 0f)
                                drawRoundRect(
                                    color = BrandCoral,
                                    topLeft = Offset(size.width / 2 - barWidth - spacing / 2, size.height - outH),
                                    size = Size(barWidth, outH),
                                    cornerRadius = cornerRadius,
                                )

                                val inH = (inflowAnim * size.height).coerceAtLeast(if (bar.inflowPaise > 0) 4f else 0f)
                                drawRoundRect(
                                    color = BrandSage,
                                    topLeft = Offset(size.width / 2 + spacing / 2, size.height - inH),
                                    size = Size(barWidth, inH),
                                    cornerRadius = cornerRadius,
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    style = LabelCaps.copy(fontSize = 10.sp),
                    color = Color(0xFF6E6E73),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
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
// TRANSACTION ROW (Light theme)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LightTransactionRow(
    payee: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    categoryColor: Color,
    isInflow: Boolean,
    statusTag: @Composable (() -> Unit)? = null,
    splitCount: Int = 0,
    onSplitRequest: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
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
                    color = TextPrimary,
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
            if (splitCount > 0 && onSplitRequest != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBE8)) // PastelCoral
                        .clickable(onClick = onSplitRequest)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Split: $splitCount",
                        color = Color(0xFFE4463A), // Coral
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        if (amount.isEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE5E5EA)) // Distinct grey placeholder background
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Add amount",
                    fontSize = 12.sp,
                    color = Color(0xFF6E6E73),
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = amount,
                style = BodyPrimary.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = amountColor,
                maxLines = 1,
            )
        }
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
        "salary" -> Color(0xFF34D399)
        "refund" -> Color(0xFF60A5FA)
        "gift" -> Color(0xFFF472B6)
        "interest" -> Color(0xFFA78BFA)
        else -> Color(0xFFF97316)
    }
}
