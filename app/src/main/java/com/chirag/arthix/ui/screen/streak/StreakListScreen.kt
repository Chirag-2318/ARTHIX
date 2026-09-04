package com.chirag.arthix.ui.screen.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.chirag.arthix.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.entity.BudgetStreakEntity

private object StreakListColors {
    val Background = Color(0xFFFAF7F2)       // warm cream
    val Surface = Color(0xFFFFFFFF)          // white cards
    val TextPrimary = Color(0xFF1A1A1C)      // near-black
    val TextSecondary = Color(0xFF6B6B75)    // muted gray
    val Coral = Color(0xFFE4463A)            // action coral
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakListScreen(
    viewModel: StreakListViewModel = hiltViewModel(),
    onNavigateToStreak: (Long) -> Unit = {},
    onAddStreak: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val streaks by viewModel.streaks.collectAsState()

    Scaffold(
        containerColor = StreakListColors.Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddStreak,
                containerColor = StreakListColors.Coral,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                text = { Text("Add new streak", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StreakListColors.Background)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = StreakListColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Budget Streaks",
                    color = StreakListColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (streaks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ill_streak),
                            contentDescription = null,
                            modifier = Modifier.size(160.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "No budget streaks yet",
                            color = StreakListColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Streaks help you limit your expenses on specific categories (like Food or Shopping) over a set period.",
                            color = StreakListColors.TextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Hit the button below to set a spending cap and keep the fire burning! \uD83D\uDD25",
                            color = StreakListColors.TextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(streaks) { streak ->
                        StreakListItem(streak = streak, onClick = { onNavigateToStreak(streak.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakListItem(streak: BudgetStreakEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(16.dp))
            .background(StreakListColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${streak.category} Budget",
                color = StreakListColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "₹${streak.monthlyAmountPaise / 100} • ${streak.daysInPeriod} days",
                color = StreakListColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
