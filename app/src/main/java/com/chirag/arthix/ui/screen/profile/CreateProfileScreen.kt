package com.chirag.arthix.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.SecondaryButton
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.HeadlineLg
import com.chirag.arthix.ui.theme.SectionHeader

/**
 * Create Profile screen — post-onboarding.
 *
 * Simple name input with avatar preview (initials).
 * Saved to SharedPreferences.
 */
@Composable
fun CreateProfileScreen(
    onComplete: () -> Unit,
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Create Profile",
            style = HeadlineLg,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xxl))

        // Avatar preview
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(colors.secondaryContainer)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                style = DisplayHeroMobile,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(spacing.sectionGap))

        // Name input
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name", style = BodySecondary) },
            singleLine = true,
            textStyle = BodyPrimary.copy(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textPrimary,
                unfocusedBorderColor = colors.border,
                cursorColor = colors.accent,
                focusedLabelColor = colors.textPrimary,
                unfocusedLabelColor = colors.textSecondary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(spacing.xxl))

        PrimaryButton(
            text = "Continue",
            onClick = {
                val prefs = context.getSharedPreferences("arthix_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("user_name", name.trim()).apply()
                onComplete()
            },
            enabled = name.isNotBlank(),
        )

        Spacer(Modifier.height(spacing.md))

        SecondaryButton(
            text = "Skip",
            onClick = onComplete,
        )
    }
}
