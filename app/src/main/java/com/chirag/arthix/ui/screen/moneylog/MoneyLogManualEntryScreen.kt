package com.chirag.arthix.ui.screen.moneylog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.entity.MoneyLogEntity
import com.chirag.arthix.data.model.MoneyLogCategory
import com.chirag.arthix.data.model.MoneyLogDateMode
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyLogManualEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoneyLogViewModel = hiltViewModel()
) {
    var category by remember { mutableStateOf(MoneyLogCategory.OTHER) }
    var customLabel by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    var dateMode by remember { mutableStateOf(MoneyLogDateMode.EXACT) }
    var exactDateStr by remember { mutableStateOf("") }
    var rangeStartStr by remember { mutableStateOf("") }
    var rangeEndStr by remember { mutableStateOf("") }
    var approxStr by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Entry", color = MoneyLogColors.TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MoneyLogColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MoneyLogColors.Background)
            )
        },
        containerColor = MoneyLogColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoneyLogColors.Background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Dropdown - simplified using Segmented Buttons for brevity or just a basic picker
            Text("Category", color = MoneyLogColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MoneyLogColors.TextPrimary,
                        unfocusedTextColor = MoneyLogColors.TextPrimary,
                        focusedBorderColor = MoneyLogColors.Brand,
                        unfocusedBorderColor = MoneyLogColors.CardBorder,
                        focusedContainerColor = MoneyLogColors.Surface,
                        unfocusedContainerColor = MoneyLogColors.Surface
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MoneyLogColors.Surface)
                ) {
                    MoneyLogCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label, color = MoneyLogColors.TextPrimary) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (category == MoneyLogCategory.OTHER) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Category Label", color = MoneyLogColors.TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MoneyLogColors.TextPrimary,
                        unfocusedTextColor = MoneyLogColors.TextPrimary,
                        focusedBorderColor = MoneyLogColors.Brand,
                        unfocusedBorderColor = MoneyLogColors.CardBorder,
                        focusedContainerColor = MoneyLogColors.Surface,
                        unfocusedContainerColor = MoneyLogColors.Surface
                    )
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = MoneyLogColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoneyLogColors.TextPrimary,
                    unfocusedTextColor = MoneyLogColors.TextPrimary,
                    focusedBorderColor = MoneyLogColors.Brand,
                    unfocusedBorderColor = MoneyLogColors.CardBorder,
                    focusedContainerColor = MoneyLogColors.Surface,
                    unfocusedContainerColor = MoneyLogColors.Surface
                )
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount", color = MoneyLogColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoneyLogColors.TextPrimary,
                    unfocusedTextColor = MoneyLogColors.TextPrimary,
                    focusedBorderColor = MoneyLogColors.Brand,
                    unfocusedBorderColor = MoneyLogColors.CardBorder,
                    focusedContainerColor = MoneyLogColors.Surface,
                    unfocusedContainerColor = MoneyLogColors.Surface
                )
            )

            // Date Mode Selector
            Text("Date Format", color = MoneyLogColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MoneyLogDateMode.values().forEach { mode ->
                    FilterChip(
                        selected = dateMode == mode,
                        onClick = { dateMode = mode },
                        label = { Text(mode.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MoneyLogColors.Surface,
                            labelColor = MoneyLogColors.TextSecondary,
                            selectedContainerColor = MoneyLogColors.BrandLight,
                            selectedLabelColor = MoneyLogColors.Brand
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = dateMode == mode,
                            borderColor = MoneyLogColors.CardBorder,
                            selectedBorderColor = MoneyLogColors.Brand
                        )
                    )
                }
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MoneyLogColors.TextPrimary,
                unfocusedTextColor = MoneyLogColors.TextPrimary,
                focusedBorderColor = MoneyLogColors.Brand,
                unfocusedBorderColor = MoneyLogColors.CardBorder,
                focusedContainerColor = MoneyLogColors.Surface,
                unfocusedContainerColor = MoneyLogColors.Surface
            )

            when (dateMode) {
                MoneyLogDateMode.EXACT -> {
                    OutlinedTextField(
                        value = exactDateStr,
                        onValueChange = { exactDateStr = it },
                        label = { Text("Date (dd-MM-yyyy)", color = MoneyLogColors.TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }
                MoneyLogDateMode.RANGE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rangeStartStr,
                            onValueChange = { rangeStartStr = it },
                            label = { Text("Start (dd-MM-yyyy)", color = MoneyLogColors.TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors
                        )
                        OutlinedTextField(
                            value = rangeEndStr,
                            onValueChange = { rangeEndStr = it },
                            label = { Text("End (dd-MM-yyyy)", color = MoneyLogColors.TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors
                        )
                    }
                }
                MoneyLogDateMode.APPROXIMATE -> {
                    OutlinedTextField(
                        value = approxStr,
                        onValueChange = { approxStr = it },
                        label = { Text("Month-Year (MM-yyyy)", color = MoneyLogColors.TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }
                MoneyLogDateMode.UNKNOWN -> {
                    Text("Date not specified", color = MoneyLogColors.TextMuted)
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)", color = MoneyLogColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    val entity = MoneyLogEntity(
                        category = category,
                        customCategoryLabel = if (category == MoneyLogCategory.OTHER) customLabel else null,
                        description = description,
                        amount = parsedAmount,
                        dateMode = dateMode,
                        exactDateMillis = try { sdf.parse(exactDateStr)?.time } catch (e: Exception) { null },
                        rangeStartMillis = try { sdf.parse(rangeStartStr)?.time } catch (e: Exception) { null },
                        rangeEndMillis = try { sdf.parse(rangeEndStr)?.time } catch (e: Exception) { null },
                        approximateMonthYear = approxStr.takeIf { it.isNotBlank() },
                        note = note.takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.insertEntry(entity)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoneyLogColors.TextPrimary) // Use a distinct button color from the theme
            ) {
                Text("Save Entry", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
