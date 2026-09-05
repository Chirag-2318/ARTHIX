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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private object AccountColors {
    val Background = Color(0xFFFAF7F2)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceRaised = Color(0xFFF0EBE1)
    val Border = Color(0xFFEAE3D9)
    val Brand = Color(0xFFE4463A)
    val BrandDim = Color(0xFFE4463A).copy(alpha = 0.14f)
    val BrandGradient = Brush.linearGradient(listOf(Color(0xFFE4463A), Color(0xFFFF6B5D)))
    val Danger = Color(0xFFEF4444)
    val DangerDim = Color(0xFFEF4444).copy(alpha = 0.12f)
    val Success = Color(0xFF22C55E)
    val SuccessDim = Color(0xFF22C55E).copy(alpha = 0.12f)
    val TextPrimary = Color(0xFF111111)
    val TextSecondary = Color(0xFF6B6B75)
    val TextMuted = Color(0xFF8F8F9B)

    // Pastel icon backgrounds
    val IconBgGeneral = Color(0xFFE5F0FF) // Soft sky-blue
    val IconGeneral = Color(0xFF0066FF)
    val IconBgSecurity = Color(0xFFFFEBEA) // Soft coral
    val IconSecurity = Color(0xFFE4463A)
    val IconBgPrivacy = Color(0xFFF3E8FF) // Soft lavender
    val IconPrivacy = Color(0xFF9333EA)
    val IconBgData = Color(0xFFE8F5E9) // Soft green
    val IconData = Color(0xFF16A34A)
}

enum class AccountSubScreen {
    Main,
    General,
    Security,
    Privacy,
    DataManagement
}

/**
 * Redesigned Account & Settings screen in light theme.
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

    var expandedSection by remember { mutableStateOf<AccountSubScreen?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    var showAppLockSetup by remember { mutableStateOf(false) }
    var showAppLockOptions by remember { mutableStateOf(false) }

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
        Text(
            text = "Account Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccountColors.TextPrimary
        )
        
        // Profile Header
        ProfileHeader(
            uiState = uiState,
            onEditProfileClick = { showEditDialog = true }
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Settings List
        SectionCard(title = "") {
            SettingsRow(
                title = "General",
                icon = Icons.Default.Layers,
                iconBg = AccountColors.IconBgGeneral,
                iconTint = AccountColors.IconGeneral,
                isExpanded = expandedSection == AccountSubScreen.General,
                onClick = { 
                    expandedSection = if (expandedSection == AccountSubScreen.General) null else AccountSubScreen.General 
                }
            )
            AnimatedVisibility(visible = expandedSection == AccountSubScreen.General) {
                GeneralTab(
                    uiState = uiState,
                    hasSmsPermission = hasSmsPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestSmsPermission = onRequestSmsPermission,
                    onAppLockClick = {
                        if (uiState.appLockEnabled) {
                            showAppLockOptions = true
                        } else {
                            showAppLockSetup = true
                        }
                    }
                )
            }
            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)
            
            SettingsRow(
                title = "Security",
                icon = Icons.Default.Shield,
                iconBg = AccountColors.IconBgSecurity,
                iconTint = AccountColors.IconSecurity,
                isExpanded = expandedSection == AccountSubScreen.Security,
                onClick = { 
                    expandedSection = if (expandedSection == AccountSubScreen.Security) null else AccountSubScreen.Security 
                }
            )
            AnimatedVisibility(visible = expandedSection == AccountSubScreen.Security) {
                SecurityTab()
            }
            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)
            
            SettingsRow(
                title = "Privacy",
                icon = Icons.Default.Lock,
                iconBg = AccountColors.IconBgPrivacy,
                iconTint = AccountColors.IconPrivacy,
                isExpanded = expandedSection == AccountSubScreen.Privacy,
                onClick = { 
                    expandedSection = if (expandedSection == AccountSubScreen.Privacy) null else AccountSubScreen.Privacy 
                }
            )
            AnimatedVisibility(visible = expandedSection == AccountSubScreen.Privacy) {
                PrivacyTab()
            }
            HorizontalDivider(color = AccountColors.Border, thickness = 1.dp)
            
            SettingsRow(
                title = "Data Management",
                icon = Icons.Default.DeleteForever,
                iconBg = AccountColors.IconBgData,
                iconTint = AccountColors.IconData,
                isExpanded = expandedSection == AccountSubScreen.DataManagement,
                onClick = { 
                    expandedSection = if (expandedSection == AccountSubScreen.DataManagement) null else AccountSubScreen.DataManagement 
                }
            )
            AnimatedVisibility(visible = expandedSection == AccountSubScreen.DataManagement) {
                DataManagementTab(
                    onSignOutClick = { showSignOutDialog = true },
                    onClearDataClick = { showClearDataDialog = true },
                )
            }
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
            containerColor = AccountColors.Surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccountColors.BrandDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = AccountColors.Brand,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccountColors.Border)
                ) {
                    Text("Cancel", color = AccountColors.TextSecondary)
                }
            }
        )
    }

    // ── Clear All App Data Confirmation Dialog ───────────────────────
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = AccountColors.Surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccountColors.DangerDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = AccountColors.Danger,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                OutlinedButton(
                    onClick = { showClearDataDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccountColors.Border)
                ) {
                    Text("Cancel", color = AccountColors.TextSecondary)
                }
            }
        )
    }

    if (showAppLockSetup) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAppLockSetup = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.chirag.arthix.ui.screen.applock.AppLockSetupScreen(
                onComplete = { type, hash ->
                    viewModel.setAppLock(type, hash)
                    showAppLockSetup = false
                    showAppLockOptions = true
                },
                onSkip = { showAppLockSetup = false }
            )
        }
    }
    
    if (showAppLockOptions) {
        AlertDialog(
            onDismissRequest = { showAppLockOptions = false },
            containerColor = AccountColors.Surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("App Lock Options", color = AccountColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Your app is protected with a ${uiState.appLockType ?: "PIN"} lock.", 
                        color = AccountColors.TextSecondary, 
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            viewModel.setAppLockEnabled(false)
                            showAppLockOptions = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccountColors.DangerDim),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable App Lock", color = AccountColors.Danger, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppLockOptions = false }) {
                    Text("Done", color = AccountColors.Brand)
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
    onAppLockClick: () -> Unit,
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
                badgeText = if (uiState.appLockEnabled) "Protected" else "Set Up",
                badgePositive = uiState.appLockEnabled,
                onClick = onAppLockClick
            )
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
    val context = LocalContext.current
    val hasOverlay = Settings.canDrawOverlays(context)

    val permissions = listOf(
        Triple(
            "Display Over Other Apps",
            "Draws category floating pop-up upon shake over payment apps",
            hasOverlay
        ),
        Triple(
            "Notification Listener",
            "Reads and categorizes real-time UPI transaction alerts",
            true
        ),
        Triple(
            "SMS (Read/Receive)",
            "Detects bank debit and credit SMS receipts",
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        ),
        Triple(
            "Microphone",
            "Enables on-device OpenAI Whisper speech recognition",
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ),
        Triple(
            "Camera",
            "Extracts totals and items from merchant receipts via ML OCR",
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Device Permissions & Privacy") {
            permissions.forEachIndexed { index, (title, desc, isGranted) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (title == "Display Over Other Apps") {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    context.startActivity(intent)
                                }
                            } else if (title == "Notification Listener") {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text(desc, fontSize = 12.sp, color = AccountColors.TextMuted)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGranted) AccountColors.SuccessDim else AccountColors.BrandDim)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isGranted) "Granted" else "Enable",
                            color = if (isGranted) AccountColors.Success else AccountColors.Brand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        // Storage status card
        SectionCard(title = "Local Database & Storage") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Storage Type", fontSize = 12.sp, color = AccountColors.TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("Room SQLite + Encrypted DataStore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccountColors.TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccountColors.SuccessDim)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("Healthy", color = AccountColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Danger Zone — coral-tinted card, visually separated
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(AccountColors.Danger.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, AccountColors.Danger.copy(alpha = 0.20f)), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "DANGER ZONE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountColors.Danger.copy(alpha = 0.7f),
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "These actions reset or remove local configurations and data. They cannot be undone.",
                fontSize = 13.sp,
                color = AccountColors.TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            // Sign Out row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSignOutClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccountColors.Brand.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AccountColors.Brand, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Sign Out",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AccountColors.Brand,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountColors.Brand.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }

            HorizontalDivider(color = AccountColors.Danger.copy(alpha = 0.15f), thickness = 1.dp)

            // Delete All Data row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClearDataClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccountColors.DangerDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AccountColors.Danger, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Delete All Data",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AccountColors.Danger,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccountColors.Danger.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = AccountColors.Surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccountColors.TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (badgePositive) AccountColors.IconBgData else AccountColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (badgePositive) AccountColors.IconData else AccountColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
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
                .clip(RoundedCornerShape(20.dp))
                .background(if (badgePositive) AccountColors.SuccessDim else AccountColors.BrandDim)
                .padding(horizontal = 10.dp, vertical = 5.dp)
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
private fun ProfileHeader(
    uiState: AccountUiState,
    onEditProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape)
                .background(AccountColors.SurfaceRaised)
                .border(BorderStroke(1.dp, AccountColors.Border), com.chirag.arthix.ui.theme.ArthixTheme.shapes.avatarShape),
            contentAlignment = Alignment.Center
        ) {
            if (!uiState.profileAvatar.isNullOrBlank()) {
                val context = LocalContext.current
                val resId = context.resources.getIdentifier(uiState.profileAvatar.removeSuffix(".png"), "drawable", context.packageName)
                if (resId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = resId),
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val imageModel = when {
                        uiState.profileAvatar.startsWith("content://") -> android.net.Uri.parse(uiState.profileAvatar)
                        uiState.profileAvatar.startsWith("/") -> java.io.File(uiState.profileAvatar)
                        else -> uiState.profileAvatar
                    }
                    coil.compose.AsyncImage(
                        model = imageModel,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            } else {
                Text(
                    text = uiState.initials,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccountColors.TextPrimary
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiState.userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AccountColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (uiState.phoneNumber.isNotBlank()) uiState.phoneNumber else "No phone linked",
                fontSize = 14.sp,
                color = AccountColors.TextSecondary
            )
        }

        // Edit Button
        Text(
            text = "Edit",
            color = AccountColors.Brand,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(onClick = onEditProfileClick)
                .padding(8.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = AccountColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = AccountColors.TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = AccountColors.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AccountColors.TextPrimary
        )
    }
    Spacer(Modifier.height(16.dp))
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
