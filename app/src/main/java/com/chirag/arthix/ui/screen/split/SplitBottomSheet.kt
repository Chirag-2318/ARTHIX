package com.chirag.arthix.ui.screen.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.ui.components.VoiceCaptureBottomSheet
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.LabelCaps
import com.chirag.arthix.ui.theme.SectionHeader
import com.chirag.arthix.voice.VoiceIntent
import com.chirag.arthix.voice.VoiceIntentParser
import com.chirag.arthix.voice.VoskSttEngine

object SplitSheetColors {
    val background = Color(0xFFFAF7F2)
    val textPrimary = Color(0xFF1A1A1C)
    val textSecondary = Color(0xFF6E6E73)
    val accent = Color(0xFFE4463A)
    val border = Color(0xFFE5E5EA)
    val surface = Color.White
    val surfaceContainerHigh = Color(0xFFF0EDE8)
    val tagPosBg = Color(0xFFE6F4EA)
    val tagPosText = Color(0xFF1E8E3E)
    val accentSpend = Color(0xFFE4463A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBottomSheet(
    triggerViewModel: SplitTriggerViewModel = hiltViewModel(),
    editViewModel: SplitEditViewModel = hiltViewModel()
) {
    val triggerState by triggerViewModel.state.collectAsState()
    val editState by editViewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shapes = ArthixTheme.shapes

    if (triggerState is SplitTriggerState.Prompting) {
        val prompting = triggerState as SplitTriggerState.Prompting

        LaunchedEffect(prompting.transactionId) {
            editViewModel.initForTransaction(
                prompting.transactionId,
                prompting.suggestedGroup,
                prompting.initialParticipantNames
            )
        }

        ModalBottomSheet(
            onDismissRequest = {
                triggerViewModel.dismissPrompt()
                editViewModel.cancel()
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = SplitSheetColors.background,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = SplitSheetColors.border,
                    width = 36.dp,
                    height = 4.dp
                )
            }
        ) {
            when (val state = editState) {
                is SplitEditState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading split details...", style = BodySecondary, color = SplitSheetColors.textSecondary)
                    }
                }
                is SplitEditState.Active -> {
                    SplitEditContent(
                        state = state,
                        sttEngine = editViewModel.sttEngine,
                        onCustomModeToggle = { editViewModel.setCustomMode(it) },
                        onAddParticipant = { name, _ -> editViewModel.addParticipant(name, null) },
                        onAddParticipants = { names -> editViewModel.addParticipants(names) },
                        onRemoveParticipant = { editViewModel.removeParticipant(it) },
                        onUpdateCustomShare = { id, amount -> editViewModel.updateCustomShare(id, amount) },
                        onConfirm = {
                            editViewModel.confirmSplit(SplitConfirmedVia.TAP)
                            triggerViewModel.dismissPrompt()
                        },
                        onCancel = {
                            triggerViewModel.dismissPrompt()
                            editViewModel.cancel()
                        }
                    )
                }
                is SplitEditState.Done -> {
                    LaunchedEffect(Unit) {
                        triggerViewModel.dismissPrompt()
                    }
                }
            }
        }
    }
}

@Composable
fun SplitEditContent(
    state: SplitEditState.Active,
    sttEngine: VoskSttEngine,
    onCustomModeToggle: (Boolean) -> Unit,
    onAddParticipant: (String, String?) -> Unit,
    onAddParticipants: (List<String>) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onUpdateCustomShare: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var newParticipantName by remember { mutableStateOf("") }
    var showVoiceCapture by remember { mutableStateOf(false) }
    val spacing = ArthixTheme.spacing
    val shapes = ArthixTheme.shapes

    if (showVoiceCapture) {
        VoiceCaptureBottomSheet(
            sttEngine = sttEngine,
            title = "Voice Bill Split",
            promptHint = "Speak names to split with",
            exampleHint = "e.g. \"split with Aman and Priya\" or \"Rahul, Sneha, Rohan\"",
            onDismiss = { showVoiceCapture = false },
            onVoiceIntent = { intent, transcript ->
                when (intent) {
                    is VoiceIntent.Split -> {
                        onAddParticipants(intent.names)
                    }
                    else -> {
                        val parsedSplit = VoiceIntentParser.parseSplitIntent(transcript)
                        if (parsedSplit != null && parsedSplit.names.isNotEmpty()) {
                            onAddParticipants(parsedSplit.names)
                        } else {
                            val names = transcript.split(Regex("\\s*,\\s*|\\s+and\\s+"))
                                .map { it.trim().trim('.', '!', '?') }
                                .filter { it.isNotBlank() && it.length >= 2 }
                                .map { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
                            if (names.isNotEmpty()) {
                                onAddParticipants(names)
                            }
                        }
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // ── Header Bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Split Bill",
                    style = SectionHeader,
                    color = SplitSheetColors.textPrimary
                )
                Text(
                    text = "Total Bill: ${formatPaise(state.totalAmountPaise)}",
                    style = BodySecondary,
                    color = SplitSheetColors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(SplitSheetColors.accent.copy(alpha = 0.12f))
                    .border(1.dp, SplitSheetColors.accent.copy(alpha = 0.3f), shapes.pill)
            ) {
                IconButton(
                    onClick = { showVoiceCapture = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Split",
                        tint = SplitSheetColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Custom Amounts Toggle Card ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.listItem)
                .background(SplitSheetColors.surface)
                .border(1.dp, SplitSheetColors.border, shapes.listItem)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (state.isCustomMode) "Custom Amounts Mode" else "Split Evenly",
                        style = BodyPrimary.copy(fontWeight = FontWeight.Medium),
                        color = SplitSheetColors.textPrimary
                    )
                    Text(
                        text = if (state.isCustomMode) "Specify exact share per person" else "Divided equally among ${state.participants.size} people",
                        style = BodySecondary.copy(fontSize = 11.sp),
                        color = SplitSheetColors.textSecondary
                    )
                }

                Switch(
                    checked = state.isCustomMode,
                    onCheckedChange = { onCustomModeToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SplitSheetColors.surface,
                        checkedTrackColor = SplitSheetColors.accent,
                        uncheckedThumbColor = SplitSheetColors.textSecondary,
                        uncheckedTrackColor = SplitSheetColors.surfaceContainerHigh
                    )
                )
            }
        }

        // Remainder allocation banner when custom mode is on
        if (state.isCustomMode) {
            Spacer(Modifier.height(8.dp))
            val isBalanced = state.remainderToAllocate == 0L
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.input)
                    .background(if (isBalanced) SplitSheetColors.tagPosBg else SplitSheetColors.accentSpend.copy(alpha = 0.15f))
                    .border(1.dp, if (isBalanced) SplitSheetColors.tagPosText.copy(alpha = 0.3f) else SplitSheetColors.accentSpend.copy(alpha = 0.3f), shapes.input)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isBalanced) "✓ All ₹${state.totalAmountPaise / 100.0} allocated perfectly!" else "Remaining to allocate: ${formatPaise(state.remainderToAllocate)}",
                    style = BodySecondary.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isBalanced) SplitSheetColors.tagPosText else SplitSheetColors.accentSpend
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Participants List ────────────────────────────────────────
        Text(
            text = "PARTICIPANTS (${state.participants.size})",
            style = LabelCaps,
            color = SplitSheetColors.textSecondary
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.participants, key = { it.participantId }) { participant ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.listItem)
                        .background(SplitSheetColors.surface)
                        .border(1.dp, SplitSheetColors.border, shapes.listItem)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Initial
                        val initial = (participant.displayName.firstOrNull() ?: 'U').uppercaseChar()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape)
                                .background(if (participant.isAppUser) SplitSheetColors.accent.copy(alpha = 0.2f) else SplitSheetColors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial.toString(),
                                style = BodyPrimary.copy(fontWeight = FontWeight.Bold),
                                color = if (participant.isAppUser) SplitSheetColors.accent else SplitSheetColors.textPrimary
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = participant.displayName + if (participant.isAppUser) " (You)" else "",
                                style = BodyPrimary.copy(fontWeight = if (participant.isAppUser) FontWeight.SemiBold else FontWeight.Normal),
                                color = SplitSheetColors.textPrimary
                            )
                        }

                        // Share / Input
                        if (state.isCustomMode) {
                            OutlinedTextField(
                                value = participant.customOverrideString,
                                onValueChange = { onUpdateCustomShare(participant.participantId, it) },
                                modifier = Modifier.width(96.dp),
                                prefix = { Text("₹", style = BodySecondary, color = SplitSheetColors.textSecondary) },
                                singleLine = true,
                                shape = shapes.input,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SplitSheetColors.accent,
                                    unfocusedBorderColor = SplitSheetColors.border,
                                    focusedTextColor = SplitSheetColors.textPrimary,
                                    unfocusedTextColor = SplitSheetColors.textPrimary
                                )
                            )
                        } else {
                            Text(
                                text = formatPaise(participant.sharePaise),
                                style = BodyPrimary.copy(fontWeight = FontWeight.Bold),
                                color = SplitSheetColors.textPrimary
                            )
                        }

                        // Delete button
                        if (!participant.isAppUser) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { onRemoveParticipant(participant.participantId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = SplitSheetColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Spacer(Modifier.width(36.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Add Participant Field ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newParticipantName,
                onValueChange = { newParticipantName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add name (or speak)...", style = BodySecondary, color = SplitSheetColors.textSecondary) },
                singleLine = true,
                shape = shapes.input,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SplitSheetColors.accent,
                    unfocusedBorderColor = SplitSheetColors.border,
                    focusedTextColor = SplitSheetColors.textPrimary,
                    unfocusedTextColor = SplitSheetColors.textPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = { showVoiceCapture = true }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak name",
                            tint = SplitSheetColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(shapes.input)
                    .background(SplitSheetColors.accent)
            ) {
                IconButton(
                    onClick = {
                        if (newParticipantName.isNotBlank()) {
                            onAddParticipant(newParticipantName.trim(), null)
                            newParticipantName = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Actions ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = shapes.pill,
                border = androidx.compose.foundation.BorderStroke(1.dp, SplitSheetColors.border)
            ) {
                Text("Skip", style = BodyPrimary, color = SplitSheetColors.textSecondary)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                shape = shapes.pill,
                enabled = !state.isCustomMode || state.remainderToAllocate == 0L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplitSheetColors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = SplitSheetColors.surfaceContainerHigh,
                    disabledContentColor = SplitSheetColors.textSecondary
                )
            ) {
                Text(
                    text = "Confirm Split",
                    style = BodyPrimary.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

private fun formatPaise(paise: Long): String {
    val rupees = paise / 100.0
    return if (rupees == rupees.toLong().toDouble()) {
        "₹${rupees.toLong()}"
    } else {
        "₹${"%.2f".format(rupees)}"
    }
}

