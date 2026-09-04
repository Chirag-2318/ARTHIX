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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.ui.model.Category
import com.chirag.arthix.ui.model.expenseCategories

private object AddStreakColors {
    val Background = Color(0xFFFAF7F2)       // warm cream
    val Surface = Color(0xFFFFFFFF)           // white cards
    val SurfaceFocused = Color(0xFFFFFFFF)
    val Border = Color(0xFFF0EDE8)            // soft border
    val TextPrimary = Color(0xFF1A1A1C)       // near-black
    val TextSecondary = Color(0xFF6B6B75)     // muted gray
    val TextMuted = Color(0xFF9A9AA5)         // lighter muted
    
    val Coral = Color(0xFFE4463A)             // coral brand
    val AmberSoft = Color(0xFFFFF8E5)         // pastel amber
    val AmberDark = Color(0xFFD97706)         // dark amber for text/icons
    val Error = Color(0xFFE4463A)             // coral red

    // Category Pastels
    val PastelPeach = Color(0xFFFFE8E5)
    val PastelSky = Color(0xFFE5F0FF)
    val PastelLavender = Color(0xFFF0E8FF)
    val PastelSage = Color(0xFFE5F5E0)
    val PastelSand = Color(0xFFFFF5E5)
    
    val PeachDark = Color(0xFFE4463A)
    val SkyDark = Color(0xFF3A7BE4)
    val LavenderDark = Color(0xFF8B5CF6)
    val SageDark = Color(0xFF34A853)
    val SandDark = Color(0xFFF59E0B)
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
        bottomBar = {
            Box(modifier = Modifier.background(Color.Transparent).padding(20.dp)) {
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AddStreakColors.Coral,
                        disabledContainerColor = Color(0xFFE5E5E5),
                        contentColor = Color.White,
                        disabledContentColor = AddStreakColors.TextMuted
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = AddStreakColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Budget Streak",
                    color = AddStreakColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                SectionLabel("HOW MUCH ARE YOU ALLOCATING?")
                Spacer(Modifier.height(12.dp))
                AmountEntryCard(amountText = amountText, onAmountChange = { new ->
                    if (new.isEmpty() || new.matches(Regex("^\\d{0,7}(\\.\\d{0,2})?$"))) amountText = new
                })

                Spacer(Modifier.height(32.dp))
                SectionLabel("WHAT'S IT FOR?")
                Spacer(Modifier.height(12.dp))
                CategoryPickerRow(selected = selectedCategory, onSelect = { selectedCategory = it })

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    placeholder = { Text("Optional note, e.g. 'Monthly food money from home'", color = AddStreakColors.TextMuted, fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.03f)),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AddStreakColors.SurfaceFocused,
                        unfocusedContainerColor = AddStreakColors.Surface,
                        focusedBorderColor = AddStreakColors.Border,
                        unfocusedBorderColor = AddStreakColors.Border,
                        focusedTextColor = AddStreakColors.TextPrimary,
                        unfocusedTextColor = AddStreakColors.TextPrimary,
                        cursorColor = AddStreakColors.Coral
                    )
                )

                Spacer(Modifier.height(32.dp))
                SectionLabel("OVER HOW MANY DAYS?")
                Spacer(Modifier.height(12.dp))
                DaysStepper(days = daysInMonth, onChange = { daysInMonth = it })

                Spacer(Modifier.height(32.dp))
                SectionLabel("HOW SHOULD IT BE SPLIT?")
                Spacer(Modifier.height(12.dp))
                DistributionPicker(mode = mode, onModeChange = { mode = it })

                Spacer(Modifier.height(24.dp))
                DailyCapPreview(dailyCap = dailyCap, mode = mode)

                Spacer(Modifier.height(32.dp))
            }
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
                    Text("Custom Distribution", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                }

                // Sticky summary row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AddStreakColors.Surface)
                        .border(BorderStroke(1.dp, AddStreakColors.Border))
                        .padding(20.dp)
                ) {
                    Text("Target Total: ₹${totalAmountPaise / 100}", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    
                    val color = if (isMatched) Color(0xFF34A853) else AddStreakColors.Error
                    val diffText = if (isMatched) "Perfectly matched" else if (difference > 0) "₹${difference / 100} remaining to assign" else "Over target by ₹${-difference / 100}"
                    
                    Text(diffText, color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }

                // List of days
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
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
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = AddStreakColors.Surface,
                                    unfocusedContainerColor = AddStreakColors.Surface,
                                    focusedBorderColor = AddStreakColors.Coral,
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
                Box(modifier = Modifier.background(Color.Transparent).padding(20.dp)) {
                    Button(
                        onClick = {
                            val parsedCaps = amounts.map { (it.toDoubleOrNull() ?: 0.0).toLong() * 100L }
                            onConfirm(parsedCaps)
                        },
                        enabled = isMatched,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AddStreakColors.Coral,
                            disabledContainerColor = Color(0xFFE5E5E5),
                            contentColor = Color.White,
                            disabledContentColor = AddStreakColors.TextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
    Text(text.uppercase(), color = AddStreakColors.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
}

@Composable
private fun AmountEntryCard(amountText: String, onAmountChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(AddStreakColors.Surface)
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", color = AddStreakColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 44.sp)
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = amountText,
                onValueChange = onAmountChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = AddStreakColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AddStreakColors.Coral),
                decorationBox = { inner ->
                    if (amountText.isEmpty()) {
                        Text("0", color = AddStreakColors.TextMuted, fontWeight = FontWeight.Bold, fontSize = 52.sp)
                    }
                    inner()
                },
                modifier = Modifier.widthIn(min = 30.dp, max = 220.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("/ month", color = AddStreakColors.TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CategoryPickerRow(selected: Category?, onSelect: (Category) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(expenseCategories) { cat ->
            val isSelected = cat == selected
            
            // Determine colors based on category
            val (bg, iconColor) = when (cat.label.lowercase()) {
                "food" -> AddStreakColors.PastelPeach to AddStreakColors.PeachDark
                "travel" -> AddStreakColors.PastelSky to AddStreakColors.SkyDark
                "shopping" -> AddStreakColors.PastelLavender to AddStreakColors.LavenderDark
                "bills" -> AddStreakColors.PastelSage to AddStreakColors.SageDark
                "groceries" -> AddStreakColors.PastelSand to AddStreakColors.SandDark
                else -> AddStreakColors.PastelLavender to AddStreakColors.LavenderDark
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(cat) }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) iconColor else bg)
                        .then(
                            if (isSelected) Modifier.border(2.dp, iconColor, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        cat.icon,
                        contentDescription = cat.label,
                        tint = if (isSelected) Color.White else iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    cat.label,
                    color = if (isSelected) AddStreakColors.TextPrimary else AddStreakColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
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
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(20.dp))
            .background(AddStreakColors.Surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$days days", color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepperButton(icon = Icons.Filled.Remove, onClick = { if (days > 1) onChange(days - 1) })
            StepperButton(icon = Icons.Filled.Add, onClick = { onChange(days + 1) })
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AddStreakColors.Background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AddStreakColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DistributionPicker(mode: DistributionMode, onModeChange: (DistributionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(20.dp))
            .background(AddStreakColors.Surface)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AddStreakColors.Coral else AddStreakColors.Border),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AddStreakColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AddStreakColors.Coral, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AddStreakColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = AddStreakColors.TextSecondary, fontSize = 13.sp)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AddStreakColors.Coral, unselectedColor = AddStreakColors.Border)
        )
    }
}

@Composable
private fun DailyCapPreview(dailyCap: Double, mode: DistributionMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AddStreakColors.AmberSoft)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = AddStreakColors.AmberDark, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (mode == DistributionMode.EQUAL)
                "That's about ₹${"%.0f".format(dailyCap)} a day"
            else
                "You'll set each day's cap after creating this streak",
            color = AddStreakColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
