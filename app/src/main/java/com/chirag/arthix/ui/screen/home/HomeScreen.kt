package com.chirag.arthix.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.ui.components.SkeletonLoader
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.LabelCaps
import com.chirag.arthix.ui.theme.SectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home dashboard screen — the primary landing screen.
 *
 * Layout matches Stitch design language:
 * - Hero spend amount at top (display-hero-mobile)
 * - Inflow/pending cards
 * - Category breakdown
 * - Recent transactions
 *
 * All data reads from live Room Flow — no hardcoded numbers (NFR-5).
 */
@Composable
fun HomeScreen(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val shapes = ArthixTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.marginX),
    ) {
        Spacer(Modifier.height(spacing.xl))

        // ── Greeting ────────────────────────────────────────────────
        Text(
            text = "Today's Overview",
            style = HeadlineLg,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.xs))
        Text(
            text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            style = BodySecondary,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(spacing.xl))

        // ── Hero spend amount ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, shapes.card)
                .padding(spacing.cardPadding),
        ) {
            Column {
                Text(
                    text = "SPENT TODAY",
                    style = LabelCaps,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(spacing.sm))
                if (uiState.isLoading) {
                    SkeletonLoader(
                        modifier = Modifier.height(44.dp).fillMaxWidth(0.5f),
                    )
                } else {
                    Text(
                        text = formatPaise(uiState.todaySpendPaise),
                        style = DisplayHeroMobile,
                        color = colors.accentSpend,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.gutter))

        // ── Inflow + Pending row ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter),
        ) {
            // Inflow card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.tagPosBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = colors.tagPosText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Text("Inflow", style = LabelCaps, color = colors.textSecondary)
                    }
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = formatPaise(uiState.todayInflowPaise),
                        style = SectionHeader,
                        color = colors.tagPosText,
                    )
                }
            }

            // Pending card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.accentWarning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.PendingActions,
                                contentDescription = null,
                                tint = colors.accentWarning,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Text("Pending", style = LabelCaps, color = colors.textSecondary)
                    }
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = "${uiState.pendingCount}",
                        style = SectionHeader,
                        color = colors.accentWarning,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.sectionGap))

        // ── Category breakdown ──────────────────────────────────────
        if (uiState.categoryBreakdown.isNotEmpty()) {
            Text(
                text = "Categories",
                style = SectionHeader,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.md))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem),
            ) {
                Column {
                    uiState.categoryBreakdown.entries.sortedByDescending { it.value }
                        .forEachIndexed { index, (category, amountPaise) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = category.replaceFirstChar { it.uppercase() },
                                    style = BodyPrimary,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = formatPaise(amountPaise),
                                    style = BodyPrimary,
                                    color = colors.accentSpend,
                                )
                            }
                            if (index < uiState.categoryBreakdown.size - 1) {
                                HorizontalDivider(color = colors.border, thickness = 1.dp)
                            }
                        }
                }
            }
        }

        Spacer(Modifier.height(spacing.sectionGap))

        // ── Recent transactions ─────────────────────────────────────
        if (uiState.recentTransactions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    style = SectionHeader,
                    color = colors.textPrimary,
                )
                Row(
                    modifier = Modifier.clickable(onClick = onNavigateToActivity),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "View all",
                        style = BodySecondary,
                        color = colors.textSecondary,
                    )
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(spacing.md))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem),
            ) {
                Column {
                    uiState.recentTransactions.forEachIndexed { index, txn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToEdit(txn.id) }
                                .padding(spacing.cardPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Left: icon + payee/category
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceIconChip),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        if (txn.direction == Direction.INFLOW)
                                            Icons.Default.TrendingUp
                                        else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (txn.direction == Direction.INFLOW)
                                            colors.tagPosText else colors.accentSpend,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(spacing.md))
                                Column {
                                    Text(
                                        text = txn.payee ?: txn.category?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                                        style = BodyPrimary,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = SimpleDateFormat("h:mm a", Locale.getDefault())
                                            .format(Date(txn.timestamp)),
                                        style = BodySecondary,
                                        color = colors.textSecondary,
                                    )
                                }
                            }

                            // Right: amount
                            Text(
                                text = (if (txn.direction == Direction.OUTFLOW) "- " else "+ ") +
                                        formatPaise(txn.amountPaise ?: 0L),
                                style = BodyPrimary,
                                color = if (txn.direction == Direction.INFLOW)
                                    colors.tagPosText else colors.accentSpend,
                            )
                        }
                        if (index < uiState.recentTransactions.lastIndex) {
                            HorizontalDivider(color = colors.border, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Empty state
        if (!uiState.isLoading && uiState.recentTransactions.isEmpty()) {
            Spacer(Modifier.height(spacing.xxl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No transactions yet",
                    style = SectionHeader,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = "Shake your phone after a payment or tap + to log manually",
                    style = BodySecondary,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(spacing.xxl))
    }
}

private fun formatPaise(paise: Long): String {
    val rupees = paise / 100.0
    return if (rupees == rupees.toLong().toDouble()) {
        "₹${rupees.toLong()}"
    } else {
        "₹${"%.2f".format(rupees)}"
    }
}
