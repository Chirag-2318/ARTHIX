package com.chirag.arthix.ui.screen.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.LabelCaps
import com.chirag.arthix.ui.theme.SectionHeader

/**
 * Insights screen — spending trends, category breakdown, week-over-week comparison.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
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

        Text(
            text = "Insights",
            style = HeadlineLg,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(spacing.sectionGap))

        // ── This month hero ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, shapes.card)
                .padding(spacing.cardPadding),
        ) {
            Column {
                Text("THIS MONTH", style = LabelCaps, color = colors.textSecondary)
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = formatPaise(uiState.thisMonthSpendPaise),
                    style = DisplayHeroMobile,
                    color = colors.accentSpend,
                )
                
                Spacer(Modifier.height(spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val momText = if (uiState.moMPercentage > 0) {
                        "↑ ${String.format(java.util.Locale.US, "%.1f", uiState.moMPercentage)}% vs last month"
                    } else if (uiState.moMPercentage < 0) {
                        "↓ ${String.format(java.util.Locale.US, "%.1f", -uiState.moMPercentage)}% vs last month"
                    } else {
                        "— vs last month"
                    }
                    val momColor = if (uiState.moMPercentage > 0) colors.accentSpend else if (uiState.moMPercentage < 0) colors.tagPosText else colors.onSurfaceVariant
                    
                    Text(text = momText, style = BodySecondary, color = momColor)
                    Text(text = "Avg ${formatPaise(uiState.dailyAveragePaise)}/day", style = BodySecondary, color = colors.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(spacing.gutter))

        // ── Week comparison ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter),
        ) {
            // This week
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Text("THIS WEEK", style = LabelCaps, color = colors.textSecondary)
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = formatPaise(uiState.thisWeekSpendPaise),
                        style = SectionHeader,
                        color = colors.accentSpend,
                    )
                }
            }

            // Last week
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Text("LAST WEEK", style = LabelCaps, color = colors.textSecondary)
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = formatPaise(uiState.lastWeekSpendPaise),
                        style = SectionHeader,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.gutter))

        // ── Trend indicator ─────────────────────────────────────────
        if (uiState.lastWeekSpendPaise > 0L) {
            val delta = uiState.thisWeekSpendPaise - uiState.lastWeekSpendPaise
            val isUp = delta > 0
            val pct = (kotlin.math.abs(delta).toFloat() / uiState.lastWeekSpendPaise.toFloat() * 100).toInt()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isUp) colors.accentSpend.copy(alpha = 0.15f) else colors.tagPosBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isUp) colors.accentSpend else colors.tagPosText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(spacing.md))
                    Text(
                        text = "${if (isUp) "↑" else "↓"} $pct% vs last week",
                        style = BodyPrimary,
                        color = if (isUp) colors.accentSpend else colors.tagPosText,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.sectionGap))

        // ── Category breakdown ──────────────────────────────────────
        if (uiState.categoryBreakdown.isNotEmpty()) {
            Text(
                text = "Category Breakdown",
                style = SectionHeader,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.md))

            val maxAmount = uiState.categoryBreakdown.maxOf { cs -> cs.total }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.listItem),
            ) {
                Column {
                    uiState.categoryBreakdown.forEachIndexed { index, categorySum ->
                        Column(
                            modifier = Modifier.padding(spacing.cardPadding),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = (categorySum.category ?: "Uncategorized").replaceFirstChar { c -> c.uppercase() },
                                    style = BodyPrimary,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = formatPaise(categorySum.total),
                                    style = BodyPrimary,
                                    color = colors.accentSpend,
                                )
                            }
                            Spacer(Modifier.height(spacing.sm))
                            LinearProgressIndicator(
                                progress = { categorySum.total.toFloat() / maxAmount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(shapes.pill),
                                color = colors.accentSpend,
                                trackColor = colors.surfaceContainerHigh,
                            )
                        }
                        if (index < uiState.categoryBreakdown.lastIndex) {
                            HorizontalDivider(color = colors.border, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Empty state
        if (!uiState.isLoading && uiState.categoryBreakdown.isEmpty() && uiState.thisMonthSpendPaise == 0L) {
            Spacer(Modifier.height(spacing.xxl))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No data yet",
                    style = SectionHeader,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = "Start logging transactions to see spending insights",
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
