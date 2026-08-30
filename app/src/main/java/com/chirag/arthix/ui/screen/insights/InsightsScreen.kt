package com.chirag.arthix.ui.screen.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Insights screen — spending trends, smart AI report insights, category breakdown,
 * and actionable savings opportunities.
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Financial Insights",
                style = HeadlineLg,
                color = colors.textPrimary,
            )

            IconButton(
                onClick = { viewModel.refreshReport() },
                enabled = !uiState.isReportLoading,
            ) {
                if (uiState.isReportLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Insights",
                        tint = colors.accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.md))

        // ── Smart AI Financial Report & Suggestions Card ──────────────
        val report = uiState.report
        if (report != null && report.suggestions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), shapes.card)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(spacing.sm))
                            Text(
                                "AI FINANCIAL ANALYSIS",
                                style = LabelCaps.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent,
                            )
                        }

                        if (report.periodLabel.isNotBlank()) {
                            Text(
                                text = report.periodLabel,
                                style = BodySecondary.copy(fontSize = 11.sp),
                                color = colors.textSecondary,
                            )
                        }
                    }

                    Spacer(Modifier.height(spacing.md))

                    // Primary highlight
                    Text(
                        text = report.suggestions.first(),
                        style = BodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )

                    // Additional detailed suggestions
                    if (report.suggestions.size > 1) {
                        Spacer(Modifier.height(spacing.sm))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            report.suggestions.drop(1).forEach { sug ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = "• ",
                                        style = BodySecondary,
                                        color = colors.accent,
                                    )
                                    Text(
                                        text = sug,
                                        style = BodySecondary,
                                        color = colors.textSecondary,
                                    )
                                }
                            }
                        }
                    }

                    // Potential Savings Pill
                    if (report.projectedSavingsPaise > 0) {
                        Spacer(Modifier.height(spacing.md))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.tagPosBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = colors.tagPosText,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Potential Weekly Savings",
                                        style = BodySecondary.copy(fontWeight = FontWeight.Medium),
                                        color = colors.tagPosText,
                                    )
                                }
                                Text(
                                    text = formatPaise(report.projectedSavingsPaise),
                                    style = BodyPrimary.copy(fontWeight = FontWeight.Bold),
                                    color = colors.tagPosText,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.gutter))
        }

        // ── Lifestyle vs Essential Spending Ratio ───────────────────
        if (uiState.thisMonthSpendPaise > 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, shapes.card)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("SPENDING BEHAVIOR", style = LabelCaps, color = colors.textSecondary)
                        Text(
                            text = "${uiState.discretionaryPercentage}% Discretionary",
                            style = LabelCaps.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.discretionaryPercentage > 50) colors.accentSpend else colors.accent,
                        )
                    }

                    Spacer(Modifier.height(spacing.sm))

                    LinearProgressIndicator(
                        progress = { uiState.discretionaryPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(shapes.pill),
                        color = colors.accentSpend,
                        trackColor = colors.tagPosText.copy(alpha = 0.5f),
                    )

                    Spacer(Modifier.height(spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Lifestyle: ${formatPaise(uiState.discretionarySpendPaise)}",
                            style = BodySecondary.copy(fontSize = 11.sp),
                            color = colors.accentSpend,
                        )
                        Text(
                            text = "Essential: ${formatPaise(uiState.essentialSpendPaise)}",
                            style = BodySecondary.copy(fontSize = 11.sp),
                            color = colors.tagPosText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.gutter))
        }

        // ── This month spend & burn rate hero ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, shapes.card)
                .padding(spacing.cardPadding),
        ) {
            Column {
                Text("THIS MONTH TOTAL", style = LabelCaps, color = colors.textSecondary)
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = formatPaise(uiState.thisMonthSpendPaise),
                    style = DisplayHeroMobile,
                    color = colors.accentSpend,
                )

                Spacer(Modifier.height(spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    Text(text = "Burn ~${formatPaise(uiState.dailyAveragePaise)}/day", style = BodySecondary, color = colors.textSecondary)
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
                    text = "No spending data yet",
                    style = SectionHeader,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = "Start logging transactions to see smart AI insights and savings opportunities.",
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

