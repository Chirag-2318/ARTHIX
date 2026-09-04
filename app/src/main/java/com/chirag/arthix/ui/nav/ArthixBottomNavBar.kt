package com.chirag.arthix.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

import androidx.compose.ui.BiasAlignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic

/**
 * The complete Arthix bottom nav bar: Home / Activity / Voice / Insights / Plus.
 *
 * Layout architecture (three independent layers, back-to-front):
 *
 *   Layer 1  — The dark capsule pill with Home / Activity / gap / Insights / Plus-trigger
 *   Layer 2  — The Voice record button, centered, elevated above the capsule
 *   Layer 3  — The Plus radial arc options, anchored bottom-end, fanning upward
 *
 * Each layer is a separate child of the root Box, positioned with .align().
 * Nothing is nested inside the capsule's Row except the tab items and a simple
 * Plus trigger icon. The PlusRadialMenu is a sibling overlay so its arc pills
 * can draw freely above the capsule without any clip or z-index collision.
 *
 * IMPORTANT — this composable has NO background of its own beyond the capsule pill.
 * Do not wrap it in a Surface, Scaffold bottomBar slot, or anything that paints a
 * full-width rectangle.
 */
@Composable
fun ArthixBottomNavBar(
    modifier: Modifier = Modifier,
    selectedDestination: ArthixDestination,
    onDestinationSelected: (ArthixDestination) -> Unit,
    onVoiceClick: () -> Unit,
    onPlusOptionSelected: (PlusOption) -> Unit,
    onPlusExpandedChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left space (25% of empty space)
        Spacer(modifier = Modifier.weight(1f))

        // =====================================================================
        // Layer 1: The capsule background + tab Row
        // =====================================================================
        Box(
            modifier = Modifier.zIndex(1f)
        ) {
            // Background pill
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(50),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(ArthixNavColors.CapsuleBackground)
            )

            // Tab content
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    destination = ArthixDestination.HOME,
                    selected = selectedDestination == ArthixDestination.HOME,
                    onClick = { onDestinationSelected(ArthixDestination.HOME) }
                )
                Spacer(Modifier.width(6.dp))
                NavTabItem(
                    destination = ArthixDestination.ACTIVITY,
                    selected = selectedDestination == ArthixDestination.ACTIVITY,
                    onClick = { onDestinationSelected(ArthixDestination.ACTIVITY) }
                )

                // Standard gap between tabs
                Spacer(Modifier.width(6.dp))

                NavTabItem(
                    destination = ArthixDestination.INSIGHTS,
                    selected = selectedDestination == ArthixDestination.INSIGHTS,
                    onClick = { onDestinationSelected(ArthixDestination.INSIGHTS) }
                )
                Spacer(Modifier.width(6.dp))

                // Plus trigger: just the button circle; the arc menu is Layer 3
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    PlusRadialMenu(
                        onOptionSelected = onPlusOptionSelected,
                        onExpandedChange = onPlusExpandedChange
                    )
                }
            }
        }

        // Space between pill and voice button
        Spacer(modifier = Modifier.weight(1.5f))

        // =====================================================================
        // Voice Button
        // =====================================================================
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(50),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(50))
                .background(ArthixNavColors.CapsuleBackground)
                .clickable(onClick = onVoiceClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Voice Log",
                tint = ArthixNavColors.IconInactive,
                modifier = Modifier.size(24.dp)
            )
        }

        // Space on right of voice button
        Spacer(modifier = Modifier.weight(1.5f))
    }
}

/**
 * One Home/Activity/Insights tab. The active tab expands into a filled coral pill
 * with icon + label; inactive tabs are icon-only. `animateContentSize` gives the
 * whole row the "morphing slide" feel as tabs expand/contract, with no manual
 * position math required.
 */
@Composable
private fun NavTabItem(
    destination: ArthixDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "tabPadding"
    )

    Row(
        modifier = Modifier
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 200f))
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) Modifier.background(ArthixNavColors.Coral)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = if (selected) ArthixNavColors.IconActive else ArthixNavColors.IconInactive,
            modifier = Modifier.size(22.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(250)) + expandHorizontally(tween(250)),
            exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = destination.label,
                    color = ArthixNavColors.LabelActive,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}
