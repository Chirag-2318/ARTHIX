package com.chirag.arthix.ui.screen.split

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Label
import com.chirag.arthix.ui.theme.Title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBottomSheet(
    triggerViewModel: SplitTriggerViewModel = hiltViewModel(),
    editViewModel: SplitEditViewModel = hiltViewModel()
) {
    val triggerState by triggerViewModel.state.collectAsState()
    val editState by editViewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (triggerState is SplitTriggerState.Prompting) {
        val prompting = triggerState as SplitTriggerState.Prompting
        
        LaunchedEffect(prompting.transactionId) {
            editViewModel.initForTransaction(prompting.transactionId, prompting.suggestedGroup)
        }
        
        ModalBottomSheet(
            onDismissRequest = {
                triggerViewModel.dismissPrompt()
                editViewModel.cancel()
            },
            sheetState = sheetState,
            containerColor = ArthixTheme.colors.surface
        ) {
            when (val state = editState) {
                is SplitEditState.Loading -> {
                    // Loading UI
                    Text("Loading...", modifier = Modifier.padding(16.dp))
                }
                is SplitEditState.Active -> {
                    SplitEditContent(
                        state = state,
                        onCustomModeToggle = { editViewModel.setCustomMode(it) },
                        onAddParticipant = { name, _ -> editViewModel.addParticipant(name, null) }, // Always ad-hoc for now
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
    onCustomModeToggle: (Boolean) -> Unit,
    onAddParticipant: (String, String?) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onUpdateCustomShare: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var newParticipantName by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Text("Split with?", style = Title, color = ArthixTheme.colors.textPrimary)
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Custom amounts?", color = ArthixTheme.colors.textPrimary)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = state.isCustomMode,
                onCheckedChange = { onCustomModeToggle(it) }
            )
        }
        
        if (state.isCustomMode) {
            Text(
                text = "Remaining to allocate: ₹${state.remainderToAllocate / 100.0}",
                color = if (state.remainderToAllocate == 0L) Color.Green else Color.Red
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(state.participants) { participant ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        participant.displayName + if (participant.isAppUser) " (You)" else "", 
                        modifier = Modifier.weight(1f),
                        color = ArthixTheme.colors.textPrimary
                    )
                    
                    if (state.isCustomMode) {
                        // Very simple text field for paise. In a real app this would be a currency formatter
                        OutlinedTextField(
                            value = participant.customOverrideString,
                            onValueChange = { 
                                onUpdateCustomShare(participant.participantId, it) 
                            },
                            modifier = Modifier.width(100.dp)
                        )
                    } else {
                        Text("₹${participant.sharePaise / 100.0}", color = ArthixTheme.colors.textPrimary)
                    }
                    
                    if (!participant.isAppUser) {
                        IconButton(onClick = { onRemoveParticipant(participant.participantId) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = ArthixTheme.colors.textSecondary)
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Add ad-hoc participant
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newParticipantName,
                onValueChange = { newParticipantName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add someone...") }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    if (newParticipantName.isNotBlank()) {
                        onAddParticipant(newParticipantName, null)
                        newParticipantName = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = ArthixTheme.colors.accent)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !state.isCustomMode || state.remainderToAllocate == 0L,
                colors = ButtonDefaults.buttonColors(containerColor = ArthixTheme.colors.accent)
            ) {
                Text("Confirm Split")
            }
        }
    }
}
