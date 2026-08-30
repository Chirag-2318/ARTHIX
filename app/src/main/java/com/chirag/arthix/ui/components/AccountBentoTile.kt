package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Bento-style tile from Account Home in Stitch design.
 *
 * Layout: icon-in-circle at top, title + subtitle at bottom.
 * 1:1 aspect ratio, surface-elevated bg, border-hairline border,
 * rounded-lg (8dp) corners, card-padding (16dp) internal.
 *
 * Used for "Protect account", "App Lock", "Account settings" etc.
 */
@Composable
fun AccountBentoTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val shapes = ArthixTheme.shapes

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shapes.listItem)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, shapes.listItem)
            .clickable(onClick = onClick)
            .padding(spacing.cardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Icon in circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceIconChip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            // Title
            Text(
                text = title,
                style = BodyPrimary,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            // Subtitle
            Text(
                text = subtitle,
                style = BodySecondary,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
