package com.chirag.arthix.ui.screen.manual

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.ocr.ReceiptCaptureActivity
import com.chirag.arthix.ui.components.CategoryChipRow
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Label
import com.chirag.arthix.ui.theme.Title

/**
 * Manual entry screen — the always-reachable fallback (FR-5, EC-35).
 *
 * Accessible via the FAB on every top-level screen. Creates a new
 * MANUAL-sourced CONFIRMED transaction on save.
 *
 * Styled with design system: dark bg, pill button, category chip row,
 * styled text fields with accent focus color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ArthixTheme.colors
    val context = LocalContext.current

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    var showVoiceCapture by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val amount = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_AMOUNT)
            val payee = data?.getStringExtra(ReceiptCaptureActivity.EXTRA_PREFILL_PAYEE)
            viewModel.openWithPrefill(ManualEntryPrefill(amount = amount, payee = payee))
        }
    }

    if (showVoiceCapture) {
        VoiceCaptureBottomSheet(
            sttEngine = viewModel.sttEngine,
            onDismiss = { showVoiceCapture = false },
            onResult = { prefill ->
                viewModel.openWithPrefill(prefill)
            }
        )
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Transaction",
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
                    IconButton(onClick = { cameraLauncher.launch(ReceiptCaptureActivity.createIntent(context)) }) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = "Scan Receipt",
                            tint = colors.textPrimary,
                        )
                    }
                    IconButton(onClick = { showVoiceCapture = true }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Amount ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Amount", style = Label, color = colors.textSecondary)
                Text(
                    "Tap mic or camera to auto-fill",
                    style = Caption,
                    color = colors.accent,
                )
            }
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                placeholder = { Text("0.00", style = Body) },
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
                trailingIcon = {
                    Row {
                        IconButton(onClick = { cameraLauncher.launch(ReceiptCaptureActivity.createIntent(context)) }) {
                            Icon(
                                Icons.Outlined.CameraAlt,
                                contentDescription = "Scan receipt",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { showVoiceCapture = true }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            )

            // ── Payee ───────────────────────────────────────────────
            Text("Payee", style = Label, color = colors.textSecondary)
            OutlinedTextField(
                value = uiState.payee,
                onValueChange = { viewModel.updatePayee(it) },
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

            // ── Category ────────────────────────────────────
            Text("Category", style = Label, color = colors.textSecondary)
            CategoryChipRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
            )

            Spacer(Modifier.weight(1f))

            // ── Save button ─────────────────────────────────────────
            PrimaryButton(
                text = if (uiState.isSaving) "Saving…" else "Save Transaction",
                onClick = { viewModel.save() },
                enabled = !uiState.isSaving && uiState.amount.isNotBlank(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
