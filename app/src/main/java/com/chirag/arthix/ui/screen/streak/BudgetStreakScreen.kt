package com.chirag.arthix.ui.screen.streak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

/* ═════════════════════════════════════════════════════════════════════════
   BUDGET STREAK — new feature, combining:
   • reference 1 (yellow booking dashboard): search bar, yellow "hero" stat
     tile + dark secondary stat tile side by side, chip-style counters
   • reference 2 (habit tracker): profile+date header, day-grid streak
     visualization, "X days finished / Y% completed / Z steps" stat row,
     bottom action buttons
   mapped onto: a monthly budget envelope with a derived daily cap, where
   each day's spend is tracked like a habit, and overspend must be repaid
   from future days ("compensation").
   ═══════════════════════════════════════════════════════════════════════ */

private object StreakColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceRaised = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)

    val Yellow = Color(0xFFF5C518)
    val YellowDim = Color(0xFFF5C518).copy(alpha = 0.14f)

    val Held = Color(0xFFF5C518)          // day spent within cap
    val Over = Color(0xFFFF5B5B)          // day over cap, not yet compensated
    val Compensated = Color(0xFF34D399)   // day was over, later made up
    val Future = Color(0xFF232329)        // day hasn't happened yet
    val OnYellow = Color(0xFF241D00)
}

@Composable
fun BudgetStreakScreen(
    viewModel: BudgetStreakViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onAddStreak: () -> Unit = {},
    onSettingsTap: () -> Unit = {},
    onDayTap: (Int) -> Unit = {},
) {
    val streak by viewModel.streak.collectAsState()
    val days by viewModel.days.collectAsState()
    val daysElapsed by viewModel.daysElapsed.collectAsState()

    if (streak == null) {
        // Show loading or empty state
        Box(modifier = Modifier.fillMaxSize().background(StreakColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StreakColors.Yellow)
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

    Scaffold(containerColor = StreakColors.Background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(StreakColors.Background)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))
                TopBar(onBack = onBack, onSettingsTap = onSettingsTap, onAddStreak = onAddStreak)

                Spacer(Modifier.height(20.dp))
                CategoryHeader(categoryLabel = categoryLabel, monthlyAmount = monthlyAmount, dailyCap = dailyCap)

                Spacer(Modifier.height(20.dp))
                StatTileRow(
                    remainingBudget = remainingBudget,
                    percentCompleted = percentCompleted,
                    runningBalance = runningBalance
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Streak", color = StreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("Day $daysElapsed of $daysInMonth", color = StreakColors.TextMuted, fontSize = 13.sp)
                }

                Spacer(Modifier.height(14.dp))
                StreakGridCard(days = days, daysInMonth = daysInMonth, onDayTap = onDayTap)

                Spacer(Modifier.height(16.dp))
                MiniStatRow(heldStreak = heldStreak, percentCompleted = percentCompleted, overDays = overDays)

                if (runningBalance < 0) {
                    Spacer(Modifier.height(16.dp))
                    CompensationNotice(amountOwed = -runningBalance, dailyCap = dailyCap)
                }

                Spacer(Modifier.height(24.dp))
                Text("Recent Log", color = StreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Spacer(Modifier.height(12.dp))
            }

            LogEntries(
                days = days.filter { it.status != DayStatus.FUTURE && it.status != DayStatus.TODAY_EMPTY }.reversed().take(6),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onSettingsTap: () -> Unit, onAddStreak: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(icon = Icons.Filled.ArrowBack, onClick = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconCircleButton(icon = Icons.Outlined.Settings, onClick = onSettingsTap)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StreakColors.Yellow)
                    .clickable(onClick = onAddStreak),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add streak", tint = StreakColors.OnYellow, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun IconCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(StreakColors.Surface)
            .border(BorderStroke(1.dp, StreakColors.Border), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = StreakColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CategoryHeader(categoryLabel: String, monthlyAmount: Long, dailyCap: Long) {
    Column {
        Text(
            "$categoryLabel Budget",
            color = StreakColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "₹$monthlyAmount / month · ₹$dailyCap / day",
            color = StreakColors.TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StatTileRow(remainingBudget: Long, percentCompleted: Long, runningBalance: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(StreakColors.Yellow)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Left this month", color = StreakColors.OnYellow.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹$remainingBudget", color = StreakColors.OnYellow, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = StreakColors.OnYellow, modifier = Modifier.size(14.dp))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(StreakColors.Surface)
                .border(BorderStroke(1.dp, StreakColors.Border), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    if (runningBalance < 0) "Owed (compensate)" else "Ahead of plan",
                    color = StreakColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${kotlin.math.abs(runningBalance)}",
                        color = StreakColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (runningBalance < 0) Icons.Filled.South else Icons.Filled.North,
                        contentDescription = null,
                        tint = if (runningBalance < 0) StreakColors.Over else StreakColors.Compensated,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakGridCard(days: List<StreakDay>, daysInMonth: Int, onDayTap: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StreakColors.Surface)
            .border(BorderStroke(1.dp, StreakColors.Border), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(((daysInMonth / 6 + 1) * 38).dp),
            userScrollEnabled = false
        ) {
            items(days) { day -> StreakCell(day = day, onTap = { onDayTap(day.dayOfMonth) }) }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(color = StreakColors.Held, label = "Held")
            LegendDot(color = StreakColors.Over, label = "Over")
            LegendDot(color = StreakColors.Compensated, label = "Made up")
            LegendDot(color = StreakColors.Future, label = "Upcoming")
        }
    }
}

@Composable
private fun StreakCell(day: StreakDay, onTap: () -> Unit) {
    val bg = when (day.status) {
        DayStatus.HELD -> StreakColors.Held
        DayStatus.OVER -> StreakColors.Over
        DayStatus.COMPENSATED -> StreakColors.Compensated
        DayStatus.TODAY_EMPTY -> StreakColors.SurfaceRaised
        DayStatus.FUTURE -> StreakColors.Future
    }
    val textColor = when (day.status) {
        DayStatus.HELD -> StreakColors.OnYellow
        DayStatus.OVER, DayStatus.COMPENSATED -> Color.White
        DayStatus.TODAY_EMPTY -> StreakColors.TextSecondary
        DayStatus.FUTURE -> StreakColors.TextMuted
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .then(
                if (day.status == DayStatus.TODAY_EMPTY)
                    Modifier.border(BorderStroke(1.dp, StreakColors.Yellow), RoundedCornerShape(9.dp))
                else Modifier
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(day.dayOfMonth.toString(), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = StreakColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun MiniStatRow(heldStreak: Int, percentCompleted: Long, overDays: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StreakColors.SurfaceRaised)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MiniStat(value = "$heldStreak", label = "Day streak")
        MiniStatDivider()
        MiniStat(value = "$percentCompleted%", label = "Budget used")
        MiniStatDivider()
        MiniStat(value = "$overDays", label = "Over-cap days")
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = StreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = StreakColors.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MiniStatDivider() {
    Box(modifier = Modifier.height(32.dp).width(1.dp).background(StreakColors.Border))
}

@Composable
private fun CompensationNotice(amountOwed: Long, dailyCap: Long) {
    val daysToRecover = if (dailyCap > 0) (amountOwed + dailyCap - 1) / dailyCap else 0 // ceil
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StreakColors.Over.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, StreakColors.Over.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StreakColors.Over, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "You're ₹$amountOwed over — spread across the next $daysToRecover day(s)",
                color = StreakColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                "Your daily cap will trim slightly until this is repaid",
                color = StreakColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LogEntries(days: List<StreakDay>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        days.forEach { day -> LogEntryRow(day) }
    }
}

@Composable
private fun LogEntryRow(day: StreakDay) {
    val (statusColor, statusLabel) = when (day.status) {
        DayStatus.HELD -> StreakColors.Held to "Within cap"
        DayStatus.OVER -> StreakColors.Over to "Over cap"
        DayStatus.COMPENSATED -> StreakColors.Compensated to "Made up"
        else -> StreakColors.TextMuted to ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StreakColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Day ${day.dayOfMonth}", color = StreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(statusLabel, color = StreakColors.TextMuted, fontSize = 12.sp)
        }
        Text("₹${day.spent / 100}", color = StreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text("/ ₹${day.cap / 100}", color = StreakColors.TextMuted, fontSize = 12.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D, widthDp = 360, heightDp = 900)
@Composable
private fun BudgetStreakScreenPreview() {
    MaterialTheme {
        BudgetStreakScreen()
    }
}
