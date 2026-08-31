package com.chirag.arthix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Title
import com.chirag.arthix.ui.theme.Label

@Composable
fun DeleteTxnDialog(
    amountPaise: Long?,
    payee: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ArthixTheme.colors
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Delete Transaction?",
                    color = colors.textPrimary,
                    style = Title,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val amountStr = if (amountPaise != null) (amountPaise / 100.0).toString().removeSuffix(".0") else "0"
                val payeeStr = if (!payee.isNullOrBlank()) " to $payee" else ""
                
                Text(
                    text = "Delete this ₹$amountStr transaction$payeeStr? This can't be undone.",
                    color = colors.textSecondary,
                    style = Body,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", style = Label, color = colors.textSecondary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("Delete", style = Label, color = colors.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
