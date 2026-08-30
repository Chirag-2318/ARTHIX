package com.chirag.arthix.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chirag.arthix.ui.screen.manual.ManualEntryPrefill
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Title
import com.chirag.arthix.voice.SttResult
import com.chirag.arthix.voice.VoiceIntent
import com.chirag.arthix.voice.VoiceIntentParser
import com.chirag.arthix.voice.VoskSttEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data object Listening : VoiceUiState
    data class Success(val transcript: String, val prefill: ManualEntryPrefill) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCaptureBottomSheet(
    sttEngine: VoskSttEngine,
    title: String = "Voice Quick Log",
    promptHint: String = "Listening… speak amount & category",
    exampleHint: String = "e.g. \"350 on food\", \"twelve hundred cab\", \"split with Aman\"",
    onDismiss: () -> Unit,
    onVoiceIntent: ((VoiceIntent, String) -> Unit)? = null,
    onResult: (ManualEntryPrefill) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ArthixTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<VoiceUiState>(VoiceUiState.Idle) }

    fun startListening() {
        uiState = VoiceUiState.Listening
        scope.launch {
            val result = sttEngine.recognize()
            withContext(Dispatchers.Main) {
                when (result) {
                    is SttResult.Recognized -> {
                        val parsed = VoiceIntentParser.parse(result.text)
                        val prefill = when (parsed) {
                            is VoiceIntent.CategoryAndAmount -> ManualEntryPrefill(
                                amount = "%.2f".format(parsed.amountPaise / 100.0),
                                category = parsed.category,
                            )
                            is VoiceIntent.Amount -> ManualEntryPrefill(
                                amount = "%.2f".format(parsed.amountPaise / 100.0),
                            )
                            is VoiceIntent.Category -> ManualEntryPrefill(
                                category = parsed.category,
                            )
                            is VoiceIntent.Discard -> ManualEntryPrefill()
                            is VoiceIntent.Split -> ManualEntryPrefill(
                                payee = parsed.names.firstOrNull(),
                            )
                            is VoiceIntent.Unclear -> ManualEntryPrefill(
                                payee = result.text,
                            )
                        }
                        uiState = VoiceUiState.Success(result.text, prefill)
                        delay(700)
                        if (onVoiceIntent != null) {
                            onVoiceIntent(parsed, result.text)
                        } else {
                            onResult(prefill)
                        }
                        onDismiss()
                    }
                    is SttResult.LowConfidence -> {
                        val prefill = ManualEntryPrefill(payee = result.text)
                        val parsed = VoiceIntentParser.parse(result.text)
                        uiState = VoiceUiState.Success(result.text, prefill)
                        delay(700)
                        if (onVoiceIntent != null) {
                            onVoiceIntent(parsed, result.text)
                        } else {
                            onResult(prefill)
                        }
                        onDismiss()
                    }
                    is SttResult.Timeout -> {
                        uiState = VoiceUiState.Error("Didn't catch that. Please tap the mic and try again.")
                    }
                    is SttResult.Error -> {
                        uiState = VoiceUiState.Error(result.cause)
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            uiState = VoiceUiState.Error("Microphone permission required for voice logging")
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = Title,
                color = colors.textPrimary,
            )

            // Animated Mic Pulser
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (uiState is VoiceUiState.Listening) 1.25f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "mic_scale",
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when (uiState) {
                            is VoiceUiState.Listening -> colors.accent.copy(alpha = 0.25f)
                            is VoiceUiState.Success -> colors.success.copy(alpha = 0.25f)
                            is VoiceUiState.Error -> colors.error.copy(alpha = 0.25f)
                            else -> colors.chipBg
                        }
                    ),
            ) {
                IconButton(
                    onClick = {
                        if (uiState !is VoiceUiState.Listening) {
                            startListening()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when (uiState) {
                                is VoiceUiState.Listening -> colors.accent
                                is VoiceUiState.Success -> colors.success
                                is VoiceUiState.Error -> colors.error
                                else -> colors.accent
                            }
                        ),
                ) {
                    Icon(
                        imageVector = when (uiState) {
                            is VoiceUiState.Success -> Icons.Outlined.Check
                            is VoiceUiState.Error -> Icons.Outlined.ErrorOutline
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Voice Action",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Status message
            when (val state = uiState) {
                is VoiceUiState.Listening -> {
                    Text(
                        text = promptHint,
                        style = Body,
                        color = colors.accent,
                    )
                    Text(
                        text = exampleHint,
                        style = Caption,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                is VoiceUiState.Success -> {
                    Text(
                        text = "Recognized: \"${state.transcript}\"",
                        style = Body,
                        color = colors.success,
                    )
                }
                is VoiceUiState.Error -> {
                    Text(
                        text = state.message,
                        style = Body,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    Text(
                        text = "Tap to speak",
                        style = Body,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
