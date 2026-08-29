package com.chirag.arthix.ui.screen.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.ui.components.ActivityListRow
import com.chirag.arthix.ui.components.EmptyState
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.StatusTag
import com.chirag.arthix.ui.components.confidenceTagConfig
import com.chirag.arthix.ui.components.statusTagConfig
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Display
import com.chirag.arthix.ui.theme.Label
import com.chirag.arthix.ui.theme.Title
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transaction history / Home screen (PRD §6.1 + §6.8).
 *
 * Layout mirrors Uber Home:
 * - Pill-shaped "quick log" bar at top (visual echo of "Where to?")
 * - Quick action row (Shake info, Camera — FR-4)
 * - Recent transactions as ActivityListRow list with StatusTag badges
 * - EmptyState when no transactions exist
 *
 * Every row shows status (EC-53) and confidence_flag badge (EC-15/22/30/32).
 */
@Composable
fun TransactionHistoryScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // ── Top bar: App title ──────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Arthix",
            style = Display,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))

        // ── Pill search / quick-log bar (PRD §6.1) ─────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Quick log…",
                style = Body,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Quick action chips (Shake + Camera) ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionChip(
                icon = Icons.Outlined.Vibration,
                label = "Shake",
                modifier = Modifier.weight(1f),
            )
            QuickActionChip(
                icon = Icons.Outlined.CameraAlt,
                label = "Camera",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Section header ──────────────────────────────────────────
        Text(
            text = "Recent Transactions",
            style = Title,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))

        // ── Transaction list or empty state ─────────────────────────
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.transactions,
                        key = { it.id },
                    ) { txn ->
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(txn.timestamp))

                        val amountStr = txn.amountPaise?.let { paise ->
                            val sign = if (txn.direction == Direction.INFLOW) "+" else "-"
                            val rupees = kotlin.math.abs(paise) / 100
                            val remainder = kotlin.math.abs(paise) % 100
                            "${sign}₹${rupees}.%02d".format(remainder)
                        } ?: "₹—"

                        val amountColor = when {
                            txn.direction == Direction.INFLOW -> colors.success
                            txn.status == TransactionStatus.DISCARDED -> colors.textSecondary
                            else -> colors.textPrimary
                        }

                        // Determine which tag to show — confidence takes priority
                        val confidenceConfig = confidenceTagConfig(txn.confidenceFlag)
                        val statusConfig = if (confidenceConfig == null && txn.status != TransactionStatus.CONFIRMED) {
                            statusTagConfig(txn.status)
                        } else null

                        ActivityListRow(
                            title = txn.payee ?: txn.category ?: txn.source.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            subtitle = "${txn.source.name.lowercase().replaceFirstChar { it.uppercase() }} · $dateStr",
                            amount = amountStr,
                            amountColor = amountColor,
                            statusTag = when {
                                confidenceConfig != null -> {
                                    { StatusTag(config = confidenceConfig) }
                                }
                                statusConfig != null -> {
                                    { StatusTag(config = statusConfig) }
                                }
                                else -> null
                            },
                            onClick = { onNavigateToEdit(txn.id) },
                        )
                    }

                    // Bottom spacing for FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.chipBg),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = Label.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}
