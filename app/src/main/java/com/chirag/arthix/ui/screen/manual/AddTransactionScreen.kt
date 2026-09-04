package com.chirag.arthix.ui.screen.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.R
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.ui.model.Category
import com.chirag.arthix.ui.model.expenseCategories
import com.chirag.arthix.ui.model.incomeCategories

private object AddTxnColors {
    val Background = Color(0xFFFAF7F2)       // warm cream
    val Surface = Color(0xFFFFFFFF)           // white cards
    val SurfaceFocused = Color(0xFFFFFFFF)    // white when typed
    val Border = Color(0xFFF0EDE8)            // soft border
    val TextPrimary = Color(0xFF1A1A1C)       // near-black
    val TextSecondary = Color(0xFF6B6B75)     // muted gray
    val TextMuted = Color(0xFF9A9AA5)         // lighter muted
    
    val Coral = Color(0xFFE4463A)             // coral brand
    val Sage = Color(0xFF34A853)              // sage green for incoming

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

private enum class TxnType { OUTGOING, INCOMING }

@Composable
fun AddTransactionScreen(
    direction: Direction,
    amount: String,
    payee: String,
    selectedCategory: String?,
    isSaving: Boolean = false,
    splitNames: List<String> = emptyList(),
    wantsToSplit: Boolean = false,
    onWantsToSplitChange: (Boolean) -> Unit = {},
    onClearSplit: () -> Unit = {},
    onDirectionChange: (Direction) -> Unit,
    onAmountChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit = {},
    onLogExpense: () -> Unit,
    isEditMode: Boolean = false,
    showDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    topContent: @Composable () -> Unit = {}
) {
    val type = if (direction == Direction.OUTFLOW) TxnType.OUTGOING else TxnType.INCOMING
    val categories = if (type == TxnType.OUTGOING) expenseCategories else incomeCategories

    val isUntouched = amount.isEmpty() && payee.isEmpty() && selectedCategory == null
    val amountValue = amount.toDoubleOrNull()?.let { (it * 100).toLong() } 
        ?: amount.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val canLog = amountValue > 0L && !isSaving

    Scaffold(
        containerColor = AddTxnColors.Background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {

            // 1. Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AddTxnColors.TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditMode) "Edit Transaction" else "Add Transaction",
                    color = AddTxnColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (showDelete) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = AddTxnColors.Coral)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(16.dp))

                topContent()

                // 2. Outgoing / Incoming toggle
                TypeToggle(selected = type, onSelect = { 
                    onDirectionChange(if (it == TxnType.OUTGOING) Direction.OUTFLOW else Direction.INFLOW)
                })

                Spacer(Modifier.height(40.dp))

                // 3. Amount display
                val displayAmount = if (amount.endsWith(".00")) amount.removeSuffix(".00") else amount
                AmountInput(
                    value = displayAmount,
                    onValueChange = { input -> 
                        val cleaned = input.filter { it.isDigit() || it == '.' }.take(9)
                        onAmountChange(cleaned) 
                    },
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 60.dp),
                    thickness = 2.dp,
                    color = AddTxnColors.Border
                )
                Spacer(Modifier.height(32.dp))

                // 4. Merchant/description input
                OutlinedTextField(
                    value = payee,
                    onValueChange = onPayeeChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = AddTxnColors.TextMuted
                        )
                    },
                    placeholder = { 
                        Text(
                            "e.g. Swiggy, Amazon, Metro", 
                            color = AddTxnColors.TextMuted
                        ) 
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AddTxnColors.SurfaceFocused,
                        unfocusedContainerColor = AddTxnColors.Surface,
                        focusedTextColor = AddTxnColors.TextPrimary,
                        unfocusedTextColor = AddTxnColors.TextPrimary,
                        focusedBorderColor = AddTxnColors.Border,
                        unfocusedBorderColor = AddTxnColors.Border,
                        cursorColor = AddTxnColors.Coral,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.03f)),
                )

                Spacer(Modifier.height(20.dp))

                // Split Bill Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onWantsToSplitChange(!wantsToSplit) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = AddTxnColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Split Bill",
                            color = AddTxnColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    androidx.compose.material3.Switch(
                        checked = wantsToSplit,
                        onCheckedChange = { onWantsToSplitChange(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AddTxnColors.Coral,
                            uncheckedThumbColor = AddTxnColors.TextSecondary,
                            uncheckedTrackColor = AddTxnColors.SurfaceFocused
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 5. Category selector
                Text("Category", color = AddTxnColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categories) { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory == category.label,
                            onClick = {
                                onCategoryChange(if (selectedCategory == category.label) "" else category.label)
                            },
                        )
                    }
                }

                if (splitNames.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AddTxnColors.Surface)
                            .border(BorderStroke(1.dp, AddTxnColors.Border), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = null,
                                tint = AddTxnColors.Coral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Splitting with: ${splitNames.joinToString(", ")}",
                                color = AddTxnColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AddTxnColors.Background)
                                .clickable(onClick = onClearSplit),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove split",
                                tint = AddTxnColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 6. Mascot illustration
                AnimatedVisibility(visible = isUntouched, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ill_manual_entry),
                            contentDescription = null,
                            modifier = Modifier.size(190.dp),
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Logging it manually? Fair enough.",
                            color = AddTxnColors.TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 7. Primary submit button
            val btnColor = if (type == TxnType.OUTGOING) AddTxnColors.Coral else AddTxnColors.Sage
            val buttonLabel = if (isSaving) "Logging..." 
                else if (wantsToSplit || splitNames.isNotEmpty()) "Log & Split Bill" 
                else if (isEditMode) "Save Changes"
                else if (type == TxnType.OUTGOING) "Log Outgoing" 
                else "Log Incoming"

            Button(
                onClick = onLogExpense,
                enabled = canLog,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnColor,
                    disabledContainerColor = btnColor.copy(alpha = 0.4f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp),
            ) {
                Text(
                    buttonLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun TypeToggle(selected: TxnType, onSelect: (TxnType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF0EDE8)) // light cream/gray track
            .padding(6.dp),
    ) {
        ToggleSegment(
            label = "Outgoing",
            selected = selected == TxnType.OUTGOING,
            color = AddTxnColors.Coral,
            onClick = { onSelect(TxnType.OUTGOING) },
            modifier = Modifier.weight(1f),
        )
        ToggleSegment(
            label = "Incoming",
            selected = selected == TxnType.INCOMING,
            color = AddTxnColors.Sage,
            onClick = { onSelect(TxnType.INCOMING) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, 
            color = if (selected) Color.White else AddTxnColors.TextSecondary, 
            fontSize = 14.sp, 
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AmountInput(value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Text("₹", color = AddTxnColors.TextSecondary, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        BasicTextFieldAmount(value = value, onValueChange = onValueChange)
    }
}

@Composable
private fun BasicTextFieldAmount(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = AddTxnColors.TextPrimary,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AddTxnColors.Coral),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("0", color = AddTxnColors.TextMuted, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            inner()
        },
    )
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    // Determine colors based on category
    val (bg, iconColor) = when (category.label.lowercase()) {
        "food" -> AddTxnColors.PastelPeach to AddTxnColors.PeachDark
        "travel" -> AddTxnColors.PastelSky to AddTxnColors.SkyDark
        "shopping" -> AddTxnColors.PastelLavender to AddTxnColors.LavenderDark
        "bills" -> AddTxnColors.PastelSage to AddTxnColors.SageDark
        "groceries", "salary" -> AddTxnColors.PastelSand to AddTxnColors.SandDark
        "refund" -> AddTxnColors.PastelSky to AddTxnColors.SkyDark
        "gift" -> AddTxnColors.PastelPeach to AddTxnColors.PeachDark
        "interest" -> AddTxnColors.PastelSage to AddTxnColors.SageDark
        else -> AddTxnColors.PastelLavender to AddTxnColors.LavenderDark
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) iconColor else bg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label,
                tint = if (selected) Color.White else iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(category.label, color = if (selected) AddTxnColors.TextPrimary else AddTxnColors.TextSecondary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}
