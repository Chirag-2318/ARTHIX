package com.chirag.arthix.ui.screen.split

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.chirag.arthix.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private object SplitListColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)
    val Accent = Color(0xFFFF7A1A)
    val Green = Color(0xFF34D399)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitListScreen(
    onNavigateToSplit: (Long) -> Unit,
    viewModel: SplitListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSplits()
    }

    Scaffold(
        containerColor = SplitListColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Splits", color = SplitListColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SplitListColors.Background)
            )
        },

    ) { padding ->
        if (uiState == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SplitListColors.Accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                if (uiState!!.isEmpty()) {
                    item {
                        EmptySplitState(Modifier.fillParentMaxHeight(0.6f))
                    }
                } else {
                    items(uiState!!) { split ->
                        SplitListItem(split = split, onClick = { onNavigateToSplit(split.transactionId) })
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { onNavigateToSplit(0L) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SplitListColors.Accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Add Split",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(100.dp)) // padding for global FAB
                }
            }
        }
    }
}

@Composable
private fun EmptySplitState(modifier: Modifier = Modifier) {
    // Slot 4: ill_split_bill — shown only when uiState!!.isEmpty().
    // Gate: uiState!!.isEmpty(). "Add Split" button stays exactly as is below.
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ill_split_bill),
            contentDescription = null,
            modifier = Modifier.size(130.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "No splits yet",
            color = SplitListColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Split a transaction with a friend from Activity",
            color = SplitListColors.TextMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SplitListItem(split: SplitListItemUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SplitListColors.Surface)
            .border(BorderStroke(1.dp, SplitListColors.Border), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(split.merchantName, color = SplitListColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("${split.participantsCount} people", color = SplitListColors.TextMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${split.totalAmountPaise / 100}", color = SplitListColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            
            val isFullySettled = split.paidParticipantsCount == split.participantsCount && split.participantsCount > 0
            val netText = if (isFullySettled) "Settled" 
                          else if (split.netOwedPaise > 0) "You are owed ₹${split.netOwedPaise / 100} (${split.paidParticipantsCount}/${split.participantsCount} paid)" 
                          else if (split.netOwedPaise < 0) "You owe ₹${-split.netOwedPaise / 100} (${split.paidParticipantsCount}/${split.participantsCount} paid)" 
                          else "${split.paidParticipantsCount}/${split.participantsCount} paid"
                          
            val netColor = if (isFullySettled) SplitListColors.TextMuted 
                           else if (split.netOwedPaise > 0) SplitListColors.Green 
                           else if (split.netOwedPaise < 0) SplitListColors.Accent 
                           else SplitListColors.TextMuted
                           
            Text(netText, color = netColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
