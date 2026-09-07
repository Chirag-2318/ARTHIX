package com.chirag.arthix.ui.screen.moneylog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyLogImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoneyLogViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var parsedResults by remember { mutableStateOf<List<ParsedImportRow>?>(null) }

    val promptTemplate = """
        Convert these messy notes into this exact pipe-delimited format, one entry per line:
        CATEGORY | DESCRIPTION | AMOUNT | DATE

        Where:
        - CATEGORY is one of: EXAM, COLLEGE_FEE, RENT, MONTHLY_EXPENSE, TRAVEL, MEDICAL, MISC, OTHER
        - DESCRIPTION is a short label
        - AMOUNT is a plain number (no currency symbols, no commas)
        - DATE is one of: dd-mm-yyyy, or a range dd-mm-yyyy to dd-mm-yyyy, or mm-yyyy if only the month is known, or the literal word unknown if there's no date at all

        Examples:
        EXAM | NDA 2 2023 | 100 | 06-06-2023
        COLLEGE_FEE | IITM term 1 | 2100 | unknown
        MONTHLY_EXPENSE | Monthly expense | 4500 | 16-12-2024 to 15-01-2025

        Only output lines in this exact format, one entry per line, no extra commentary, no markdown.
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Import", color = MoneyLogColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
        ) {
            if (parsedResults == null) {
                // Import Input Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Part A: Copy Prompt Template", fontWeight = FontWeight.SemiBold, color = MoneyLogColors.TextPrimary)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MoneyLogColors.Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = promptTemplate,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MoneyLogColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Prompt Template", promptTemplate)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyLogColors.SurfaceWarm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MoneyLogColors.Brand)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Prompt", color = MoneyLogColors.Brand, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    HorizontalDivider(color = MoneyLogColors.CardBorder)

                    Text("Part B: Paste LLM Output", fontWeight = FontWeight.SemiBold, color = MoneyLogColors.TextPrimary)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Paste the pipe-delimited text here...", color = MoneyLogColors.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MoneyLogColors.TextPrimary,
                            unfocusedTextColor = MoneyLogColors.TextPrimary,
                            focusedBorderColor = MoneyLogColors.Brand,
                            unfocusedBorderColor = MoneyLogColors.CardBorder,
                            focusedContainerColor = MoneyLogColors.Surface,
                            unfocusedContainerColor = MoneyLogColors.Surface
                        )
                    )

                    Button(
                        onClick = {
                            parsedResults = viewModel.parseBulkImportText(importText)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyLogColors.TextPrimary),
                        enabled = importText.isNotBlank()
                    ) {
                        Text("Parse Preview", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            } else {
                // Preview Mode
                val results = parsedResults!!
                val validEntries = results.mapNotNull { it.entity }
                val errorCount = results.count { it.error != null }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Preview Import", fontWeight = FontWeight.Bold, color = MoneyLogColors.TextPrimary, fontSize = 20.sp)
                    Text("Valid: ${validEntries.size}, Errors: $errorCount", color = MoneyLogColors.TextSecondary)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { row ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (row.error != null) MoneyLogColors.PastelBlush else MoneyLogColors.Surface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Line ${row.lineNum}: ${row.originalText}", fontSize = 12.sp, color = MoneyLogColors.TextPrimary)
                                    if (row.error != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(row.error, color = MoneyLogColors.Negative, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { parsedResults = null },
                            modifier = Modifier.weight(1f).height(50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MoneyLogColors.CardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneyLogColors.TextPrimary)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                viewModel.commitBulkImport(validEntries)
                                Toast.makeText(context, "Imported ${validEntries.size} entries", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MoneyLogColors.TextPrimary),
                            enabled = validEntries.isNotEmpty()
                        ) {
                            Text("Import Valid", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
