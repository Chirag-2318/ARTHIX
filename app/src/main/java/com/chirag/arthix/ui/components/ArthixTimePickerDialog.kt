package com.chirag.arthix.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import java.time.LocalTime

/**
 * Universal time picker modal styled with ARTHIX design system tokens.
 *
 * Allows users to set or edit the time of payment taken from the receipt or entered manually.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArthixTimePickerDialog(
    initialHour: Int = LocalTime.now().hour,
    initialMinute: Int = LocalTime.now().minute,
    is24Hour: Boolean = false,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ArthixTheme.colors

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text(
                    text = "Confirm",
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = colors.textSecondary,
                )
            }
        },
        title = {
            Text(
                text = "Time of Payment",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFF1E2026),
                    selectorColor = colors.accent,
                    containerColor = colors.surfaceElevated,
                    periodSelectorBorderColor = colors.border,
                    periodSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                    periodSelectorUnselectedContainerColor = Color(0xFF1E2026),
                    periodSelectorSelectedContentColor = colors.accent,
                    periodSelectorUnselectedContentColor = colors.textSecondary,
                    timeSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                    timeSelectorUnselectedContainerColor = Color(0xFF1E2026),
                    timeSelectorSelectedContentColor = colors.accent,
                    timeSelectorUnselectedContentColor = colors.textPrimary,
                )
            )
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surfaceElevated,
    )
}
