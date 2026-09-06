package com.chirag.arthix.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArthixBottomNavBar(
    modifier: Modifier = Modifier,
    selectedDestination: ArthixDestination,
    onDestinationSelected: (ArthixDestination) -> Unit,
    onVoiceClick: () -> Unit,
    onPlusOptionSelected: (PlusOption) -> Unit,
    onPlusExpandedChange: (Boolean) -> Unit = {}
) {
    var isPlusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isPlusExpanded) {
        onPlusExpandedChange(isPlusExpanded)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Respects system gesture safe areas
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp), // Lowered slightly per user request
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // Expanded State: Row of options (Animated vertically)
        // =====================================================================
        AnimatedVisibility(
            visible = isPlusExpanded,
            enter = fadeIn(tween(250)) + expandVertically(
                spring(dampingRatio = 0.8f, stiffness = 200f),
                expandFrom = Alignment.Bottom
            ),
            exit = fadeOut(tween(200)) + shrinkVertically(
                spring(dampingRatio = 0.8f, stiffness = 200f),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            // Visually contained within a single cohesive pill/card surface
            Row(
                modifier = Modifier
                    .shadow(18.dp, RoundedCornerShape(32.dp))
                    .background(ArthixNavColors.CapsuleBackground, RoundedCornerShape(32.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Tighter padding
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Much tighter gap between Camera, Goal, etc.
                verticalAlignment = Alignment.CenterVertically
            ) {
                val options = listOf(PlusOption.ACCOUNT, PlusOption.STREAKS, PlusOption.GOALS, PlusOption.CAMERA)
                options.forEach { option ->
                    PlusOptionItem(
                        option = option,
                        onClick = {
                            isPlusExpanded = false
                            onPlusOptionSelected(option)
                        }
                    )
                }
            }
        }

        // =====================================================================
        // Main Nav Bar: Collapsed State (Pill + Mic Button)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Centers both the pill and the mic button as a group
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The Capsule Pill (Home, Activity, Insights, Plus)
            Row(
                modifier = Modifier
                    .shadow(18.dp, RoundedCornerShape(50))
                    .background(ArthixNavColors.CapsuleBackground, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                // Consistent spacing inside the pill
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    destination = ArthixDestination.HOME,
                    selected = selectedDestination == ArthixDestination.HOME,
                    onClick = { onDestinationSelected(ArthixDestination.HOME) }
                )
                NavTabItem(
                    destination = ArthixDestination.ACTIVITY,
                    selected = selectedDestination == ArthixDestination.ACTIVITY,
                    onClick = { onDestinationSelected(ArthixDestination.ACTIVITY) }
                )
                NavTabItem(
                    destination = ArthixDestination.INSIGHTS,
                    selected = selectedDestination == ArthixDestination.INSIGHTS,
                    onClick = { onDestinationSelected(ArthixDestination.INSIGHTS) }
                )
                
                // Plus Button with rotational animation on toggle
                val plusRotation by animateFloatAsState(
                    targetValue = if (isPlusExpanded) 45f else 0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                    label = "plusRotation"
                )
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ArthixNavColors.CapsuleBackgroundElevated)
                        .clickable { isPlusExpanded = !isPlusExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PlusIcon,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = plusRotation }
                    )
                }
            }

            // Fixed, consistent gap between the pill and the Mic button
            Spacer(modifier = Modifier.width(16.dp))

            // Voice Button (Mic) - Placed outside the pill to prevent expanding the pill too much
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(18.dp, CircleShape)
                    .clip(CircleShape)
                    .background(ArthixNavColors.CapsuleBackground)
                    .clickable { onVoiceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice Log",
                    tint = ArthixNavColors.IconInactive,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PlusOptionItem(
    option: PlusOption,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp), // Slightly smaller icons in the expanded row for a tighter look
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = ArthixNavColors.IconInactive,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = option.label,
            color = ArthixNavColors.IconInactive,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

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
