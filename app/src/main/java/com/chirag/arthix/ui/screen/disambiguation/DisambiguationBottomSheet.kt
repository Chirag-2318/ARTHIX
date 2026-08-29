package com.chirag.arthix.ui.screen.disambiguation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirag.arthix.notification.DisambiguationCandidate
import com.chirag.arthix.ui.components.ActivityListRow
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.StatusTag
import com.chirag.arthix.ui.components.StatusTagConfig
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Display
import com.chirag.arthix.ui.theme.Label

/**
 * Disambiguation drawer (PRD §6.6).
 *
 * Mirrors Uber's "Choose a ride" bottom sheet. Triggered reactively
 * when the ReconciliationEngine emits a DisambiguationPrompt.
 * Shows a list of pending shakes (candidates) with an automatic
 * timeout progress line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisambiguationBottomSheet(
    viewModel: DisambiguationViewModel = hiltViewModel()
) {
    val prompt by viewModel.currentPrompt.collectAsStateWithLifecycle()
    val selectedCandidateId by viewModel.selectedCandidateId.collectAsStateWithLifecycle()
    val timeLeft by viewModel.timeLeftMs.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = ArthixTheme.colors

    if (prompt != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.ignore() },
            sheetState = sheetState,
            containerColor = colors.surface,
            dragHandle = {
                // Custom small drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.border)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Header
                Text(
                    text = "Multiple shakes detected",
                    style = Display,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Which shake matches your ₹${prompt!!.amountPaise / 100} payment to ${prompt!!.payee}?",
                    style = Body,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(24.dp))

                // Options
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(prompt!!.candidates) { candidate ->
                        CandidateRow(
                            candidate = candidate,
                            isSelected = selectedCandidateId == candidate.captureId,
                            onClick = { viewModel.selectCandidate(candidate.captureId) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action
                PrimaryButton(
                    text = "Confirm Match",
                    onClick = { viewModel.confirmSelection() },
                    enabled = selectedCandidateId != null,
                )
                
                // Progress / Timeout (FR-15 / auto_resolve)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Auto-matching in ${maxOf(0, timeLeft / 1000)}s",
                        style = Caption,
                        color = colors.textSecondary,
                    )
                }
                
                // Bottom spacing for system nav
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: DisambiguationCandidate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ArthixTheme.colors
    
    ActivityListRow(
        title = "Shake",
        subtitle = "${candidate.approximateSecondsAgo} seconds ago",
        amount = "",
        amountColor = Color.Transparent,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.bg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Vibration,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        statusTag = if (candidate.category != null) {
            {
                StatusTag(
                    config = StatusTagConfig(
                        text = candidate.category,
                        icon = Icons.Outlined.Check,
                        bgColor = colors.border,
                        textColor = colors.textSecondary
                    )
                )
            }
        } else null,
        isSelected = isSelected,
        onClick = onClick,
    )
}
