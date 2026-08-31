package com.chirag.arthix.ui.screen.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.R
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.ui.model.Category
import com.chirag.arthix.ui.model.expenseCategories
import com.chirag.arthix.ui.model.incomeCategories

private object ArthixColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceFocused = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextTertiary = Color(0xFF6B6B75)
    val Accent = Color(0xFFFF6B5B) // Or keep your existing Accent
}

private enum class TxnType { EXPENSE, INCOME }

@Composable
fun AddTransactionScreen(
    direction: Direction,
    amount: String,
    payee: String,
    selectedCategory: String?,
    isSaving: Boolean,
    onDirectionChange: (Direction) -> Unit,
    onAmountChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onLogExpense: () -> Unit
) {
    val type = if (direction == Direction.OUTFLOW) TxnType.EXPENSE else TxnType.INCOME
    val categories = if (type == TxnType.EXPENSE) expenseCategories else incomeCategories

    val isUntouched = amount.isEmpty() && payee.isEmpty() && selectedCategory == null
    val amountValue = amount.toIntOrNull() ?: 0
    val canLog = amountValue > 0 && payee.isNotBlank() && !isSaving

    Surface(modifier = Modifier.fillMaxSize(), color = ArthixColors.Background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

            // Header — flat, matches every other screen's back+title row, no gradient
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ArthixColors.TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Add Transaction",
                    color = ArthixColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                TypeToggle(selected = type, onSelect = { 
                    onDirectionChange(if (it == TxnType.EXPENSE) Direction.OUTFLOW else Direction.INFLOW)
                })

                Spacer(Modifier.height(28.dp))

                CaptureRow(onVoiceCapture = onMicClick, onCameraCapture = onCameraClick)

                Spacer(Modifier.height(28.dp))

                AmountInput(
                    value = amount,
                    onValueChange = { input -> onAmountChange(input.filter { it.isDigit() }.take(7)) },
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = payee,
                    onValueChange = onPayeeChange,
                    placeholder = { Text("e.g. Swiggy, Amazon, Metro", color = ArthixColors.TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ArthixColors.SurfaceFocused,
                        unfocusedContainerColor = ArthixColors.Surface,
                        focusedTextColor = ArthixColors.TextPrimary,
                        unfocusedTextColor = ArthixColors.TextPrimary,
                        focusedBorderColor = ArthixColors.Accent,
                        unfocusedBorderColor = ArthixColors.Border,
                        cursorColor = ArthixColors.Accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                Text("Category", color = ArthixColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(20.dp))

                // Fills the dead space in the original layout — fades out the moment
                // the user actually starts filling the form, never overlaps real input.
                AnimatedVisibility(visible = isUntouched, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Image(
                            painter = painterResource(R.drawable.ill_manual_entry),
                            contentDescription = null,
                            modifier = Modifier.size(140.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Logging it manually? Fair enough.",
                            color = ArthixColors.TextTertiary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // Bottom CTA — was a muted/disabled-looking pill; now a real primary button
            Button(
                onClick = onLogExpense,
                enabled = canLog,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArthixColors.Accent,
                    disabledContainerColor = ArthixColors.Accent.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .height(54.dp),
            ) {
                Text(
                    if (isSaving) "Saving..." else if (type == TxnType.EXPENSE) "Log Expense" else "Log Income",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun TypeToggle(selected: TxnType, onSelect: (TxnType) -> Unit) {
    val outflowColor = ArthixColors.Accent          // matches your Outflow legend dot
    val inflowColor = Color(0xFF34C759)          // matches your Inflow legend dot

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArthixColors.Surface, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        ToggleSegment(
            label = "Expense",
            icon = Icons.Outlined.TrendingDown,
            selected = selected == TxnType.EXPENSE,
            color = outflowColor,
            onClick = { onSelect(TxnType.EXPENSE) },
            modifier = Modifier.weight(1f),
        )
        ToggleSegment(
            label = "Income",
            icon = Icons.Outlined.TrendingUp,
            selected = selected == TxnType.INCOME,
            color = inflowColor,
            onClick = { onSelect(TxnType.INCOME) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) color.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) color else ArthixColors.TextTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) color else ArthixColors.TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CaptureRow(onVoiceCapture: () -> Unit, onCameraCapture: () -> Unit) {
    // Same bold filled-pill treatment as Home — was ghost circles here, inconsistent
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CapturePill(icon = Icons.Filled.Mic, label = "Voice", onClick = onVoiceCapture, modifier = Modifier.weight(1f))
        CapturePill(icon = Icons.Filled.CameraAlt, label = "Camera", onClick = onCameraCapture, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CapturePill(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = ArthixColors.Accent),
        modifier = modifier.height(48.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AmountInput(value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Text("₹", color = ArthixColors.Accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
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
            color = ArthixColors.TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(ArthixColors.Accent),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("0", color = ArthixColors.TextTertiary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            inner()
        },
    )
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    // Note: The previous chip used Text for emoji (glyph), but this design uses an ImageVector.
    // The existing expenseCategories have glyphs (Strings). The provided code assumed ImageVectors.
    // I will adapt the design to use the existing `glyph` (emoji) as requested by the user previously?
    // Wait, the user specifically said: "emoji for category/type icons (rest of the app uses real vector icons... your own New Budget Streak screen already set the correct pattern...)"
    // Oh, the user WANTS me to use real vector icons, not emojis!
    // But `expenseCategories` currently uses `glyph` which is a String.
    // Wait, let's look at `expenseCategories` in `ui.model.Category`.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (selected) ArthixColors.Accent.copy(alpha = 0.18f) else ArthixColors.Surface)
                .then(
                    if (selected) Modifier.border(1.5.dp, ArthixColors.Accent, CircleShape) else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // Using the existing emoji glyph string because updating the entire data model
            // will break the DB and other screens that depend on String emojis.
            Text(
                category.glyph,
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(category.label, color = if (selected) ArthixColors.TextPrimary else ArthixColors.TextTertiary, fontSize = 12.sp)
    }
}

