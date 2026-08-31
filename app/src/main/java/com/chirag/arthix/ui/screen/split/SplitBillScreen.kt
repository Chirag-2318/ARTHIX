package com.chirag.arthix.ui.screen.split

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

private object SplitColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceRaised = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)
    val Accent = Color(0xFFFF7A1A)
    val AccentGradient = Brush.verticalGradient(listOf(Color(0xFFFF9142), Color(0xFFFF5B3D)))
    val TrackEmpty = Color(0xFF232329)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    onBack: () -> Unit,
    viewModel: SplitBillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onBack()
    }
    
    var showAddPersonDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SplitColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Split Bill", color = SplitColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SplitColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPersonDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add person", tint = SplitColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SplitColors.Background)
            )
        },
        bottomBar = {
            SplitNowButton(
                enabled = uiState.participants.isNotEmpty() && (uiState.totalAmountPaise > 0L) && (!uiState.isNewTransaction || uiState.payee.isNotBlank()),
                onClick = { viewModel.confirmSplit() }
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
                onShareChanged = { participantId, newShare ->
                    viewModel.updateShare(participantId, newShare)
                },
                onTogglePaid = { participantId ->
                    viewModel.togglePaidStatus(participantId)
                }
            )

            Spacer(Modifier.height(16.dp))
            ReconciliationFooter(
                participants = uiState.participants,
                totalAmountPaise = uiState.totalAmountPaise
            )
            
            Spacer(Modifier.height(32.dp)) // bottom padding for scroll
        }
    }
    
    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onAdd = { 
                viewModel.addParticipant(it)
                showAddPersonDialog = false 
            }
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
            .clip(RoundedCornerShape(12.dp))
            .background(SplitColors.Surface)
            .border(BorderStroke(1.dp, SplitColors.Border), RoundedCornerShape(12.dp))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (currentMode == SplitMode.EQUALLY) SplitColors.SurfaceRaised else Color.Transparent)
                .clickable { onModeChange(SplitMode.EQUALLY) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Equally", color = if (currentMode == SplitMode.EQUALLY) SplitColors.Accent else SplitColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (currentMode == SplitMode.MANUALLY) SplitColors.SurfaceRaised else Color.Transparent)
                .clickable { onModeChange(SplitMode.MANUALLY) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Manually", color = if (currentMode == SplitMode.MANUALLY) SplitColors.Accent else SplitColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    .clip(CircleShape)
                    .background(p.avatarTint)
                    .border(BorderStroke(2.dp, SplitColors.Background), CircleShape)
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
    onShareChanged: (participantId: String, newSharePaise: Long) -> Unit,
    onTogglePaid: (participantId: String) -> Unit,
) {
    val trackHeight = 220.dp
    // Using a horizontal scroll if there are too many people
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = if (participants.size > 4) Arrangement.spacedBy(16.dp) else Arrangement.SpaceEvenly
    ) {
        participants.forEach { p ->
            SplitPuck(
                participant = p,
                totalAmountPaise = totalAmountPaise,
                trackHeight = trackHeight,
                isInteractive = mode == SplitMode.MANUALLY,
                onShareChanged = { newShare -> onShareChanged(p.id, newShare) },
                onTogglePaid = { onTogglePaid(p.id) }
            )
        }
    }
}

@Composable
private fun SplitPuck(
    participant: SplitParticipant,
    totalAmountPaise: Long,
    trackHeight: Dp,
    isInteractive: Boolean,
    onShareChanged: (Long) -> Unit,
    onTogglePaid: () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val fraction = if (totalAmountPaise == 0L) 0f else (participant.sharePaise.toFloat() / totalAmountPaise.toFloat()).coerceIn(0f, 1f)
    val fillHeight = trackHeight * fraction

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onTogglePaid() }) {
            Icon(
                if (participant.isPaid) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (participant.isPaid) "Paid" else "Unpaid",
                tint = if (participant.isPaid) Color(0xFF34D399) else SplitColors.TextMuted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(participant.name, color = SplitColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(52.dp)
                .height(trackHeight)
                .clip(RoundedCornerShape(26.dp))
                .background(SplitColors.TrackEmpty)
                .border(BorderStroke(1.dp, SplitColors.Border), RoundedCornerShape(26.dp))
                .let {
                    if (isInteractive) {
                        it.pointerInput(participant.id, totalAmountPaise) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                val currentFraction = (participant.sharePaise.toFloat() / totalAmountPaise.toFloat())
                                val deltaFraction = -dragAmount / trackHeightPx
                                val newFraction = (currentFraction + deltaFraction).coerceIn(0f, 1f)
                                onShareChanged((newFraction * totalAmountPaise).roundToInt().toLong())
                            }
                        }
                    } else it
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fillHeight)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isInteractive) SplitColors.AccentGradient else Brush.verticalGradient(listOf(Color(0xFF555555), Color(0xFF333333))))
            )

            if (isInteractive) {
                Box(
                    modifier = Modifier
                        .padding(bottom = (fillHeight - 22.dp).coerceAtLeast(0.dp))
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SplitColors.Background)
                        .border(BorderStroke(2.dp, SplitColors.Accent), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.UnfoldMore, contentDescription = "Drag to adjust ${participant.name}'s share", tint = SplitColors.Accent, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        
        if (isInteractive) {
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
                    Box(contentAlignment = Alignment.Center) {
                        if (textValue.isEmpty()) {
                            Text("0", color = SplitColors.TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                },
                modifier = Modifier.width(60.dp)
            )
        } else {
            Text(
                "₹${participant.sharePaise / 100}",
                color = SplitColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ReconciliationFooter(participants: List<SplitParticipant>, totalAmountPaise: Long) {
    val sum = participants.sumOf { it.sharePaise }
    val balanced = sum == totalAmountPaise
    val diff = totalAmountPaise - sum
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (balanced) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = if (balanced) Color(0xFF34D399) else SplitColors.Accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (balanced) "Shares add up to the full bill" else "Remaining to allocate: ₹${(diff / 100.0).let { if (it % 1 == 0.0) it.toInt() else it }}",
            color = if (balanced) Color(0xFF34D399) else SplitColors.Accent,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SplitNowButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SplitColors.Background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SplitColors.Accent,
                disabledContainerColor = SplitColors.SurfaceRaised,
                contentColor = Color.White,
                disabledContentColor = SplitColors.TextMuted
            )
        ) {
            Text("Split Now / Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AddPersonDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Participant", color = SplitColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Name", color = SplitColors.TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SplitColors.TextPrimary,
                    unfocusedTextColor = SplitColors.TextPrimary,
                    focusedBorderColor = SplitColors.Accent,
                    unfocusedBorderColor = SplitColors.Border,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name) }, enabled = name.isNotBlank()) {
                Text("Add", color = SplitColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SplitColors.TextMuted)
            }
        },
        containerColor = SplitColors.Surface,
        shape = RoundedCornerShape(12.dp)
    )
}
