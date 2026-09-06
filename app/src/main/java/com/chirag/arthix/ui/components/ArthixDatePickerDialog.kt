package com.chirag.arthix.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme

/**
 * Universal date picker modal styled with ARTHIX design system tokens.
 *
 * Enforces future-date guard: dates after today are disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArthixDatePickerDialog(
    initialDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val todayMillis = System.currentTimeMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: todayMillis,
        selectableDates = object : SelectableDates {
            // Guard: no future dates allowed (with a 1-day timezone buffer)
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayMillis + 86_400_000L
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedUtc ->
                        onDateSelected(selectedUtc)
                    }
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
        shape = RoundedCornerShape(20.dp),
        colors = DatePickerDefaults.colors(
            containerColor = colors.surfaceElevated,
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = colors.surfaceElevated,
                titleContentColor = colors.textPrimary,
                headlineContentColor = colors.textPrimary,
                weekdayContentColor = colors.textSecondary,
                subheadContentColor = colors.textSecondary,
                yearContentColor = colors.textPrimary,
                currentYearContentColor = colors.accent,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = colors.accent,
                dayContentColor = colors.textPrimary,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = colors.accent,
                todayContentColor = colors.accent,
                todayDateBorderColor = colors.accent,
            )
        )
    }
}
