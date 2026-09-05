package com.chirag.arthix.ui.screen.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.components.PatternLock
import com.chirag.arthix.ui.components.SecondaryButton
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.SectionHeader
import java.security.MessageDigest

fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * App Lock Setup screen — PIN or Pattern creation.
 */
@Composable
fun AppLockSetupScreen(
    onComplete: (type: String, hash: String) -> Unit,
    onSkip: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    var lockType by remember { mutableStateOf("PIN") } // "PIN" or "PATTERN"
    
    // PIN states
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    // Pattern states
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var confirmPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var patternAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Toggle if not confirming
        if (!isConfirming) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { lockType = "PIN" }) {
                    Text(
                        "PIN",
                        color = if (lockType == "PIN") colors.primary else colors.textSecondary,
                        style = SectionHeader
                    )
                }
                TextButton(onClick = { lockType = "PATTERN" }) {
                    Text(
                        "Pattern",
                        color = if (lockType == "PATTERN") colors.primary else colors.textSecondary,
                        style = SectionHeader
                    )
                }
            }
            Spacer(Modifier.height(spacing.xl))
        }

        Text(
            text = if (isConfirming) "Confirm $lockType" else "Set App Lock",
            style = HeadlineLg,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        
        val subtitle = if (lockType == "PIN") {
            if (isConfirming) "Re-enter your 4-digit PIN" else "Choose a 4-digit PIN"
        } else {
            if (isConfirming) "Draw your pattern again" else "Draw an unlock pattern"
        }
        
        Text(
            text = subtitle,
            style = BodySecondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xxl))

        if (lockType == "PIN") {
            val currentEntry = if (isConfirming) confirmPin else pin
            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < currentEntry.length) colors.textPrimary
                                else colors.surfaceContainerHighest,
                            ),
                    )
                }
            }
        }

        error?.let { errorMsg ->
            Spacer(Modifier.height(spacing.md))
            Text(
                text = errorMsg,
                style = BodySecondary,
                color = colors.error,
            )
        }

        Spacer(Modifier.height(if (lockType == "PIN") spacing.xxl else spacing.sm))

        if (lockType == "PIN") {
            // Numpad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Box(Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        error = null
                                        if (key == "⌫") {
                                            if (isConfirming) {
                                                confirmPin = confirmPin.dropLast(1)
                                            } else {
                                                pin = pin.dropLast(1)
                                            }
                                        } else if ((if (isConfirming) confirmPin else pin).length < 4) {
                                            if (isConfirming) {
                                                confirmPin += key
                                                if (confirmPin.length == 4) {
                                                    if (confirmPin == pin) {
                                                        onComplete("PIN", hashPin(pin))
                                                    } else {
                                                        error = "PINs don't match"
                                                        confirmPin = ""
                                                    }
                                                }
                                            } else {
                                                pin += key
                                                if (pin.length == 4) {
                                                    isConfirming = true
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = SectionHeader,
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.sm))
            }
        } else {
            // Pattern Lock
            androidx.compose.runtime.key(isConfirming, patternAttempt) {
                PatternLock(
                    onPatternComplete = { sequence ->
                        error = null
                        if (isConfirming) {
                            confirmPattern = sequence
                            if (confirmPattern == pattern) {
                                onComplete("PATTERN", hashPin(pattern.joinToString(",")))
                            } else {
                                error = "Patterns don't match"
                                patternAttempt++
                            }
                        } else {
                            if (sequence.size < 4) {
                                error = "Connect at least 4 dots"
                                patternAttempt++
                            } else {
                                pattern = sequence
                                isConfirming = true
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(spacing.sectionGap))

        SecondaryButton(
            text = "Skip for now",
            onClick = onSkip,
        )
    }
}

/**
 * App Lock Verify Screen — verifies PIN or Pattern to unlock the app.
 */
@Composable
fun AppLockVerifyScreen(
    lockType: String,
    lockHash: String,
    onUnlocked: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var patternAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (lockType == "PIN") "Enter PIN" else "Draw Pattern",
            style = HeadlineLg,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = "App is locked for your security",
            style = BodySecondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xxl))

        if (lockType == "PIN") {
            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < pin.length) colors.textPrimary
                                else colors.surfaceContainerHighest,
                            ),
                    )
                }
            }
        }

        error?.let { errorMsg ->
            Spacer(Modifier.height(spacing.md))
            Text(
                text = errorMsg,
                style = BodySecondary,
                color = colors.error,
            )
        }

        Spacer(Modifier.height(if (lockType == "PIN") spacing.xxl else spacing.sm))

        if (lockType == "PIN") {
            // Numpad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Box(Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        error = null
                                        if (key == "⌫") {
                                            pin = pin.dropLast(1)
                                        } else if (pin.length < 4) {
                                            pin += key
                                            if (pin.length == 4) {
                                                if (hashPin(pin) == lockHash) {
                                                    onUnlocked()
                                                } else {
                                                    error = "Incorrect PIN"
                                                    pin = ""
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = SectionHeader,
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.sm))
            }
        } else {
            // Pattern Lock
            androidx.compose.runtime.key(patternAttempt) {
                PatternLock(
                    onPatternComplete = { sequence ->
                        error = null
                        if (hashPin(sequence.joinToString(",")) == lockHash) {
                            onUnlocked()
                        } else {
                            error = "Incorrect Pattern"
                            patternAttempt++
                        }
                    }
                )
            }
        }
    }
}
