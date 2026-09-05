package com.chirag.arthix.ui.screen.split

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

import com.chirag.arthix.data.entity.CloseFriendEntity
import com.chirag.arthix.domain.split.FriendMatchResult
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.voice.VoiceIntent
import com.chirag.arthix.voice.VoiceIntentParser

private object SplitColors {
    val Background = Color(0xFFFAF7F2)
    val Surface = Color.White
    val SurfaceRaised = Color(0xFFF0F0F0)
    val Border = Color(0xFFE5E5E5)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF6E6E73)
    val TextMuted = Color(0xFF8E8E93)
    val Accent = Color(0xFFE4463A) // Coral
    val AccentGradient = Brush.verticalGradient(listOf(Color(0xFFE4463A), Color(0xFFE4463A)))
    val TrackEmpty = Color(0xFFE5E5EA)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    onBack: () -> Unit,
    viewModel: SplitBillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveComplete, uiState.smsSendSummary, uiState.showSaveNewFriendsDialog) {
        if (uiState.saveComplete && uiState.smsSendSummary == null && !uiState.showSaveNewFriendsDialog) {
            onBack()
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.confirmSplit(sendSms = isGranted)
    }

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var editingPhoneParticipant by remember { mutableStateOf<SplitParticipant?>(null) }
    var showVoiceCapture by remember { mutableStateOf(false) }

    if (showVoiceCapture) {
        VoiceCaptureBottomSheet(
            sttEngine = viewModel.sttEngine,
            title = "Voice Bill Split",
            promptHint = "Speak names or bill to split",
            exampleHint = "e.g. \"split 450 with Aman and Priya\" or \"Rahul, Sneha, Rohan\"",
            onDismiss = { showVoiceCapture = false },
            onVoiceIntent = { intent, transcript ->
                when (intent) {
                    is VoiceIntent.Split -> {
                        if (intent.amountPaise != null && intent.amountPaise > 0L) {
                            viewModel.updateAmount(intent.amountPaise)
                        }
                        if (!intent.payee.isNullOrBlank() && uiState.isNewTransaction) {
                            viewModel.updatePayee(intent.payee)
                        }
                        viewModel.addParticipants(intent.names)
                    }
                    is VoiceIntent.Amount -> {
                        viewModel.updateAmount(intent.amountPaise)
                        if (!intent.payee.isNullOrBlank() && uiState.isNewTransaction) {
                            viewModel.updatePayee(intent.payee)
                        }
                    }
                    is VoiceIntent.CategoryAndAmount -> {
                        viewModel.updateAmount(intent.amountPaise)
                        if (!intent.payee.isNullOrBlank() && uiState.isNewTransaction) {
                            viewModel.updatePayee(intent.payee)
                        }
                    }
                    else -> {
                        val parsedSplit = VoiceIntentParser.parseSplitIntent(transcript)
                        if (parsedSplit != null && parsedSplit.names.isNotEmpty()) {
                            if (parsedSplit.amountPaise != null && parsedSplit.amountPaise > 0L) {
                                viewModel.updateAmount(parsedSplit.amountPaise)
                            }
                            viewModel.addParticipants(parsedSplit.names)
                        } else {
                            val stopWords = setOf("logged", "log", "logging", "record", "recorded", "add", "added", "split", "splitting", "bill", "with", "for", "to", "on", "at")
                            val rawTokens = transcript.split(Regex("\\s*,\\s*|\\s+and\\s+"))
                                .map { it.trim().trim('.', '!', '?') }
                                .filter { it.isNotBlank() && it.length >= 2 }
                            val names = rawTokens.map { token ->
                                token.split(Regex("\\s+"))
                                    .filter { it.lowercase() !in stopWords }
                                    .joinToString(" ")
                                    .trim()
                            }.filter { it.isNotBlank() && it.length >= 2 }
                             .map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
                            if (names.isNotEmpty()) {
                                viewModel.addParticipants(names)
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = SplitColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Split Bill", color = SplitColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SplitColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showVoiceCapture = true }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice split", tint = SplitColors.Accent)
                    }
                    IconButton(onClick = { showAddPersonDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add person", tint = SplitColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SplitColors.Background)
            )
        },
        bottomBar = {
            SplitNowButton(
                enabled = !uiState.isSendingSms && uiState.participants.isNotEmpty() && (uiState.totalAmountPaise > 0L) && (!uiState.isNewTransaction || uiState.payee.isNotBlank()),
                isSending = uiState.isSendingSms,
                onClick = {
                    val anyHasPhone = uiState.participants.any { !it.isAppUser && !it.phoneNumber.isNullOrBlank() }
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.SEND_SMS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (anyHasPhone && !hasPermission) {
                        smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                    } else {
                        viewModel.confirmSplit(sendSms = hasPermission)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(SplitColors.Background)
        ) {
            Spacer(Modifier.height(4.dp))
            
            if (uiState.isNewTransaction) {
                // Inline Creation Form
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Total Amount", color = SplitColors.TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    InlineAmountInput(
                        amount = uiState.totalAmountPaise,
                        onAmountChange = { viewModel.updateAmount(it) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Merchant / Shop", color = SplitColors.TextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    InlinePayeeInput(
                        payee = uiState.payee,
                        onPayeeChange = { viewModel.updatePayee(it) }
                    )
                }
            } else {
                TotalBillHeader(totalRupees = uiState.totalAmountPaise / 100.0, billLabel = uiState.payee)
            }

            if (uiState.savedCloseFriends.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                QuickAddCloseFriendsRow(
                    friends = uiState.savedCloseFriends,
                    selectedParticipants = uiState.participants,
                    onToggleFriend = { viewModel.toggleCloseFriend(it) }
                )
            }

            Spacer(Modifier.height(20.dp))
            ParticipantAvatarStrip(
                participants = uiState.participants,
                onRemove = { viewModel.removeParticipant(it) }
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (uiState.splitMode == SplitMode.MANUALLY) "Drag to adjust each share" else "Shares divided equally",
                    color = SplitColors.TextMuted, fontSize = 12.sp
                )
                ModeToggle(
                    currentMode = uiState.splitMode,
                    onModeChange = { viewModel.setSplitMode(it) }
                )
            }

            Spacer(Modifier.height(12.dp))
            SplitPuckRow(
                participants = uiState.participants,
                totalAmountPaise = uiState.totalAmountPaise,
                mode = uiState.splitMode,
                voiceMatches = uiState.voiceMatches,
                onShareChanged = { participantId, newShare ->
                    viewModel.updateShare(participantId, newShare)
                },
                onTogglePaid = { participantId ->
                    viewModel.togglePaidStatus(participantId)
                },
                onRemoveParticipant = { participantId ->
                    viewModel.removeParticipant(participantId)
                },
                onEditPhone = { participant ->
                    editingPhoneParticipant = participant
                },
                onDismissVoiceMatch = { participantId ->
                    viewModel.dismissVoiceMatch(participantId)
                }
            )

            Spacer(Modifier.height(16.dp))
            ReconciliationFooter(
                participants = uiState.participants,
                totalAmountPaise = uiState.totalAmountPaise,
                onAutoBalance = { viewModel.autoBalanceRemaining() }
            )
            
            Spacer(Modifier.height(32.dp)) // bottom padding for scroll
        }
    }
    
    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onAdd = { name, phone -> 
                viewModel.addParticipant(name, phone)
                showAddPersonDialog = false 
            }
        )
    }

    editingPhoneParticipant?.let { p ->
        var editPhoneText by remember(p.id) { mutableStateOf(p.phoneNumber ?: "+91 ") }
        AlertDialog(
            onDismissRequest = { editingPhoneParticipant = null },
            title = { Text("Phone for ${p.name}", color = SplitColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Enter phone number to send an SMS reminder for this split.", color = SplitColors.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPhoneText,
                        onValueChange = { editPhoneText = it },
                        placeholder = { Text("e.g. +91 98765 43210", color = SplitColors.TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = editPhoneText.trim()
                    val finalPhone = if (trimmed == "+91" || trimmed.isBlank()) null else trimmed
                    viewModel.updateParticipantPhone(p.id, finalPhone)
                    editingPhoneParticipant = null
                }) {
                    Text("Save", color = SplitColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPhoneParticipant = null }) {
                    Text("Cancel", color = SplitColors.TextMuted)
                }
            },
            containerColor = SplitColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    uiState.smsSendSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSmsSummary()
                onBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (summary.failedCount == 0) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (summary.failedCount == 0) Color(0xFF34A853) else SplitColors.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (summary.failedCount == 0) "Reminders Sent" else "SMS Summary",
                        color = SplitColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        summary.userMessage,
                        color = SplitColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    if (summary.failureDetails.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        summary.failureDetails.forEach { failure ->
                            Text(
                                "• ${failure.participantName}: ${failure.reason}",
                                color = SplitColors.Accent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSmsSummary()
                    if (!uiState.showSaveNewFriendsDialog) {
                        onBack()
                    }
                }) {
                    Text("OK", color = SplitColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SplitColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (uiState.showSaveNewFriendsDialog && uiState.smsSendSummary == null) {
        val newFriends = uiState.unpromptedNewFriends
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSaveNewFriendsDialog()
                onBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = SplitColors.Accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Close Friends?", color = SplitColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val friendNames = newFriends.joinToString(", ") { it.name }
                Text(
                    text = "Save $friendNames as Close Friend${if (newFriends.size > 1) "s" else ""} for one-tap access in future splits?",
                    color = SplitColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNewFriendsAsCloseFriends(newFriends)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SplitColors.Accent)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissSaveNewFriendsDialog()
                    onBack()
                }) {
                    Text("Not Now", color = SplitColors.TextMuted)
                }
            },
            containerColor = SplitColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Split Error", color = SplitColors.TextPrimary) },
            text = { Text(error, color = SplitColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK", color = SplitColors.Accent)
                }
            },
            containerColor = SplitColors.Surface
        )
    }
}

@Composable
private fun InlineAmountInput(amount: Long, onAmountChange: (Long) -> Unit) {
    val textValue = if (amount == 0L) "" else (amount / 100.0).let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("₹", color = SplitColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 34.sp)
        Spacer(Modifier.width(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = textValue,
            onValueChange = { str ->
                if (str.isEmpty()) onAmountChange(0L)
                else {
                    str.toDoubleOrNull()?.let { onAmountChange((it * 100).toLong()) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SplitColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(SplitColors.Accent),
            decorationBox = { inner ->
                Box {
                    if (textValue.isEmpty()) {
                        Text("0", color = SplitColors.TextMuted, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun InlinePayeeInput(payee: String, onPayeeChange: (String) -> Unit) {
    OutlinedTextField(
        value = payee,
        onValueChange = onPayeeChange,
        placeholder = { Text("e.g. Swiggy, Amazon", color = SplitColors.TextMuted) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SplitColors.Surface,
            unfocusedContainerColor = SplitColors.Surface,
            focusedBorderColor = SplitColors.TextSecondary,
            unfocusedBorderColor = SplitColors.Border,
            focusedTextColor = SplitColors.TextPrimary,
            unfocusedTextColor = SplitColors.TextPrimary,
            cursorColor = SplitColors.Accent
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ModeToggle(currentMode: SplitMode, onModeChange: (SplitMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SplitColors.SurfaceRaised)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentMode == SplitMode.EQUALLY) SplitColors.Accent else Color.Transparent)
                .clickable { onModeChange(SplitMode.EQUALLY) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Equally", color = if (currentMode == SplitMode.EQUALLY) Color.White else SplitColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (currentMode == SplitMode.MANUALLY) SplitColors.Accent else Color.Transparent)
                .clickable { onModeChange(SplitMode.MANUALLY) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Manually", color = if (currentMode == SplitMode.MANUALLY) Color.White else SplitColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TotalBillHeader(totalRupees: Double, billLabel: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Total Bill", color = SplitColors.TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "₹${"%,.2f".format(totalRupees)}",
            color = SplitColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(billLabel, color = SplitColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun ParticipantAvatarStrip(participants: List<SplitParticipant>, onRemove: (String) -> Unit) {
    // Horizontally scrollable row of avatars. 
    // Added removal capability on tap for non-app users.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        participants.forEachIndexed { index, p ->
            Box(
                modifier = Modifier
                    .offset(x = (-10 * index).dp)
                    .size(48.dp)
                    .clip(com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape)
                    .background(p.avatarTint)
                    .border(BorderStroke(2.dp, SplitColors.Background), com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape)
                    .clickable(enabled = !p.isAppUser) { onRemove(p.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(p.avatarInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!p.isAppUser) {
                    // Small visual indicator it can be removed
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp)
                            .size(14.dp).clip(CircleShape).background(SplitColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = SplitColors.TextMuted, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitPuckRow(
    participants: List<SplitParticipant>,
    totalAmountPaise: Long,
    mode: SplitMode,
    voiceMatches: Map<String, FriendMatchResult> = emptyMap(),
    onShareChanged: (String, Long) -> Unit,
    onTogglePaid: (String) -> Unit,
    onRemoveParticipant: ((String) -> Unit)? = null,
    onEditPhone: ((SplitParticipant) -> Unit)? = null,
    onDismissVoiceMatch: ((String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        participants.forEach { participant ->
            SplitPuck(
                participant = participant,
                totalAmountPaise = totalAmountPaise,
                mode = mode,
                voiceMatch = voiceMatches[participant.id],
                onShareChanged = { onShareChanged(participant.id, it) },
                onTogglePaid = { onTogglePaid(participant.id) },
                onRemove = if (onRemoveParticipant != null && !participant.isAppUser) {
                    { onRemoveParticipant(participant.id) }
                } else null,
                onEditPhone = if (onEditPhone != null && !participant.isAppUser) {
                    { onEditPhone(participant) }
                } else null,
                onDismissVoiceMatch = if (onDismissVoiceMatch != null) {
                    { onDismissVoiceMatch(participant.id) }
                } else null
            )
        }
    }
}

@Composable
private fun SplitPuck(
    participant: SplitParticipant,
    totalAmountPaise: Long,
    mode: SplitMode,
    voiceMatch: FriendMatchResult? = null,
    onShareChanged: (Long) -> Unit,
    onTogglePaid: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onEditPhone: (() -> Unit)? = null,
    onDismissVoiceMatch: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val trackHeight = 220.dp
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val fraction = if (totalAmountPaise <= 0L) 0f else (participant.sharePaise.toFloat() / totalAmountPaise.toFloat()).coerceIn(0f, 1f)
    val handleSize = 38.dp
    val handleSizePx = with(density) { handleSize.toPx() }

    val currentTotal by rememberUpdatedState(totalAmountPaise)
    val currentParticipant by rememberUpdatedState(participant)
    val currentOnShareChanged by rememberUpdatedState(onShareChanged)

    var startDragFraction by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(66.dp)
    ) {
        if (voiceMatch != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8F5E9))
                    .border(BorderStroke(0.5.dp, Color(0xFF81C784)), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "✓ Matched",
                    color = Color(0xFF2E7D32),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                if (onDismissVoiceMatch != null) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss voice match",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .clickable { onDismissVoiceMatch() }
                    )
                }
            }
        }

        // Participant Avatar / Paid status Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onTogglePaid() }
                .padding(bottom = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!participant.isAppUser && onEditPhone != null) {
                    Icon(
                        imageVector = if (!participant.phoneNumber.isNullOrBlank()) Icons.Filled.Sms else Icons.Filled.Phone,
                        contentDescription = "SMS Phone",
                        tint = if (!participant.phoneNumber.isNullOrBlank()) SplitColors.Accent else SplitColors.TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .clickable { onEditPhone() }
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    text = participant.name,
                    color = SplitColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (onRemove != null) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = SplitColors.TextMuted,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onRemove() }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (participant.isPaid) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8BA888)) // Sage Green
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Paid ✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, SplitColors.TextMuted, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Pending", color = SplitColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Vertical Slider Cylinder Box
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(trackHeight)
                .clip(RoundedCornerShape(28.dp))
                .background(SplitColors.TrackEmpty)
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (mode == SplitMode.MANUALLY) SplitColors.Accent.copy(alpha = 0.5f) else SplitColors.Border
                    ),
                    RoundedCornerShape(28.dp)
                )
                .pointerInput(participant.id) {
                    detectDragGestures(
                        onDragStart = {
                            startDragFraction = if (currentTotal > 0L) {
                                (currentParticipant.sharePaise.toFloat() / currentTotal.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            accumulatedDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragY += dragAmount.y
                            val deltaFraction = -accumulatedDragY / trackHeightPx
                            val newFraction = (startDragFraction + deltaFraction).coerceIn(0f, 1f)
                            val newPaise = (newFraction * currentTotal).roundToLong()
                            currentOnShareChanged(newPaise)
                        }
                    )
                }
                .pointerInput(participant.id) {
                    detectTapGestures { offset ->
                        val tapFraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                        val newPaise = (tapFraction * currentTotal).roundToLong()
                        currentOnShareChanged(newPaise)
                    }
                }
        ) {
            // Fill level from bottom
            val fillHeight = trackHeight * fraction
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(fillHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                participant.avatarTint,
                                participant.avatarTint.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Percentage badge inside fill
            if (fraction >= 0.16f) {
                Text(
                    text = "${(fraction * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }

            // Tactile Drag Knob / Puck Handle
            val maxTravelPx = (trackHeightPx - handleSizePx).coerceAtLeast(0f)
            val handleOffsetY = with(density) {
                ((1f - fraction) * maxTravelPx).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(y = handleOffsetY)
                    .align(Alignment.TopCenter)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(SplitColors.Background)
                    .border(
                        BorderStroke(
                            2.dp,
                            if (mode == SplitMode.MANUALLY) SplitColors.Accent else participant.avatarTint
                        ),
                        CircleShape
                    )
                    .shadow(elevation = 6.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.UnfoldMore,
                    contentDescription = "Drag to adjust ${participant.name}'s share",
                    tint = if (mode == SplitMode.MANUALLY) SplitColors.Accent else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Editable numeric share input
        val textValue = if (participant.sharePaise == 0L) "" else (participant.sharePaise / 100.0).let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }
        androidx.compose.foundation.text.BasicTextField(
            value = textValue,
            onValueChange = { str ->
                if (str.isEmpty()) onShareChanged(0L)
                else {
                    str.toDoubleOrNull()?.let { onShareChanged((it * 100).toLong()) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SplitColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(SplitColors.Accent),
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SplitColors.Surface)
                        .border(BorderStroke(1.dp, SplitColors.Border), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (textValue.isEmpty()) {
                        Text("₹0", color = SplitColors.TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹", color = SplitColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            inner()
                        }
                    }
                }
            },
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun ReconciliationFooter(
    participants: List<SplitParticipant>,
    totalAmountPaise: Long,
    onAutoBalance: () -> Unit,
) {
    val sum = participants.sumOf { it.sharePaise }
    val balanced = sum == totalAmountPaise
    val diff = totalAmountPaise - sum

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (balanced) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = if (balanced) Color(0xFF34D399) else SplitColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (balanced) "Shares add up to the full bill"
                else if (diff > 0) "Remaining to allocate: ₹${(diff / 100.0).let { if (it % 1 == 0.0) it.toInt() else it }}"
                else "Over-allocated by: ₹${(-diff / 100.0).let { if (it % 1 == 0.0) it.toInt() else it }}",
                color = if (balanced) Color(0xFF34D399) else SplitColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (!balanced && participants.isNotEmpty() && totalAmountPaise > 0L) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAutoBalance,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SplitColors.Accent
                ),
                border = BorderStroke(1.dp, SplitColors.Accent.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Distribute Difference", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SplitNowButton(enabled: Boolean, isSending: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitColors.Background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isSending,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x1A000000)),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitColors.Accent,
                disabledContainerColor = SplitColors.SurfaceRaised,
                contentColor = Color.White,
                disabledContentColor = SplitColors.TextMuted
            )
        ) {
            if (isSending) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Sending SMS reminders...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text("Split Now / Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (name: String, phoneNumber: String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 ") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Participant", color = SplitColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name (e.g. Rahul)", color = SplitColors.TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SplitColors.TextPrimary,
                        unfocusedTextColor = SplitColors.TextPrimary,
                        focusedBorderColor = SplitColors.Accent,
                        unfocusedBorderColor = SplitColors.Border,
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("Phone (e.g. +91 98765 43210)", color = SplitColors.TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SplitColors.TextPrimary,
                        unfocusedTextColor = SplitColors.TextPrimary,
                        focusedBorderColor = SplitColors.Accent,
                        unfocusedBorderColor = SplitColors.Border,
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = phone.trim()
                    val finalPhone = if (trimmed == "+91" || trimmed.isBlank()) null else trimmed
                    onAdd(name, finalPhone)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = SplitColors.Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SplitColors.TextMuted)
            }
        },
        containerColor = SplitColors.Surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun QuickAddCloseFriendsRow(
    friends: List<CloseFriendEntity>,
    selectedParticipants: List<SplitParticipant>,
    onToggleFriend: (CloseFriendEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CLOSE FRIENDS",
            color = SplitColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            friends.forEach { friend ->
                val isSelected = selectedParticipants.any { 
                    !it.isAppUser && (it.name.equals(friend.name, ignoreCase = true) || (!it.phoneNumber.isNullOrBlank() && it.phoneNumber == friend.phoneNumber))
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) SplitColors.Accent else SplitColors.Surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) SplitColors.Accent else SplitColors.Border
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onToggleFriend(friend) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else SplitColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = friend.name,
                            color = if (isSelected) Color.White else SplitColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
