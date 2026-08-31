package com.chirag.arthix.ui.screen.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import android.view.HapticFeedbackConstants

private val BgBlack = Color(0xFF0A0A0A)
private val AccentRed = Color(0xFFEE4444)
private val FieldGray = Color(0xFF1C1C1E)
private val TextGray = Color(0xFF8E8E93)

@Composable
fun CreateAccountScreen(
    onAccountCreated: () -> Unit,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var isNameFocused by remember { mutableStateOf(false) }
    var isPhoneFocused by remember { mutableStateOf(false) }

    // Staggered animation triggers
    var showHeader by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var showFooter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showHeader = true
        delay(150)
        showContent = true
        delay(150)
        showFooter = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .imePadding()
            .verticalScroll(scrollState),
    ) {
        Spacer(Modifier.height(64.dp))

        AnimatedVisibility(
            visible = showHeader,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        // Blurred radial glow behind icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AccentRed.copy(alpha = 0.5f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(AccentRed, AccentRed.copy(alpha = 0.8f))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Arthix", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Log expenses without\ntyping or talking",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Create an account to get started. Nothing you enter here leaves your phone.",
                    color = TextGray,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            Column {
                // Name Field
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = { Text("Enter your name", color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldGray,
                        unfocusedContainerColor = FieldGray,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AccentRed,
                        errorBorderColor = AccentRed,
                        errorCursorColor = AccentRed
                    ),
                    isError = uiState.nameError != null && uiState.name.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isNameFocused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = when {
                                uiState.nameError != null && uiState.name.isNotEmpty() -> AccentRed
                                isNameFocused -> AccentRed.copy(alpha = 0.5f)
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                )

                if (uiState.nameError != null && uiState.name.isNotEmpty()) {
                    Text(
                        text = uiState.nameError!!,
                        color = AccentRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Phone Field
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    placeholder = { Text("Phone number (optional)", color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                            if (!uiState.isSaving) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.submit(onSuccess = onAccountCreated)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldGray,
                        unfocusedContainerColor = FieldGray,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AccentRed,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isPhoneFocused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = if (isPhoneFocused) AccentRed.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                )
                Text(
                    text = "Used to identify you when splitting bills with friends.",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(
            visible = showFooter,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            Column {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        focusManager.clearFocus()
                        viewModel.submit(onSuccess = onAccountCreated)
                    },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Get Started", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "By continuing you agree your details stay on this device.",
                    color = TextGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
