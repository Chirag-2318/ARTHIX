package com.chirag.arthix.ui.screen.moneylog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.entity.MoneyLogEntity
import com.chirag.arthix.data.model.MoneyLogDateMode
import com.chirag.arthix.util.MoneyLogPdfExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyLogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToManualEntry: () -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: MoneyLogViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val totalAmount = entries.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money Log", color = MoneyLogColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MoneyLogColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(Icons.Filled.Upload, contentDescription = "Import", tint = MoneyLogColors.TextPrimary)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            MoneyLogPdfExporter.exportToPdf(context, entries)
                        }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Export PDF", tint = MoneyLogColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MoneyLogColors.Background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToManualEntry,
                containerColor = MoneyLogColors.Brand,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Entry")
            }
        },
        containerColor = MoneyLogColors.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MoneyLogColors.Background)) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = MoneyLogColors.Brand.copy(alpha = 0.08f)),
                colors = CardDefaults.cardColors(containerColor = MoneyLogColors.Surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Received", color = MoneyLogColors.TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${"%.2f".format(totalAmount)}",
                        color = MoneyLogColors.TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Entries List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(entries) { entry ->
                    MoneyLogEntryRow(entry = entry, onDelete = { viewModel.deleteEntry(it) })
                }
            }
        }
    }
}

@Composable
fun MoneyLogEntryRow(entry: MoneyLogEntity, onDelete: (MoneyLogEntity) -> Unit) {
    val exactFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val dateStr = when (entry.dateMode) {
        MoneyLogDateMode.EXACT -> entry.exactDateMillis?.let { exactFormat.format(it) } ?: "Unknown"
        MoneyLogDateMode.RANGE -> {
            val s = entry.rangeStartMillis?.let { exactFormat.format(it) } ?: "?"
            val e = entry.rangeEndMillis?.let { exactFormat.format(it) } ?: "?"
            "$s - $e"
        }
        MoneyLogDateMode.APPROXIMATE -> entry.approximateMonthYear ?: "Approximate"
        MoneyLogDateMode.UNKNOWN -> "Date not specified"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MoneyLogColors.Surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat look with border or soft shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    color = MoneyLogColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.category.label} • $dateStr",
                    color = MoneyLogColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                text = "₹${"%.2f".format(entry.amount)}",
                color = MoneyLogColors.Positive, // Green for inflow
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
