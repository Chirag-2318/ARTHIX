package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary

/**
 * Security status card from the Stitch Account Home design.
 *
 * A list-style card with rows of icon + label + status text,
 * separated by hairline dividers. Surface-elevated bg, border-hairline
 * border, rounded-lg corners.
 */

data class SecurityStatusRow(
    val icon: ImageVector,
    val label: String,
    val status: String,
    val isPositive: Boolean = true,
)

@Composable
fun SecurityStatusCard(
    rows: List<SecurityStatusRow>,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors
    val shapes = ArthixTheme.shapes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.listItem)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, shapes.listItem),
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = row.icon,
                        contentDescription = row.label,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = row.label,
                        style = BodyPrimary,
                        color = colors.textPrimary,
                    )
                }

                Text(
                    text = row.status,
                    style = BodySecondary,
                    color = if (row.isPositive) colors.tagPosText else colors.accentWarning,
                )
            }

            if (index < rows.lastIndex) {
                HorizontalDivider(
                    color = colors.border,
                    thickness = 1.dp,
                )
            }
        }
    }
}
