package com.chirag.arthix.ui.screen.account

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.R
import kotlinx.coroutines.delay

private val BgCream = Color(0xFFFAF7F2)
private val AccentCoral = Color(0xFFE4463A)
private val InputBg = Color(0xFFFFFFFF)
private val TextNearBlack = Color(0xFF1A1A1A)
private val TextMutedGray = Color(0xFF6B6B6B)
private val BorderGray = Color(0xFFE5E5E5)

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed Background Image
        Image(
            painter = painterResource(id = R.drawable.ac),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Cream to Transparent Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to BgCream,
                        0.55f to Color.Transparent
                    )
                )
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = screenHeight)
                        .systemBarsPadding()
                        .imePadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = showHeader,
                            enter = fadeIn(tween(400)),
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp)
                        ) {
                            Text("Arthix", color = TextNearBlack, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(Modifier.height(48.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            // Headline & Subtext
                            AnimatedVisibility(
                                visible = showHeader,
                                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                            ) {
                                Column {
                                    Text(
                                        text = "Log expenses without\ntyping or talking",
                                        color = TextNearBlack,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 36.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Create an account to get started. Nothing you enter here leaves your phone.",
                                        color = TextMutedGray,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            // Inputs
                            AnimatedVisibility(
                                visible = showContent,
                                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
                            ) {
                                Column {
                                    // Name Field
                                    OutlinedTextField(
                                        value = uiState.name,
                                        onValueChange = { viewModel.updateName(it) },
                                        placeholder = { Text("Enter your name", color = TextMutedGray) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = InputBg.copy(alpha = 0.95f),
                                            unfocusedContainerColor = InputBg.copy(alpha = 0.9f),
                                            focusedBorderColor = AccentCoral,
                                            unfocusedBorderColor = BorderGray.copy(alpha = 0.5f),
                                            focusedTextColor = TextNearBlack,
                                            unfocusedTextColor = TextNearBlack,
                                            cursorColor = AccentCoral,
                                            errorBorderColor = AccentCoral,
                                            errorCursorColor = AccentCoral,
                                            errorContainerColor = InputBg,
                                        ),
                                        isError = uiState.nameError != null && uiState.name.isNotEmpty(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.05f),
                                                spotColor = Color.Black.copy(alpha = 0.05f)
                                            )
                                            .onFocusChanged { isNameFocused = it.isFocused }
                                    )

                                    if (uiState.nameError != null && uiState.name.isNotEmpty()) {
                                        Text(
                                            text = uiState.nameError!!,
                                            color = AccentCoral,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(20.dp))

                                    // Phone Field
                                    OutlinedTextField(
                                        value = uiState.phone,
                                        onValueChange = { viewModel.updatePhone(it) },
                                        placeholder = { Text("Phone number (optional)", color = TextMutedGray) },
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
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = InputBg.copy(alpha = 0.95f),
                                            unfocusedContainerColor = InputBg.copy(alpha = 0.9f),
                                            focusedBorderColor = AccentCoral,
                                            unfocusedBorderColor = BorderGray.copy(alpha = 0.5f),
                                            focusedTextColor = TextNearBlack,
                                            unfocusedTextColor = TextNearBlack,
                                            cursorColor = AccentCoral,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.05f),
                                                spotColor = Color.Black.copy(alpha = 0.05f)
                                            )
                                            .onFocusChanged { isPhoneFocused = it.isFocused }
                                    )
                                    Text(
                                        text = "USED TO IDENTIFY YOU WHEN SPLITTING BILLS WITH FRIENDS.",
                                        color = TextMutedGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))

                    // Bottom Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
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
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentCoral,
                                        disabledContainerColor = AccentCoral.copy(alpha = 0.4f),
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(percent = 50),
                                            ambientColor = AccentCoral.copy(alpha = 0.1f),
                                            spotColor = AccentCoral.copy(alpha = 0.2f)
                                        )
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
                                    // Deep muted color for legibility on grassy background
                                    color = Color.White, 
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
