package com.chirag.arthix.ui.screen.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.DisplayHero
import kotlinx.coroutines.delay

/**
 * Splash Screen — matches Stitch design exactly.
 *
 * Black background, centered "Shake & Audit" in display-hero Hanken Grotesk,
 * tagline "Log it before you forget it" in body-primary,
 * bottom 2px animated loading bar with primary (#FFFFFF) fill.
 * Auto-navigates after 2s.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    var animationStarted by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 2000),
        label = "splash_progress",
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2200L)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Center content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.marginX),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Shake & Audit",
                style = DisplayHero,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.md))

            Text(
                text = "Log it before you forget it",
                style = BodyPrimary,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Bottom loading bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = spacing.marginX)
                .padding(bottom = 48.dp),
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surfaceElevated),
            )
            // Fill
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = colors.textPrimary,
                trackColor = colors.surfaceElevated,
            )
        }
    }
}
