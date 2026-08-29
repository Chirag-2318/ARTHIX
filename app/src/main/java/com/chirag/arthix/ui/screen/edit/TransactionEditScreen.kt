package com.chirag.arthix.ui.screen.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.ui.components.CategoryChipRow
import com.chirag.arthix.ui.components.DestructiveButton
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.StatusTag
import com.chirag.arthix.ui.components.StatusTagConfig
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Display
import com.chirag.arthix.ui.theme.Label
import com.chirag.arthix.ui.theme.Title

/**
 * Transaction edit/delete screen (PRD §6.8, EC-52).
 *
 * Features:
 * - Editable amount (paise-precision), category (chip row), payee (free text)
 * - Confidence banner for auto_resolved / needs_review (§8, EC-15/22/30/32)
 * - Delete with confirmation dialog (destructive-action pattern)
 * - Atomic confidence_flag → CLEAN on save
 *
 * Styled with design system components, no default Material styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }
    LaunchedEffect(uiState.deleteComplete) {
        if (uiState.deleteComplete) onNavigateBack()
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Transaction",
                        style = Title,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = colors.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                ),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            uiState.transaction == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Transaction not found",
                        style = Body,
                        color = colors.textSecondary,
                    )
                }
            }
            else -> {
                val txn = uiState.transaction!!
                var amountText by remember {
                    mutableStateOf(
                        txn.amountPaise?.let { (it / 100.0).toString() } ?: ""
                    )
                }
                var payee by remember { mutableStateOf(txn.payee ?: "") }
                var selectedCategory by remember { mutableStateOf(txn.category) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ── Confidence banner (§8, EC-15/22/30/32) ──────
                    if (txn.confidenceFlag != ConfidenceFlag.CLEAN) {
                        ConfidenceBanner(flag = txn.confidenceFlag)
                    }

                    // ── Amount field ─────────────────────────────────
                    Text("Amount", style = Label, color = colors.textSecondary)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("₹0.00", style = Body) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                        ),
                        textStyle = Title,
                        prefix = {
                            Text(
                                "₹ ",
                                style = Title,
                                color = colors.textSecondary,
                            )
                        },
                    )

                    // ── Payee field ──────────────────────────────────
                    Text("Payee", style = Label, color = colors.textSecondary)
                    OutlinedTextField(
                        value = payee,
                        onValueChange = { payee = it },
                        placeholder = { Text("e.g. Swiggy, Amazon", style = Body) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                        ),
                        textStyle = Body,
                    )

                    // ── Category chips ───────────────────────────────
                    Text("Category", style = Label, color = colors.textSecondary)
                    CategoryChipRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                    )

                    Spacer(Modifier.weight(1f))

                    // ── Save button ──────────────────────────────────
                    PrimaryButton(
                        text = if (uiState.isSaving) "Saving…" else "Save Changes",
                        onClick = {
                            val paise = amountText.toDoubleOrNull()?.let { (it * 100).toLong() }
                            viewModel.save(
                                amountPaise = paise,
                                payee = payee.ifBlank { null },
                                category = selectedCategory,
                            )
                        },
                        enabled = !uiState.isSaving,
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Delete confirmation dialog ──────────────────────────────────
    if (uiState.showDeleteConfirmation) {
        val txn = uiState.transaction
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text("Delete Transaction?", style = Title)
            },
            text = {
                Text(
                    "Delete this ₹${txn?.amountPaise?.let { it / 100 } ?: "—"} transaction" +
                            "${txn?.payee?.let { " to $it" } ?: ""}? This can't be undone.",
                    style = Body,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Delete", style = Label, color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text("Cancel", style = Label, color = colors.textSecondary)
                }
            },
        )
    }
}

/**
 * Confidence banner — icon + color + text (never color-only per PRD §8).
 */
@Composable
private fun ConfidenceBanner(flag: ConfidenceFlag) {
    val colors = ArthixTheme.colors

    val (bgColor, iconTint, icon, text) = when (flag) {
        ConfidenceFlag.AUTO_RESOLVED -> Quadruple(
            colors.statusAutoResolved,
            colors.warning,
            Icons.Outlined.AutoAwesome,
            "This transaction was auto-matched — please confirm the details.",
        )
        ConfidenceFlag.NEEDS_REVIEW -> Quadruple(
            colors.statusNeedsReview,
            colors.error,
            Icons.Outlined.ErrorOutline,
            "This transaction needs review — some details may be inaccurate.",
        )
        else -> return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = Caption,
            color = iconTint,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
