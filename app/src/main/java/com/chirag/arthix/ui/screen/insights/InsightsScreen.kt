package com.chirag.arthix.ui.screen.insights

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.ui.model.expenseCategories
import androidx.compose.ui.res.painterResource
import com.chirag.arthix.R

object InsightColors {
    val Background = Color(0xFFFAF7F2)
    val Surface = Color.White
    val SurfaceRaised = Color(0xFFF0F0F0)
    val Border = Color(0xFFE5E5E5)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF6E6E73)
    val TextMuted = Color(0xFF8E8E93)

    val Brand = Color(0xFFE4463A)          // Main Coral
    val BrandDim = Color(0xFFE4463A).copy(alpha = 0.14f)
    val Positive = Color(0xFF8BA888)       // Sage Green for savings/inflow
    val Warning = Color(0xFFE4463A)
    val Pending = Color(0xFF8E8E93)
}

data class CategorySpend(val label: String, val amount: Int, val icon: ImageVector)

data class TxnLogItem(
    val payee: String,
    val time: String,
    val amount: String,
    val category: String,
    val statusLabel: String?,
)

@Composable
fun InsightsScreen(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToManualEntry: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val report = uiState.report

    // Prepare Category Breakdown
    val totalSpendPaise = report?.categoryBreakdown?.values?.sum() ?: 0L
    val categories = report?.categoryBreakdown?.map { (catName, paise) ->
        val icon = expenseCategories.find { it.label.equals(catName, ignoreCase = true) }?.icon ?: Icons.Filled.Category
        CategorySpend(catName, (paise / 100).toInt(), icon)
    }?.sortedByDescending { it.amount } ?: emptyList()

    val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    val liveTxnLog = uiState.recentTransactions.map { txn ->
        val amountPaise = txn.amountPaise ?: 0L
        val rupees = amountPaise / 100
        val remainder = amountPaise % 100
        val sign = if (txn.direction == com.chirag.arthix.data.model.Direction.INFLOW) "+" else "-"
        val amountStr = if (amountPaise == 0L) "—" else "$sign₹$rupees" + if (remainder > 0) ".%02d".format(remainder) else ""

        TxnLogItem(
            payee = txn.payee ?: txn.category?.replaceFirstChar { it.uppercase() } ?: "Expense",
            time = timeFormat.format(java.util.Date(txn.timestamp)),
            amount = amountStr,
            category = txn.category ?: "Uncategorized",
            statusLabel = if (txn.status != com.chirag.arthix.data.model.TransactionStatus.CONFIRMED) "Pending" else null
        )
    }

    val totalSpent = (totalSpendPaise / 100).toInt()
    // Mock budget for progress ring (if none, just assume spent + some buffer)
    val projectedTotal = ((report?.projectedTotalPaise ?: 0L) / 100).toInt()
    val weeklyBudget = if (projectedTotal > 0) projectedTotal else if (totalSpent > 0) (totalSpent * 1.2).toInt() else 1000

    FinancialInsightsScreen(
        weekLabel = report?.periodLabel ?: "This Period",
        weeklyBudget = weeklyBudget,
        totalSpent = totalSpent,
        projectedTotal = projectedTotal,
        projectedSavings = ((report?.projectedSavingsPaise ?: 0L) / 100).toInt(),
        uncategorizedTotal = ((report?.uncategorizedTotalPaise ?: 0L) / 100).toInt(),
        suggestions = report?.suggestions ?: emptyList(),
        categories = categories,
        txnLog = liveTxnLog,
        onRefresh = { viewModel.refreshReport() },
        onViewAllTxns = onNavigateToActivity,
    )
}

@Composable
fun FinancialInsightsScreen(
    weekLabel: String,
    weeklyBudget: Int,
    totalSpent: Int,
    projectedTotal: Int,
    projectedSavings: Int,
    uncategorizedTotal: Int,
    suggestions: List<String>,
    categories: List<CategorySpend>,
    txnLog: List<TxnLogItem>,
    onRefresh: () -> Unit = {},
    onViewAllTxns: () -> Unit = {},
) {
    val remaining = (weeklyBudget - totalSpent).coerceAtLeast(0)

    Scaffold(
        containerColor = InsightColors.Background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(InsightColors.Background)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 100.dp
            )
        ) {
            item {
                InsightsHeader(onRefresh = onRefresh)
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text(
                    weekLabel,
                    color = InsightColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    headlineFor(remaining = remaining, budget = weeklyBudget),
                    color = InsightColors.TextMuted,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(28.dp))
            }

            item {
                BudgetProgressRing(
                    spent = totalSpent,
                    budget = weeklyBudget
                )
                Spacer(Modifier.height(32.dp))
            }
            
            if (projectedTotal > 0 || projectedSavings > 0) {
                item {
                    ProjectionsRow(projectedTotal, projectedSavings)
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    Text("Category Breakdown", color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                    Spacer(Modifier.height(16.dp))
                }
                val total = categories.sumOf { it.amount }.coerceAtLeast(1)
                items(categories) { cat ->
                    CategoryRowCard(cat, (cat.amount.toFloat() / total * 100).toInt())
                    Spacer(Modifier.height(10.dp))
                }
                item { Spacer(Modifier.height(18.dp)) }
            }

            if (uncategorizedTotal > 0) {
                item {
                    UncategorizedNotice(uncategorizedTotal)
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    Text("Suggestions", color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                    Spacer(Modifier.height(16.dp))
                }
                items(suggestions) { suggestion ->
                    SuggestionCard(suggestion)
                    Spacer(Modifier.height(10.dp))
                }
                item { Spacer(Modifier.height(18.dp)) }
            } else {
                // Slot 5: ill_insights — zero-data state inside the suggestion card shape.
                // Gate: suggestions.isEmpty(). Shown instead of lightbulb+text for this path only.
                item {
                    Text("Suggestions", color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                            .clip(RoundedCornerShape(16.dp))
                            .background(InsightColors.Surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ill_insights),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "No expenses logged during this period.",
                            color = InsightColors.TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction Log", color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                    Text(
                        "See All",
                        color = InsightColors.Brand,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onViewAllTxns)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (txnLog.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                            .clip(RoundedCornerShape(16.dp))
                            .background(InsightColors.Surface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions recorded for this period yet.",
                            color = InsightColors.TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(txnLog) { item ->
                    TxnLogRow(item)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

private fun headlineFor(remaining: Int, budget: Int): String {
    if (budget <= 0) return "Tracking your spend"
    val ratio = remaining.toFloat() / budget
    return when {
        ratio > 0.4f -> "Great pace this period"
        ratio > 0.15f -> "Keeping it close to budget"
        ratio > 0f -> "Cutting it fine this period"
        else -> "Over budget this period"
    }
}

@Composable
private fun InsightsHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Insights", color = InsightColors.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(InsightColors.Surface)
                .border(BorderStroke(1.dp, InsightColors.Border), CircleShape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = InsightColors.TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun BudgetProgressRing(spent: Int, budget: Int) {
    val progress = (spent.toFloat() / budget.coerceAtLeast(1)).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track
                drawArc(
                    color = InsightColors.SurfaceRaised,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
                // Progress
                drawArc(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFFFF9142), InsightColors.Brand)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₹$spent", color = InsightColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                Spacer(Modifier.height(2.dp))
                Text("of ₹$budget", color = InsightColors.TextMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ProjectionsRow(projectedTotal: Int, projectedSavings: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectionCard(
            title = "Projected Total",
            amount = "₹$projectedTotal",
            icon = Icons.Filled.TrendingUp,
            iconTint = InsightColors.Warning,
            modifier = Modifier.weight(1f)
        )
        ProjectionCard(
            title = "Projected Savings",
            amount = "₹$projectedSavings",
            icon = Icons.Filled.Savings,
            iconTint = InsightColors.Positive,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProjectionCard(title: String, amount: String, icon: ImageVector, iconTint: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(18.dp))
            .background(InsightColors.Surface)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(amount, color = InsightColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(title, color = InsightColors.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun CategoryRowCard(cat: CategorySpend, percentage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(InsightColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(InsightColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(cat.icon, contentDescription = null, tint = InsightColors.Brand, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.label, color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = InsightColors.Brand,
                trackColor = InsightColors.SurfaceRaised,
                strokeCap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${cat.amount}", color = InsightColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("$percentage%", color = InsightColors.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(InsightColors.Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = InsightColors.Brand, modifier = Modifier.size(20.dp).offset(y = 2.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            suggestion,
            color = InsightColors.TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun UncategorizedNotice(amount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(InsightColors.BrandDim)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = InsightColors.Brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Uncategorized Spend", color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("₹$amount needs review to update insights", color = InsightColors.Brand, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TxnLogRow(item: TxnLogItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(InsightColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.payee, color = InsightColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.time, color = InsightColors.TextMuted, fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(text = item.category, color = InsightColors.TextSecondary, bgColor = InsightColors.SurfaceRaised)
            item.statusLabel?.let {
                Spacer(Modifier.width(6.dp))
                Pill(text = it, color = InsightColors.Warning, bgColor = InsightColors.Warning.copy(alpha = 0.15f))
            }
            Spacer(Modifier.width(12.dp))
            val isPositive = item.amount.startsWith("+")
            Text(item.amount, color = if (isPositive) InsightColors.Positive else InsightColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Pill(text: String, color: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D, widthDp = 360, heightDp = 1200)
@Composable
private fun FinancialInsightsScreenPreview() {
    MaterialTheme {
        InsightsScreen()
    }
}
