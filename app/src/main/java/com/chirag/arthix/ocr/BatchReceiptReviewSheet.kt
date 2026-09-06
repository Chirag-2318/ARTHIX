package com.chirag.arthix.ocr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.ui.components.ArthixDatePickerDialog
import com.chirag.arthix.ui.components.ArthixTimePickerDialog
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.AccessTime
import com.chirag.arthix.ui.model.expenseCategories
import com.chirag.arthix.ui.theme.ArthixTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * An item in the batch receipt review queue.
 */
data class BatchReceiptItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amountText: String,
    val amountPaise: Long?,
    val payee: String,
    val category: String,
    val transactionDateMillis: Long?,
    val isDateNeedsReview: Boolean,
    val reviewReasons: List<String>,
    val rawText: String,
    val timeDisplay: String? = null,
)

/**
 * Batch Review Queue UI (Docs/ARTHIX_OCR_Date_Extraction_Design.md §3).
 *
 * Displays one card per receipt, sorted by extracted date (oldest first).
 * Every card allows editing amount, date (via date-picker), category, and payee.
 * Provides a bulk confirmation action to commit all receipts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchReceiptReviewSheet(
    receipts: List<BatchReceiptItem>,
    isConfirming: Boolean,
    onUpdateReceipt: (BatchReceiptItem) -> Unit,
    onDeleteReceipt: (String) -> Unit,
    onAddMoreReceipts: () -> Unit,
    onConfirmAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ArthixTheme.colors

    // Sort queue by date: unassigned/flagged dates first, then oldest to newest (§3)
    val sortedReceipts = remember(receipts) {
        receipts.sortedWith(
            compareBy<BatchReceiptItem> { it.transactionDateMillis == null }
                .thenBy { it.transactionDateMillis ?: Long.MAX_VALUE }
        )
    }

    var activeDatePickerReceiptId by remember { mutableStateOf<String?>(null) }
    val activeItem = receipts.firstOrNull { it.id == activeDatePickerReceiptId }

    if (activeDatePickerReceiptId != null && activeItem != null) {
        ArthixDatePickerDialog(
            initialDateMillis = activeItem.transactionDateMillis,
            onDateSelected = { selectedDate ->
                onUpdateReceipt(
                    activeItem.copy(
                        transactionDateMillis = selectedDate,
                        isDateNeedsReview = false,
                        reviewReasons = activeItem.reviewReasons.filter { it != "Confirm date" }
                    )
                )
                activeDatePickerReceiptId = null
            },
            onDismiss = { activeDatePickerReceiptId = null }
        )
    }

    var activeTimePickerReceiptId by remember { mutableStateOf<String?>(null) }
    val activeTimeItem = receipts.firstOrNull { it.id == activeTimePickerReceiptId }

    if (activeTimePickerReceiptId != null && activeTimeItem != null) {
        val initialHourAndMinute = remember(activeTimeItem.transactionDateMillis) {
            val epoch = activeTimeItem.transactionDateMillis ?: System.currentTimeMillis()
            val lt = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalTime()
            lt.hour to lt.minute
        }
        ArthixTimePickerDialog(
            initialHour = initialHourAndMinute.first,
            initialMinute = initialHourAndMinute.second,
            onTimeSelected = { hour, minute ->
                val zoneId = ZoneId.systemDefault()
                val baseDate = if (activeTimeItem.transactionDateMillis != null) {
                    Instant.ofEpochMilli(activeTimeItem.transactionDateMillis).atZone(zoneId).toLocalDate()
                } else {
                    LocalDate.now(zoneId)
                }
                val newMillis = baseDate.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
                val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                val amPm = if (hour < 12) "AM" else "PM"
                val display = "%d:%02d %s".format(hour12, minute, amPm)

                onUpdateReceipt(
                    activeTimeItem.copy(
                        transactionDateMillis = newMillis,
                        timeDisplay = display,
                    )
                )
                activeTimePickerReceiptId = null
            },
            onDismiss = { activeTimePickerReceiptId = null }
        )
    }

    Scaffold(
        containerColor = Color(0xFF121316),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22242B))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Batch Review",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val receiptCountText = if (receipts.size == 1) "1 receipt" else "${receipts.size} receipts"
                        Text(
                            text = "$receiptCountText • Oldest first",
                            color = Color(0xFF9A9AA5),
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(onClick = onAddMoreReceipts) {
                    Text("+ Add More", color = Color(0xFFE4463A), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFF1A1B20),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onConfirmAll,
                        enabled = receipts.isNotEmpty() && !isConfirming,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE4463A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Saving Batch...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Confirm All (${receipts.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (sortedReceipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No receipts in queue",
                    color = Color(0xFF9A9AA5),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedReceipts, key = { it.id }) { item ->
                    val index = receipts.indexOfFirst { it.id == item.id } + 1
                    BatchReceiptCard(
                        item = item,
                        receiptNumber = index,
                        onUpdate = onUpdateReceipt,
                        onDelete = { onDeleteReceipt(item.id) },
                        onPickDate = { activeDatePickerReceiptId = item.id },
                        onPickTime = { activeTimePickerReceiptId = item.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchReceiptCard(
    item: BatchReceiptItem,
    receiptNumber: Int,
    onUpdate: (BatchReceiptItem) -> Unit,
    onDelete: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    val dateDisplay = remember(item.transactionDateMillis) {
        item.transactionDateMillis?.let { millis ->
            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F26)),
        border = BorderStroke(
            1.dp,
            if (item.isDateNeedsReview || item.amountPaise == null) Color(0xFFE4463A) else Color(0xFF2C2D36)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Receipt badge + Attention chip + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2B35))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Receipt #$receiptNumber",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (item.isDateNeedsReview || item.amountPaise == null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE4463A).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val text = when {
                                item.amountPaise == null && item.isDateNeedsReview -> "Needs date & amount ⚠"
                                item.isDateNeedsReview -> "Confirm date ⚠"
                                else -> "Confirm amount ⚠"
                            }
                            Text(
                                text = text,
                                color = Color(0xFFE4463A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFF9A9AA5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Row 1: Amount & Payee Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Field
                OutlinedTextField(
                    value = item.amountText,
                    onValueChange = { newAmount ->
                        val clean = newAmount.filter { it.isDigit() || it == '.' }
                        val paise = clean.toDoubleOrNull()?.let { (it * 100).toLong() }
                        onUpdate(
                            item.copy(
                                amountText = clean,
                                amountPaise = paise,
                                reviewReasons = if (paise != null) item.reviewReasons.filter { it != "Confirm amount" } else item.reviewReasons
                            )
                        )
                    },
                    label = { Text("Amount (₹)", fontSize = 12.sp) },
                    prefix = { Text("₹", color = Color(0xFFE4463A), fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE4463A),
                        unfocusedBorderColor = Color(0xFF32333E),
                        focusedContainerColor = Color(0xFF262732),
                        unfocusedContainerColor = Color(0xFF262732),
                        focusedLabelColor = Color(0xFFE4463A),
                        unfocusedLabelColor = Color(0xFF9A9AA5),
                    ),
                    modifier = Modifier.weight(0.45f)
                )

                // Payee Field
                OutlinedTextField(
                    value = item.payee,
                    onValueChange = { newPayee ->
                        onUpdate(item.copy(payee = newPayee))
                    },
                    label = { Text("Merchant / Payee", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE4463A),
                        unfocusedBorderColor = Color(0xFF32333E),
                        focusedContainerColor = Color(0xFF262732),
                        unfocusedContainerColor = Color(0xFF262732),
                        focusedLabelColor = Color(0xFFE4463A),
                        unfocusedLabelColor = Color(0xFF9A9AA5),
                    ),
                    modifier = Modifier.weight(0.55f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: Date & Time Picker Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Chip
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF262732))
                        .clickable(onClick = onPickDate)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = "Date",
                            tint = if (item.isDateNeedsReview) Color(0xFFE4463A) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val dateText = when {
                            item.transactionDateMillis == null && item.isDateNeedsReview -> "Needs date ⚠"
                            dateDisplay != null -> "📅 $dateDisplay"
                            else -> "Needs date ⚠"
                        }
                        Text(
                            text = dateText,
                            color = if (item.isDateNeedsReview) Color(0xFFE4463A) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Icon(
                        Icons.Outlined.EditCalendar,
                        contentDescription = "Change Date",
                        tint = Color(0xFF9A9AA5),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Time Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF262732))
                        .clickable(onClick = onPickTime)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = "Time",
                        tint = Color(0xFFE4463A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.timeDisplay ?: "Time",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Change Time",
                        tint = Color(0xFF9A9AA5),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Row 3: Category Quick-Selection Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(expenseCategories) { cat ->
                    val isSelected = item.category == cat.label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFE4463A) else Color(0xFF262732))
                            .clickable { onUpdate(item.copy(category = cat.label)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) Color.White else Color(0xFF9A9AA5),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
