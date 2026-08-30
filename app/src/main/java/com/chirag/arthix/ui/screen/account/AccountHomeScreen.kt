package com.chirag.arthix.ui.screen.account

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShieldMoon
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.components.AccountBentoTile
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.components.SecurityStatusCard
import com.chirag.arthix.ui.components.SecurityStatusRow
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.BodyPrimary
import com.chirag.arthix.ui.theme.BodySecondary
import com.chirag.arthix.ui.theme.DisplayHeroMobile
import com.chirag.arthix.ui.theme.LabelCaps
import com.chirag.arthix.ui.theme.SectionHeader

/**
 * Account Home screen — matches Stitch design exactly.
 *
 * 4 sub-tabs: Home, Personal info, Security, Privacy & Data.
 * Profile header with avatar circle + name in display-hero-mobile.
 * Bento tiles for quick actions.
 * Security status card with rows.
 */
@Composable
fun AccountHomeScreen(
    modifier: Modifier = Modifier,
    onRequestSmsPermission: () -> Unit = {},
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val shapes = ArthixTheme.shapes

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Personal info", "Security", "Privacy & Data")

    // Read profile from prefs (placeholder for now)
    val userName = "Rohan Mehta"
    val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Profile header ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.marginX, vertical = spacing.sectionGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.cardPadding),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = SectionHeader,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = userName,
                style = DisplayHeroMobile,
                color = colors.textPrimary,
            )
        }

        // ── Segmented tabs ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.marginX),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                tabs.forEachIndexed { index, tab ->
                    Column(
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .padding(end = spacing.xl),
                    ) {
                        Text(
                            text = tab,
                            style = if (index == selectedTab) SectionHeader else BodyPrimary,
                            color = if (index == selectedTab) colors.textPrimary else colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = spacing.cardPadding),
                        )
                        // Indicator
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .fillMaxWidth()
                                .background(
                                    if (index == selectedTab) colors.textPrimary
                                    else colors.bg
                                ),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colors.border, thickness = 1.dp)

        // ── Tab content ─────────────────────────────────────────────
        when (selectedTab) {
            0 -> AccountHomeTab(onRequestSmsPermission = onRequestSmsPermission)
            1 -> PersonalInfoTab(userName = userName)
            2 -> SecurityTab()
            3 -> PrivacyDataTab()
        }
    }
}

@Composable
private fun AccountHomeTab(onRequestSmsPermission: () -> Unit = {}) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val context = LocalContext.current
    
    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier.padding(spacing.marginX),
    ) {
        Spacer(Modifier.height(spacing.sectionGap))

        // ── Bento grid (2 cols) ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter),
        ) {
            AccountBentoTile(
                icon = Icons.Outlined.ShieldMoon,
                title = "Protect account",
                subtitle = "Review alerts",
                modifier = Modifier.weight(1f),
            )
            AccountBentoTile(
                icon = Icons.Outlined.Lock,
                title = "App Lock",
                subtitle = "PIN set",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(spacing.gutter))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.gutter),
        ) {
            AccountBentoTile(
                icon = Icons.Outlined.Settings,
                title = "Account settings",
                subtitle = "Manage preferences",
                modifier = Modifier.weight(1f),
            )
            // Spacer for 2-col grid with odd items
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(spacing.sectionGap))

        // ── Suggestions card ────────────────────────────────────────
        if (!hasSmsPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ArthixTheme.shapes.listItem)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, ArthixTheme.shapes.listItem)
                    .padding(spacing.cardPadding),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = colors.accentWarning,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete your setup",
                                style = SectionHeader,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(spacing.xs))
                            Text(
                                text = "Grant remaining permissions to fully utilize auditing features.",
                                style = BodySecondary,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(spacing.md))
                    PrimaryButton(
                        text = "Begin checkup",
                        onClick = onRequestSmsPermission,
                    )
                }
            }

            Spacer(Modifier.height(spacing.sectionGap))
        }

        // ── Security status ─────────────────────────────────────────
        Text(
            text = "Security Status",
            style = SectionHeader,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.md))
        SecurityStatusCard(
            rows = listOf(
                SecurityStatusRow(
                    icon = Icons.Filled.Lock,
                    label = "App Lock",
                    status = "PIN Enabled",
                    isPositive = true,
                ),
                SecurityStatusRow(
                    icon = Icons.Filled.NotificationsActive,
                    label = "Notification Listener",
                    status = "Active",
                    isPositive = true,
                ),
            ),
        )

        Spacer(Modifier.height(spacing.xxl))
    }
}

@Composable
private fun PersonalInfoTab(userName: String) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    Column(
        modifier = Modifier.padding(spacing.marginX),
    ) {
        Spacer(Modifier.height(spacing.sectionGap))
        InfoRow("Name", userName)
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        InfoRow("App version", "1.0.4")
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        InfoRow("Member since", "2026")
        Spacer(Modifier.height(spacing.xxl))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.cardPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = BodyPrimary, color = colors.onSurfaceVariant)
        Text(text = value, style = BodyPrimary, color = colors.textPrimary)
    }
}

@Composable
private fun SecurityTab() {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    var encryptionEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(spacing.marginX),
    ) {
        Spacer(Modifier.height(spacing.sectionGap))

        Text(
            text = "Data Protection",
            style = SectionHeader,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.md))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ArthixTheme.shapes.listItem)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, ArthixTheme.shapes.listItem),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.cardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Database encryption", style = BodyPrimary, color = colors.textPrimary)
                    Text(
                        "Encrypt all local transaction data at rest",
                        style = BodySecondary,
                        color = colors.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = encryptionEnabled,
                    onCheckedChange = { encryptionEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.accent,
                        checkedThumbColor = colors.textPrimary,
                        uncheckedTrackColor = colors.surfaceContainerHighest,
                    ),
                )
            }
        }

        Spacer(Modifier.height(spacing.md))
        Text(
            text = "When enabled, your transaction database is encrypted using SQLCipher " +
                    "with a key stored in the Android Keystore.",
            style = BodySecondary,
            color = colors.textTertiary,
        )

        Spacer(Modifier.height(spacing.xxl))
    }
}

@Composable
private fun PrivacyDataTab() {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing

    Column(
        modifier = Modifier.padding(spacing.marginX),
    ) {
        Spacer(Modifier.height(spacing.sectionGap))

        Text(
            text = "Your data stays on your device",
            style = SectionHeader,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.md))
        Text(
            text = "Arthix processes everything locally. No transaction data, " +
                    "notifications, or SMS content ever leaves your phone. " +
                    "All ML processing (OCR, voice, categorization) runs on-device.",
            style = BodyPrimary,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(spacing.sectionGap))

        Text(
            text = "PERMISSIONS IN USE",
            style = LabelCaps,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.md))

        val permissions = listOf(
            "Notification Listener" to "Reads UPI payment notifications",
            "SMS" to "Reads bank transaction SMS",
            "Camera" to "Scans receipts for OCR",
            "Microphone" to "Voice-based transaction entry",
            "Battery" to "Keeps shake detection active",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ArthixTheme.shapes.listItem)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, ArthixTheme.shapes.listItem),
        ) {
            Column {
                permissions.forEachIndexed { index, (name, desc) ->
                    Column(
                        modifier = Modifier.padding(spacing.cardPadding),
                    ) {
                        Text(name, style = BodyPrimary, color = colors.textPrimary)
                        Text(desc, style = BodySecondary, color = colors.onSurfaceVariant)
                    }
                    if (index < permissions.lastIndex) {
                        HorizontalDivider(color = colors.border, thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing.xxl))
    }
}
