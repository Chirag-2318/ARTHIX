package com.chirag.arthix.ui.screen.streak

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.model.DayStatus
import com.chirag.arthix.data.model.StreakDay

private object StreakColors {
    val Background = Color(0xFFFAF7F2)       // warm cream
    val Surface = Color(0xFFFFFFFF)          // white cards
    val Border = Color(0xFFF0EDE8)           // soft border
    val TextPrimary = Color(0xFF1A1A1C)      // near-black
    val TextSecondary = Color(0xFF6B6B75)    // muted gray
    val TextMuted = Color(0xFF9A9AA5)        // lighter muted

    val Coral = Color(0xFFE4463A)            // coral brand
    val CoralLight = Color(0xFFFFE8E5)       // light coral for over-cap
    
    val AmberSoft = Color(0xFFFFF8E5)        // pastel amber for "Left" card
    val AmberDark = Color(0xFFD97706)        // dark amber text

    val Sage = Color(0xFF34A853)             // sage green
    val SageSoft = Color(0xFFE5F5E0)         // light sage for on-track

    val FutureBg = Color(0xFFF4EFE6)         // light neutral gray-cream
    val FutureText = Color(0xFFB5B5C1)
    
    val CoralSoft = Color(0xFFFFE8E5)        // warning callout bg
}

@Composable
fun BudgetStreakScreen(
    viewModel: BudgetStreakViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onAddStreak: () -> Unit = {}, // Actually used for adding an entry
    onSettingsTap: () -> Unit = {},
    onDayTap: (Int) -> Unit = {},
) {
    val streak by viewModel.streak.collectAsState()
    val days by viewModel.days.collectAsState()
    val daysElapsed by viewModel.daysElapsed.collectAsState()

    if (streak == null) {
        Box(modifier = Modifier.fillMaxSize().background(StreakColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StreakColors.Coral)
        }
        return
    }

    val s = streak!!
    val categoryLabel = s.category
    val monthlyAmount = s.monthlyAmountPaise / 100
    val daysInMonth = s.daysInPeriod
    val dailyCap = if (daysInMonth > 0) monthlyAmount / daysInMonth else 0
    val totalSpent = days.filter { it.status != DayStatus.FUTURE && it.status != DayStatus.TODAY_EMPTY }.sumOf { it.spent } / 100L
    val totalCapSoFar = days.filter { it.status != DayStatus.FUTURE }.sumOf { it.cap } / 100L
    val runningBalance = totalCapSoFar - totalSpent // negative = in debt, needs compensation
    val overDays = days.count { it.status == DayStatus.OVER }
    val heldStreak = run {
        var count = 0
        for (d in days.filter { it.status != DayStatus.FUTURE && it.status != DayStatus.TODAY_EMPTY }.reversed()) {
            if (d.status == DayStatus.HELD || d.status == DayStatus.COMPENSATED) count++ else break
        }
        count
    }
    val percentCompleted = min(100L, if (monthlyAmount > 0) ((totalSpent.toFloat() / monthlyAmount) * 100).toLong() else 0L)
    val remainingBudget = max(0L, monthlyAmount - totalSpent)

    Scaffold(
        containerColor = StreakColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStreak,
                containerColor = StreakColors.Coral,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Log")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(StreakColors.Background)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    TopBar(onBack = onBack, onSettingsTap = onSettingsTap)

                    Spacer(Modifier.height(24.dp))
                    CategoryHeader(categoryLabel = categoryLabel, monthlyAmount = monthlyAmount, dailyCap = dailyCap)

                    Spacer(Modifier.height(24.dp))
                    StatTileRow(
                        remainingBudget = remainingBudget,
                        percentCompleted = percentCompleted,
                        runningBalance = runningBalance
                    )

                    Spacer(Modifier.height(32.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StreakFlameHeader(heldStreak = heldStreak)
                        Spacer(Modifier.height(16.dp))
                        MilestoneBadgesRow(heldStreak = heldStreak, daysElapsed = daysElapsed)
                        Spacer(Modifier.height(16.dp))
                        ProgressToNextMilestone(heldStreak = heldStreak)
                    }

                    Spacer(Modifier.height(24.dp))
                    StreakGridCard(days = days, daysInMonth = daysInMonth, daysElapsed = daysElapsed, onDayTap = onDayTap)

                    Spacer(Modifier.height(24.dp))
                    MiniStatRow(heldStreak = heldStreak, percentCompleted = percentCompleted, overDays = overDays)

                    if (runningBalance < 0) {
                        Spacer(Modifier.height(24.dp))
                        CompensationNotice(amountOwed = -runningBalance, dailyCap = dailyCap)
                    }

                    Spacer(Modifier.height(32.dp))
                    Text("Recent Log", color = StreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                }

                LogEntries(
                    days = days.filter { it.status != DayStatus.FUTURE && it.status != DayStatus.TODAY_EMPTY }.reversed().take(6),
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 80.dp) // extra padding for FAB
                )
            }

            StreakCompleteOverlay(
                isVisible = daysElapsed >= daysInMonth && daysElapsed > 0,
                totalSaved = remainingBudget,
                longestChain = heldStreak, // In a real app we'd track max chain
                onStartNew = { /* Handled elsewhere, maybe just back out for now */ onBack() }
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onSettingsTap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(icon = Icons.Filled.ArrowBack, onClick = onBack)
        IconCircleButton(icon = Icons.Outlined.Settings, onClick = onSettingsTap)
    }
}

@Composable
private fun IconCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(StreakColors.Surface)
            .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.04f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = StreakColors.TextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CategoryHeader(categoryLabel: String, monthlyAmount: Long, dailyCap: Long) {
    Column {
        Text(
            "$categoryLabel Budget",
            color = StreakColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "₹$monthlyAmount / month · ₹$dailyCap / day",
            color = StreakColors.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatTileRow(remainingBudget: Long, percentCompleted: Long, runningBalance: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left this month card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                .clip(RoundedCornerShape(24.dp))
                .background(StreakColors.AmberSoft)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Left this month", color = StreakColors.AmberDark.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹$remainingBudget", color = StreakColors.AmberDark, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = StreakColors.AmberDark, modifier = Modifier.size(16.dp))
            }
        }

        // Owed / Ahead card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(130.dp)
                .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.03f))
                .clip(RoundedCornerShape(24.dp))
                .background(StreakColors.Surface)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    if (runningBalance < 0) "Owed (compensate)" else "Ahead of plan",
                    color = StreakColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${kotlin.math.abs(runningBalance)}",
                        color = if (runningBalance < 0) StreakColors.Coral else StreakColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (runningBalance < 0) Icons.Filled.South else Icons.Filled.North,
                        contentDescription = null,
                        tint = if (runningBalance < 0) StreakColors.Coral else StreakColors.Sage,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakGridCard(days: List<StreakDay>, daysInMonth: Int, daysElapsed: Int, onDayTap: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(24.dp))
            .background(StreakColors.Surface)
            .padding(20.dp)
    ) {
        val rows = if (daysInMonth == 0) 1 else (daysInMonth - 1) / 6 + 1
        Box(modifier = Modifier.fillMaxWidth().height((rows * 50 - 10).dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val columnWidth = size.width / 6
                val rowHeight = 50.dp.toPx()
                
                for (i in 0 until days.lastIndex) {
                    val currentDay = days[i]
                    val nextDay = days[i + 1]
                    
                    val isOnTrack = currentDay.status == DayStatus.HELD || currentDay.status == DayStatus.COMPENSATED
                    val nextIsOnTrack = nextDay.status == DayStatus.HELD || nextDay.status == DayStatus.COMPENSATED
                    
                    if (isOnTrack && nextIsOnTrack) {
                        val col1 = i % 6
                        val row1 = i / 6
                        val x1 = col1 * columnWidth + (columnWidth / 2)
                        val y1 = row1 * rowHeight + 20.dp.toPx()
                        
                        val col2 = (i + 1) % 6
                        val row2 = (i + 1) / 6
                        val x2 = col2 * columnWidth + (columnWidth / 2)
                        val y2 = row2 * rowHeight + 20.dp.toPx()
                        
                        if (row1 == row2) {
                            drawLine(
                                color = StreakColors.Sage.copy(alpha = 0.5f),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 4.dp.toPx()
                            )
                        } else {
                            drawLine(
                                color = StreakColors.Sage.copy(alpha = 0.5f),
                                start = Offset(x1, y1),
                                end = Offset(size.width, y1),
                                strokeWidth = 4.dp.toPx()
                            )
                            drawLine(
                                color = StreakColors.Sage.copy(alpha = 0.5f),
                                start = Offset(0f, y2),
                                end = Offset(x2, y2),
                                strokeWidth = 4.dp.toPx()
                            )
                        }
                    }
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(days) { day -> 
                    val isToday = day.dayOfMonth == daysElapsed
                    StreakCell(day = day, isToday = isToday, onTap = { onDayTap(day.dayOfMonth) }) 
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = StreakColors.SageSoft, label = "Yield")
            LegendDot(color = StreakColors.CoralLight, label = "Over")
            LegendDot(color = StreakColors.SageSoft, label = "Made up")
            LegendDot(color = StreakColors.FutureBg, label = "Upcoming")
        }
    }
}

@Composable
private fun StreakCell(day: StreakDay, isToday: Boolean, onTap: () -> Unit) {
    val bg = when {
        isToday -> StreakColors.Coral
        day.status == DayStatus.HELD || day.status == DayStatus.COMPENSATED -> StreakColors.SageSoft
        day.status == DayStatus.OVER -> StreakColors.CoralLight
        else -> StreakColors.FutureBg
    }
    val textColor = when {
        isToday -> Color.White
        day.status == DayStatus.HELD || day.status == DayStatus.COMPENSATED -> StreakColors.Sage
        day.status == DayStatus.OVER -> StreakColors.Coral
        else -> StreakColors.FutureText
    }
    
    val transition = rememberInfiniteTransition(label = "today_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isToday) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(pulseScale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (isToday) Modifier.border(2.dp, StreakColors.Coral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(day.dayOfMonth.toString(), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = StreakColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MiniStatRow(heldStreak: Int, percentCompleted: Long, overDays: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(20.dp))
            .background(StreakColors.Surface)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniStat(value = "$heldStreak", label = "Day streak", isPrimary = true)
        MiniStatDivider()
        MiniStat(value = "$percentCompleted%", label = "Budget used")
        MiniStatDivider()
        MiniStat(value = "$overDays", label = "Over-cap days")
    }
}

@Composable
private fun MiniStat(value: String, label: String, isPrimary: Boolean = false) {
    val valueColor = if (isPrimary) StreakColors.Coral else StreakColors.TextPrimary
    val valueSize = if (isPrimary) 22.sp else 20.sp
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = valueSize)
        Spacer(Modifier.height(4.dp))
        Text(label, color = StreakColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun MiniStatDivider() {
    Box(modifier = Modifier.height(36.dp).width(1.dp).background(StreakColors.Border))
}

@Composable
private fun CompensationNotice(amountOwed: Long, dailyCap: Long) {
    val daysToRecover = if (dailyCap > 0) (amountOwed + dailyCap - 1) / dailyCap else 0 // ceil
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.02f))
            .clip(RoundedCornerShape(20.dp))
            .background(StreakColors.CoralSoft)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StreakColors.Coral, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            val titleText = if (amountOwed <= dailyCap) "A tiny bump in the road!" else "You're ₹$amountOwed over."
            val descText = if (amountOwed <= dailyCap) {
                "We'll easily balance this ₹$amountOwed out over the next $daysToRecover day(s)."
            } else {
                "We'll spread this out over the next $daysToRecover day(s) to get you back on track."
            }
            
            Text(
                titleText,
                color = StreakColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                descText,
                color = StreakColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun LogEntries(days: List<StreakDay>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEach { day -> LogEntryRow(day) }
    }
}

@Composable
private fun LogEntryRow(day: StreakDay) {
    val (statusColor, statusLabel) = when (day.status) {
        DayStatus.HELD -> StreakColors.Sage to "Within cap"
        DayStatus.OVER -> StreakColors.Coral to "Over cap"
        DayStatus.COMPENSATED -> StreakColors.Sage to "Made up"
        else -> StreakColors.TextMuted to ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(16.dp))
            .background(StreakColors.Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Day ${day.dayOfMonth}", color = StreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(statusLabel, color = StreakColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("₹${day.spent / 100}", color = StreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text("/ ₹${day.cap / 100}", color = StreakColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF7F2, widthDp = 360, heightDp = 900)
@Composable
private fun BudgetStreakScreenPreview() {
    MaterialTheme {
        BudgetStreakScreen()
    }
}
