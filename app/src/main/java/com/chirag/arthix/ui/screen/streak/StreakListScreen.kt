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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.entity.BudgetStreakEntity

private object StreakListColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val Yellow = Color(0xFFF5C518)
    val OnYellow = Color(0xFF241D00)
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
        topBar = {
            TopAppBar(
                title = { Text("Budget Streaks", color = StreakListColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = StreakListColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StreakListColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStreak,
                containerColor = StreakListColors.Yellow,
                contentColor = StreakListColors.OnYellow,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Streak")
            }
        }
    ) { padding ->
        if (streaks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(StreakListColors.Background),
                contentAlignment = Alignment.Center
            ) {
                // Slot 3: ill_streak — shown only when zero budget streaks exist.
                // Gate: streaks.isEmpty(). FAB is the single CTA; inline button removed.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ill_streak),
                        contentDescription = null,
                        modifier = Modifier.size(130.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "No budget streaks yet",
                        color = StreakListColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Set a spending cap and build a streak",
                        color = StreakListColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(StreakListColors.Background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(streaks) { streak ->
                    StreakListItem(streak = streak, onClick = { onNavigateToStreak(streak.id) })
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
