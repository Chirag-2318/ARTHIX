package com.chirag.arthix.ui.screen.report

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
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
import com.chirag.arthix.ui.components.EmptyState
import com.chirag.arthix.ui.components.InsightCard
import com.chirag.arthix.ui.components.SkeletonLoader
import com.chirag.arthix.ui.theme.AmountHero
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Display
import com.chirag.arthix.ui.theme.Label
import com.chirag.arthix.ui.theme.Title

/**
 * Report screen (PRD §6.7).
 *
 * Layout:
 * - PromoBanner-style headline card: projected total as AmountHero,
 *   suggestion line beneath it
 * - Stacked InsightCards: category breakdown, projection stats
 * - Pending/uncategorized total ALWAYS visible (EC-44)
 * - Zero-baseline shows explicit "No prior data" (EC-45), never NaN/∞%
 * - SkeletonLoader while generating (NFR-4, up to 15s)
 * - EmptyState when no data exists
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // ── Title ───────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your Report",
            style = Display,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                SkeletonLoader(modifier = Modifier.fillMaxSize())
            }
            uiState.error != null -> {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    headline = "Report generation failed",
                    subtext = uiState.error ?: "An unknown error occurred.",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            uiState.report != null -> {
                val report = uiState.report!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 4.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── Hero card — projected total ──────────────────
                    item {
                        HeroReportCard(
                            periodLabel = report.periodLabel,
                            netFlowPaise = report.netFlowPaise,
                            suggestion = report.suggestions.firstOrNull(),
                        )
                    }

                    // ── Category breakdown ───────────────────────────
                    if (report.categoryBreakdown.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.Assessment,
                                headline = "No spending logged",
                                subtext = "Start logging transactions to see your breakdown here.",
                            )
                        }
                    } else {
                        item {
                            InsightCard(
                                title = "Spending by Category",
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    report.categoryBreakdown.entries
                                        .sortedByDescending { it.value }
                                        .forEach { (cat, paise) ->
                                            CategoryBreakdownRow(
                                                category = cat ?: "Uncategorized",
                                                amount = formatPaise(paise),
                                                percentage = if (report.netFlowPaise != 0L) {
                                                    val pct = (paise.toDouble() / kotlin.math.abs(report.netFlowPaise) * 100)
                                                    "${pct.toInt()}%"
                                                } else "—",
                                            )
                                        }
                                }
                            }
                        }
                    }

                    // ── Pending / Uncategorized — ALWAYS visible (EC-44) ─
                    item {
                        InsightCard(
                            title = "Pending / Uncategorized",
                            subtitle = "Always included in your total",
                            trailingContent = {
                                Text(
                                    text = formatPaise(report.uncategorizedTotalPaise),
                                    style = Title.copy(fontWeight = FontWeight.Bold),
                                    color = if (report.uncategorizedTotalPaise > 0) colors.warning else colors.textSecondary,
                                )
                            },
                        )
                    }

                    // ── Projection stats ─────────────────────────────
                    item {
                        InsightCard(title = "Projections") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatRow(
                                    label = "Projected Total",
                                    value = formatPaise(report.projectedTotalPaise),
                                    icon = Icons.Outlined.TrendingUp,
                                )
                                StatRow(
                                    label = "Projected Savings",
                                    value = formatPaise(report.projectedSavingsPaise),
                                    icon = Icons.Outlined.TrendingDown,
                                    valueColor = colors.success,
                                )
                                if (report.noPriorData) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = " No prior period data for comparison",
                                            style = Caption,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Suggestions ──────────────────────────────────
                    if (report.suggestions.size > 1) {
                        item {
                            InsightCard(title = "Suggestions") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    report.suggestions.drop(1).forEach { suggestion ->
                                        Text(
                                            text = "• $suggestion",
                                            style = Body,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom spacing
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/**
 * Hero card at top of report — PromoBanner-style (PRD §6.7).
 * Big number + period label + one suggestion line.
 */
@Composable
private fun HeroReportCard(
    periodLabel: String,
    netFlowPaise: Long,
    suggestion: String?,
) {
    val colors = ArthixTheme.colors
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(24.dp),
    ) {
        Text(
            text = periodLabel,
            style = Label,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatPaise(netFlowPaise),
            style = AmountHero,
            color = if (netFlowPaise >= 0) colors.success else colors.textPrimary,
        )
        if (suggestion != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = suggestion,
                style = Body,
                color = colors.textSecondary,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: String,
    amount: String,
    percentage: String,
) {
    val colors = ArthixTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = category,
            style = Body,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = percentage,
            style = Caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = amount,
            style = Title.copy(fontSize = 16.sp),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = ArthixTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = Body,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Text(
            text = value,
            style = Title.copy(fontSize = 16.sp),
            color = valueColor ?: colors.textPrimary,
        )
    }
}

private fun formatPaise(paise: Long): String {
    val rupees = kotlin.math.abs(paise) / 100
    val remainder = kotlin.math.abs(paise) % 100
    val sign = if (paise < 0) "-" else ""
    return "${sign}₹${rupees}.%02d".format(remainder)
}
