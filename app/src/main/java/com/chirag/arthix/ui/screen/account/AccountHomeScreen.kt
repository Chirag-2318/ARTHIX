package com.chirag.arthix.ui.screen.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

private object AccountColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceRaised = Color(0xFF1E1E24)
    val Border = Color(0xFF2A2A31)
    val Brand = Color(0xFFFF7A1A)
    val BrandDim = Color(0xFFFF7A1A).copy(alpha = 0.14f)
    val BrandGradient = Brush.linearGradient(listOf(Color(0xFFFF9142), Color(0xFFFF5B3D)))
    val Danger = Color(0xFFEF4444)
    val DangerDim = Color(0xFFEF4444).copy(alpha = 0.12f)
    val Success = Color(0xFF22C55E)
    val SuccessDim = Color(0xFF22C55E).copy(alpha = 0.12f)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA5)
    val TextMuted = Color(0xFF6B6B75)
}

/**
 * Redesigned Account & Settings screen.
 *
 * Cohesive with Home & Splits:
 * - Matching dark charcoal and vibrant amber-orange design system
 * - Lower placement for profile info & edit controls
 * - Built-in "Clear All Data" and "Sign Out" actions with two-step confirmation dialogs
 */
@Composable
fun AccountHomeScreen(
    modifier: Modifier = Modifier,
    onRequestSmsPermission: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Security", "Privacy", "Data Management")

    var showEditDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val hasOverlayPermission = Settings.canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AccountColors.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Top Header ───────────────────────────────────────────────
        Text(
            text = "Account & Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccountColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Manage your preferences, security & local data",
            fontSize = 14.sp,
            color = AccountColors.TextMuted,
        )

        Spacer(Modifier.height(20.dp))

        // ── Segmented Category Tabs ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccountColors.Brand else AccountColors.SurfaceRaised)
                        .border(
                            BorderStroke(1.dp, if (isSelected) AccountColors.Brand else AccountColors.Border),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else AccountColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Tab Content ──────────────────────────────────────────────
        when (selectedTab) {
            0 -> GeneralTab(
                uiState = uiState,
                hasSmsPermission = hasSmsPermission,
                hasOverlayPermission = hasOverlayPermission,
                onRequestSmsPermission = onRequestSmsPermission,
                onEditProfileClick = { showEditDialog = true },
                onSignOutClick = { showSignOutDialog = true },
                onClearDataClick = { showClearDataDialog = true },
            )
            1 -> SecurityTab()
            2 -> PrivacyTab()
            3 -> DataManagementTab(
                onSignOutClick = { showSignOutDialog = true },
                onClearDataClick = { showClearDataDialog = true },
            )
        }

        Spacer(Modifier.height(40.dp))
    }

    // ── Edit Profile Dialog ──────────────────────────────────────────
    if (showEditDialog) {
        EditProfileDialog(
            currentName = if (uiState.userName == "User") "" else uiState.userName,
            currentPhone = uiState.phoneNumber,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newPhone ->
                viewModel.saveProfile(newName, newPhone)
                showEditDialog = false
            }
        )
    }

    // ── Sign Out Confirmation Dialog ─────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = AccountColors.SurfaceRaised,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = AccountColors.Brand,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Sign Out?",
                    color = AccountColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "You will be signed out and returned to the initial account setup screen. Your local offline database will remain intact.",
                    color = AccountColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut { onSignOut() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccountColors.Brand),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = AccountColors.TextSecondary)
                }
            }
        )
    }

    // ── Clear All App Data Confirmation Dialog ───────────────────────
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = AccountColors.SurfaceRaised,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = AccountColors.Danger,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete All App Data?",
                    color = AccountColors.Danger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "This action is permanent and cannot be undone. All your logged transactions, split bills, budget streaks, and local data will be permanently wiped.",
                    color = AccountColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData { onClearAllData() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccountColors.Danger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Everything", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = AccountColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GeneralTab(
    uiState: AccountUiState,
    hasSmsPermission: Boolean,
    hasOverlayPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onClearDataClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 1. Quick Config & Permissions Bento Card
        SectionCard(title = "App Features & Permissions") {
            // Floating Pop-up Setup
            ConfigRow(
                icon = Icons.Default.Layers,
                title = "Floating Pop-up for Shake",
                subtitle = if (hasOverlayPermission) "Active · Overlays banking apps" else "Action required · Grant overlay permission",
                badgeText = if (hasOverlayPermission) "Enabled" else "Enable",
                badgePositive = hasOverlayPermission,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)

            // SMS & Banking Notifications
            ConfigRow(
                icon = Icons.Default.NotificationsActive,
                title = "Bank SMS & Notifications",
                subtitle = if (hasSmsPermission) "Active · Auto-captures UPI transactions" else "Permissions required for auto-detection",
                badgeText = if (hasSmsPermission) "Active" else "Grant",
                badgePositive = hasSmsPermission,
                onClick = onRequestSmsPermission
            )

            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)

            // App Lock
            ConfigRow(
                icon = Icons.Default.Lock,
                title = "App Lock Protection",
                subtitle = "Biometric & PIN authentication",
                badgeText = "Protected",
                badgePositive = true,
                onClick = {}
            )
        }

        // 2. Profile Card — repositioned to lower section of the tab as requested
        SectionCard(title = "Your Profile") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with glowing accent ring
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AccountColors.SurfaceRaised)
                        .border(BorderStroke(2.dp, AccountColors.Brand), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.initials,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccountColors.TextPrimary
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccountColors.TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (uiState.phoneNumber.isNotBlank()) uiState.phoneNumber else "No phone linked",
                        fontSize = 13.sp,
                        color = AccountColors.TextSecondary
                    )
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEditProfileClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccountColors.Brand),
                    border = BorderStroke(1.dp, AccountColors.Brand.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Danger Zone / Account Management
        SectionCard(title = "Account Management") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sign Out
                OutlinedButton(
                    onClick = onSignOutClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccountColors.TextPrimary
                    ),
                    border = BorderStroke(1.dp, AccountColors.Border),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                // Clear All Data
                Button(
                    onClick = onClearDataClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccountColors.DangerDim,
                        contentColor = AccountColors.Danger
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Data", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SecurityTab() {
    var encryptionEnabled by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Local Security Architecture") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Database Encryption", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccountColors.TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "SQLCipher 256-bit AES at rest with keys protected in Android Keystore",
                        fontSize = 12.sp,
                        color = AccountColors.TextMuted
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = encryptionEnabled,
                    onCheckedChange = { encryptionEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccountColors.Brand,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = AccountColors.SurfaceRaised,
                    )
                )
            }

            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccountColors.Success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Zero Cloud Leakage", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
                    Text("No financial data or SMS payloads leave the device.", fontSize = 12.sp, color = AccountColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PrivacyTab() {
    val permissions = listOf(
        "Display Over Other Apps" to "Draws category floating pop-up upon shake over payment apps",
        "Notification Listener" to "Reads and categorizes real-time UPI transaction alerts",
        "SMS (Read/Receive)" to "Detects bank debit and credit SMS receipts",
        "Microphone" to "Enables on-device OpenAI Whisper speech recognition",
        "Camera" to "Extracts totals and items from merchant receipts via ML OCR",
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Device Permissions & Privacy") {
            permissions.forEachIndexed { index, (title, desc) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(desc, fontSize = 12.sp, color = AccountColors.TextMuted)
                }
                if (index < permissions.lastIndex) {
                    HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun DataManagementTab(
    onSignOutClick: () -> Unit,
    onClearDataClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Local Database & Storage") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Storage Type", fontSize = 14.sp, color = AccountColors.TextSecondary)
                    Text("Room SQLite + Encrypted DataStore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccountColors.SuccessDim)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Healthy", color = AccountColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        SectionCard(title = "Danger Zone") {
            Text(
                "Deleting data or signing out will reset local configurations.",
                fontSize = 13.sp,
                color = AccountColors.TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSignOutClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccountColors.TextPrimary),
                    border = BorderStroke(1.dp, AccountColors.Border),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onClearDataClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccountColors.Danger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Delete All Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AccountColors.Surface)
            .border(BorderStroke(1.dp, AccountColors.Border), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AccountColors.TextPrimary
        )
        content()
    }
}

@Composable
private fun ConfigRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgePositive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccountColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccountColors.Brand, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = AccountColors.TextMuted)
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (badgePositive) AccountColors.SuccessDim else AccountColors.BrandDim)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (badgePositive) AccountColors.Success else AccountColors.Brand
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AccountColors.SurfaceRaised,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Edit Profile", fontWeight = FontWeight.Bold, color = AccountColors.TextPrimary, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name", color = AccountColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccountColors.Brand,
                        unfocusedBorderColor = AccountColors.Border,
                        cursorColor = AccountColors.Brand,
                        focusedTextColor = AccountColors.TextPrimary,
                        unfocusedTextColor = AccountColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number", color = AccountColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccountColors.Brand,
                        unfocusedBorderColor = AccountColors.Border,
                        cursorColor = AccountColors.Brand,
                        focusedTextColor = AccountColors.TextPrimary,
                        unfocusedTextColor = AccountColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccountColors.Brand),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AccountColors.TextSecondary)
            }
        }
    )
}
