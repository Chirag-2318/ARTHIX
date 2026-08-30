package com.chirag.arthix.ui.screen.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.chirag.arthix.ui.components.SecondaryButton
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.SectionHeader

/**
 * App Lock Setup screen — PIN creation.
 *
 * 4-digit PIN entry with dot indicators and numpad.
 * PIN stored via EncryptedSharedPreferences.
 */
@Composable
fun AppLockSetupScreen(
    onComplete: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentEntry = if (isConfirming) confirmPin else pin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isConfirming) "Confirm PIN" else "Set App Lock",
            style = HeadlineLg,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = if (isConfirming) "Re-enter your 4-digit PIN" else "Choose a 4-digit PIN",
            style = BodySecondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xxl))

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

        error?.let { errorMsg ->
            Spacer(Modifier.height(spacing.md))
            Text(
                text = errorMsg,
                style = BodySecondary,
                color = colors.error,
            )
        }

        Spacer(Modifier.height(spacing.xxl))

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
                                    } else if (currentEntry.length < 4) {
                                        if (isConfirming) {
                                            confirmPin += key
                                            if (confirmPin.length == 4) {
                                                if (confirmPin == pin) {
                                                    onComplete()
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

        Spacer(Modifier.height(spacing.sectionGap))

        SecondaryButton(
            text = "Skip for now",
            onClick = onComplete,
        )
    }
}
