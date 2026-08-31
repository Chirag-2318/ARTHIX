package com.chirag.arthix.ui.screen.streak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.ui.model.Category
import com.chirag.arthix.ui.model.expenseCategories

/* ═════════════════════════════════════════════════════════════════════════
   ADD BUDGET STREAK — creation flow, explicitly requested:
   amount → purpose/category → daily distribution mode (Equal / Custom)
   Styled to match BudgetStreakScreen's yellow-on-dark language so the
   two feel like one feature, not two different screens bolted together.
   ═══════════════════════════════════════════════════════════════════════ */

private object AddStreakColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceRaised = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)
    val Yellow = Color(0xFFF5C518)
    val OnYellow = Color(0xFF241D00)
    val Error = Color(0xFFFF6B5B)
}

private enum class DistributionMode { EQUAL, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetStreakScreen(
    viewModel: AddBudgetStreakViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var daysInMonth by remember { mutableStateOf(30) }
    var mode by remember { mutableStateOf(DistributionMode.EQUAL) }
    
    var showCustomModal by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val dailyCap = if (daysInMonth > 0) (amount / daysInMonth) else 0.0
    val canCreate = amount > 0 && selectedCategory != null
    val isSaving by viewModel.isSaving.collectAsState()

    if (showCustomModal) {
        CustomDistributionModal(
            totalAmountPaise = (amount * 100).toLong(),
            daysInMonth = daysInMonth,
            onDismiss = { showCustomModal = false },
            onConfirm = { customAmounts ->
                showCustomModal = false
                viewModel.createStreak(
                    category = selectedCategory?.label ?: "Other",
                    label = customLabel,
                    amountPaise = (amount * 100).toLong(),
                    daysInPeriod = daysInMonth,
                    distributionMode = mode.name,
                    customCaps = customAmounts,
                    onSuccess = { onBack() }
                )
            }
        )
    }

    Scaffold(
        containerColor = AddStreakColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("New Budget Streak", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = AddStreakColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AddStreakColors.Background)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.background(AddStreakColors.Background).padding(20.dp)) {
                Button(
                    onClick = {
                        if (mode == DistributionMode.CUSTOM) {
                            showCustomModal = true
                        } else {
                            viewModel.createStreak(
                                category = selectedCategory?.label ?: "Other",
                                label = customLabel,
                                amountPaise = (amount * 100).toLong(),
                                daysInPeriod = daysInMonth,
                                distributionMode = mode.name,
                                customCaps = null,
                                onSuccess = { onBack() }
                            )
                        }
                    },
                    enabled = canCreate && !isSaving,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AddStreakColors.Yellow,
                        disabledContainerColor = AddStreakColors.SurfaceRaised,
                        contentColor = AddStreakColors.OnYellow,
                        disabledContentColor = AddStreakColors.TextMuted
                    )
                ) {
                    Text(if (isSaving) "Saving..." else "Start Streak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(AddStreakColors.Background)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SectionLabel("How much are you allocating?")
            Spacer(Modifier.height(10.dp))
            AmountEntryCard(amountText = amountText, onAmountChange = { new ->
                if (new.isEmpty() || new.matches(Regex("^\\d{0,7}(\\.\\d{0,2})?$"))) amountText = new
            })

            Spacer(Modifier.height(24.dp))
            SectionLabel("What's it for?")
            Spacer(Modifier.height(10.dp))
            CategoryPickerRow(selected = selectedCategory, onSelect = { selectedCategory = it })

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = customLabel,
                onValueChange = { customLabel = it },
                placeholder = { Text("Optional note, e.g. \"Monthly food money from home\"", color = AddStreakColors.TextMuted, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AddStreakColors.Surface,
                    unfocusedContainerColor = AddStreakColors.Surface,
                    focusedBorderColor = AddStreakColors.TextSecondary,
                    unfocusedBorderColor = AddStreakColors.Border,
                    focusedTextColor = AddStreakColors.TextPrimary,
                    unfocusedTextColor = AddStreakColors.TextPrimary,
                    cursorColor = AddStreakColors.TextPrimary
                )
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("Over how many days?")
            Spacer(Modifier.height(10.dp))
            DaysStepper(days = daysInMonth, onChange = { daysInMonth = it })

            Spacer(Modifier.height(24.dp))
            SectionLabel("How should it be split?")
            Spacer(Modifier.height(10.dp))
            DistributionPicker(mode = mode, onModeChange = { mode = it })

            Spacer(Modifier.height(20.dp))
            DailyCapPreview(dailyCap = dailyCap, mode = mode)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomDistributionModal(
    totalAmountPaise: Long,
    daysInMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    // Keep amounts in strings for text fields, convert to Long (paise) for math
    val initialPerDay = (totalAmountPaise / 100.0) / daysInMonth
    val amounts = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(daysInMonth) {
        if (amounts.isEmpty()) {
            for (i in 1..daysInMonth) {
                amounts.add("%.0f".format(initialPerDay))
            }
        }
    }

    val currentSumPaise = amounts.sumOf { (it.toDoubleOrNull() ?: 0.0).toLong() * 100L }
    val isMatched = currentSumPaise == totalAmountPaise
    val difference = totalAmountPaise - currentSumPaise

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AddStreakColors.Background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AddStreakColors.Surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = AddStreakColors.TextPrimary)
                    }
                    Text("Custom Distribution", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                }

                // Sticky summary row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AddStreakColors.SurfaceRaised)
                        .border(BorderStroke(1.dp, AddStreakColors.Border))
                        .padding(20.dp)
                ) {
                    Text("Target Total: ₹${totalAmountPaise / 100}", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    val color = if (isMatched) AddStreakColors.Yellow else AddStreakColors.Error
                    val diffText = if (isMatched) "Perfectly matched" else if (difference > 0) "₹${difference / 100} remaining to assign" else "Over target by ₹${-difference / 100}"
                    
                    Text(diffText, color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }

                // List of days
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    for (i in 0 until amounts.size) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Day ${i + 1}", color = AddStreakColors.TextSecondary, fontSize = 16.sp, modifier = Modifier.width(60.dp))
                            Spacer(Modifier.width(16.dp))
                            
                            OutlinedTextField(
                                value = amounts[i],
                                onValueChange = { new ->
                                    if (new.isEmpty() || new.matches(Regex("^\\d{0,7}(\\.\\d{0,2})?$"))) {
                                        amounts[i] = new
                                    }
                                },
                                prefix = { Text("₹", color = AddStreakColors.TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AddStreakColors.Surface,
                                    unfocusedContainerColor = AddStreakColors.Surface,
                                    focusedBorderColor = AddStreakColors.TextPrimary,
                                    unfocusedBorderColor = AddStreakColors.Border,
                                    focusedTextColor = AddStreakColors.TextPrimary,
                                    unfocusedTextColor = AddStreakColors.TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Footer CTA
                Box(modifier = Modifier.background(AddStreakColors.Surface).padding(20.dp)) {
                    Button(
                        onClick = {
                            val parsedCaps = amounts.map { (it.toDoubleOrNull() ?: 0.0).toLong() * 100L }
                            onConfirm(parsedCaps)
                        },
                        enabled = isMatched,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AddStreakColors.Yellow,
                            disabledContainerColor = AddStreakColors.SurfaceRaised,
                            contentColor = AddStreakColors.OnYellow,
                            disabledContentColor = AddStreakColors.TextMuted
                        )
                    ) {
                        Text("Confirm & Start", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = AddStreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
private fun AmountEntryCard(amountText: String, onAmountChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AddStreakColors.Surface)
            .border(BorderStroke(1.dp, AddStreakColors.Border), RoundedCornerShape(22.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", color = AddStreakColors.Yellow, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            Spacer(Modifier.width(6.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = amountText,
                onValueChange = onAmountChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = AddStreakColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AddStreakColors.Yellow),
                decorationBox = { inner ->
                    if (amountText.isEmpty()) {
                        Text("0", color = AddStreakColors.TextMuted, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                    }
                    inner()
                },
                modifier = Modifier.widthIn(min = 40.dp, max = 200.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("/ month", color = AddStreakColors.TextMuted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CategoryPickerRow(selected: Category?, onSelect: (Category) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(expenseCategories) { cat ->
            val isSelected = cat == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(cat) }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AddStreakColors.Yellow.copy(alpha = 0.20f) else AddStreakColors.SurfaceRaised)
                        .border(
                            BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) AddStreakColors.Yellow else AddStreakColors.Border),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        cat.icon,
                        contentDescription = cat.label,
                        tint = if (isSelected) AddStreakColors.Yellow else AddStreakColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    cat.label,
                    color = if (isSelected) AddStreakColors.TextPrimary else AddStreakColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DaysStepper(days: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AddStreakColors.Surface)
            .border(BorderStroke(1.dp, AddStreakColors.Border), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$days days", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepperButton(icon = Icons.Filled.Remove, onClick = { if (days > 1) onChange(days - 1) })
            StepperButton(icon = Icons.Filled.Add, onClick = { onChange(days + 1) })
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(AddStreakColors.SurfaceRaised)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AddStreakColors.TextPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun DistributionPicker(mode: DistributionMode, onModeChange: (DistributionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DistributionOption(
            title = "Split equally",
            subtitle = "Same amount every day, divided evenly across the period",
            icon = Icons.Filled.CalendarViewMonth,
            selected = mode == DistributionMode.EQUAL,
            onClick = { onModeChange(DistributionMode.EQUAL) }
        )
        DistributionOption(
            title = "Set custom amounts",
            subtitle = "Choose a different cap for specific days (e.g. more on weekends)",
            icon = Icons.Filled.Tune,
            selected = mode == DistributionMode.CUSTOM,
            onClick = { onModeChange(DistributionMode.CUSTOM) }
        )
    }
}

@Composable
private fun DistributionOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) AddStreakColors.Yellow.copy(alpha = 0.12f) else AddStreakColors.Surface)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AddStreakColors.Yellow else AddStreakColors.Border),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) AddStreakColors.Yellow.copy(alpha = 0.20f) else AddStreakColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) AddStreakColors.Yellow else AddStreakColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AddStreakColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = AddStreakColors.TextMuted, fontSize = 12.sp)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AddStreakColors.Yellow, unselectedColor = AddStreakColors.Border)
        )
    }
}

@Composable
private fun DailyCapPreview(dailyCap: Double, mode: DistributionMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AddStreakColors.SurfaceRaised)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = AddStreakColors.Yellow, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (mode == DistributionMode.EQUAL)
                "That's about ₹${"%.0f".format(dailyCap)} a day"
            else
                "You'll set each day's cap after creating this streak",
            color = AddStreakColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}
