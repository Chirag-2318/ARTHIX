package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Label

/**
 * Status tag — small pill label (PRD §5).
 *
 * Mirrors Uber's "Good deal" / "Cancelled" tag pattern.
 * Every tag pairs color + icon + text (never color-only per PRD §8).
 *
 * Used for: confidence_flag badges (EC-15/22/30/32), status enum (EC-53).
 */

data class StatusTagConfig(
    val text: String,
    val icon: ImageVector,
    val bgColor: Color,
    val textColor: Color,
)

@Composable
fun StatusTag(
    config: StatusTagConfig,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(config.bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.textColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = config.text,
            style = Label.copy(fontSize = 11.sp),
            color = config.textColor,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * Convenience: get StatusTagConfig from ConfidenceFlag.
 */
@Composable
fun confidenceTagConfig(flag: ConfidenceFlag): StatusTagConfig? {
    val colors = ArthixTheme.colors
    return when (flag) {
        ConfidenceFlag.CLEAN -> null // No badge for clean records
        ConfidenceFlag.AUTO_RESOLVED -> StatusTagConfig(
            text = "Auto-matched",
            icon = Icons.Outlined.AutoAwesome,
            bgColor = colors.statusAutoResolved,
            textColor = colors.warning,
        )
        ConfidenceFlag.NEEDS_REVIEW -> StatusTagConfig(
            text = "Needs review",
            icon = Icons.Outlined.ErrorOutline,
            bgColor = colors.statusNeedsReview,
            textColor = colors.error,
        )
    }
}

/**
 * Convenience: get StatusTagConfig from TransactionStatus.
 */
@Composable
fun statusTagConfig(status: TransactionStatus): StatusTagConfig? {
    val colors = ArthixTheme.colors
    return when (status) {
        TransactionStatus.CONFIRMED -> StatusTagConfig(
            text = "Confirmed",
            icon = Icons.Outlined.CheckCircleOutline,
            bgColor = colors.statusClean,
            textColor = colors.success,
        )
        TransactionStatus.AWAITING_MATCH -> StatusTagConfig(
            text = "Awaiting match",
            icon = Icons.Outlined.AutoAwesome,
            bgColor = colors.statusAutoResolved,
            textColor = colors.warning,
        )
        TransactionStatus.AWAITING_CATEGORY -> StatusTagConfig(
            text = "No category",
            icon = Icons.Outlined.ErrorOutline,
            bgColor = colors.statusAutoResolved,
            textColor = colors.warning,
        )
        TransactionStatus.AWAITING_AMOUNT -> StatusTagConfig(
            text = "No amount",
            icon = Icons.Outlined.ErrorOutline,
            bgColor = colors.statusNeedsReview,
            textColor = colors.error,
        )
        TransactionStatus.DISCARDED -> StatusTagConfig(
            text = "Discarded",
            icon = Icons.Outlined.ErrorOutline,
            bgColor = colors.statusNeedsReview,
            textColor = colors.error,
        )
    }
}
