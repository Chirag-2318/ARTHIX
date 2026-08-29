package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Caption
import com.chirag.arthix.ui.theme.Title

/**
 * Activity list row — past-transaction row (PRD §5, §6.8).
 *
 * Mirrors Uber's Activity list: title, date, amount, status subtext.
 * Selected state shows white border (like ride-option row).
 *
 * Used by: History screen, disambiguation candidate list.
 */
@Composable
fun ActivityListRow(
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    statusTag: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors
    val shape = RoundedCornerShape(8.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.textPrimary, shape)
                else Modifier.border(1.dp, colors.border, shape)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = Title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (statusTag != null) {
                    Spacer(Modifier.width(8.dp))
                    statusTag()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = Caption,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = amount,
            style = Title.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = amountColor,
        )
    }
}
