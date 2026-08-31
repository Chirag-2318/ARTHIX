package com.chirag.arthix.ui.screen.manual

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.data.model.Direction
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
    val shapes = ArthixTheme.shapes
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

    AddTransactionScreen(
        direction = uiState.direction,
        amount = uiState.amount,
        payee = uiState.payee,
        selectedCategory = uiState.selectedCategory,
        isSaving = uiState.isSaving,
        onDirectionChange = { 
            viewModel.updateDirection(it)
            // Auto-select a default category when switching
            if (it == Direction.OUTFLOW) viewModel.selectCategory("Food") 
            else viewModel.selectCategory("Salary") 
        },
        onAmountChange = { viewModel.updateAmount(it) },
        onPayeeChange = { viewModel.updatePayee(it) },
        onCategoryChange = { viewModel.selectCategory(it) },
        onBackClick = onNavigateBack,
        onCameraClick = { cameraLauncher.launch(ReceiptCaptureActivity.createIntent(context)) },
        onMicClick = { showVoiceCapture = true },
        onLogExpense = { viewModel.save() }
    )
}
