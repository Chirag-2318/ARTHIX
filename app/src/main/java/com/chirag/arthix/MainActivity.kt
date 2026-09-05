package com.chirag.arthix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.chirag.arthix.sensor.ShakeDetectionService
import com.chirag.arthix.ui.ArthixApp
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chirag.arthix.ui.screen.applock.AppLockVerifyScreen

/**
 * Main entry point for the Arthix app.
 *
 * Converted from plain Activity to ComponentActivity for Compose support,
 * annotated with @AndroidEntryPoint for Hilt injection.
 *
 * Permission flow uses the Activity Result API (replacing the deprecated
 * requestPermissions / onRequestPermissionsResult pattern).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var reconciliationEngine: com.chirag.arthix.notification.ReconciliationEngine

    @javax.inject.Inject
    lateinit var accountPreferences: com.chirag.arthix.data.preferences.AccountPreferences

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        android.util.Log.d("Onboarding", "SMS permissions granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        startShakeService()

        val sharedPrefs = getSharedPreferences("arthix_prefs", android.content.Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)

        var initialDeepLinkTxnId: Long? = null

        if (intent.action == "com.chirag.arthix.CATEGORIZE_SMS") {
            val notificationId = intent.getStringExtra("notification_id")
            if (notificationId != null) {
                // Run synchronously to ensure we have the ID before Compose starts
                // Alternatively, we can use a mutableState and update it async, but since this
                // is onCreate, a launch block could set it right after content is composed.
                // It's safer to use a State in Compose, but for simplicity, we pass it down
                // and Compose can update when ready. Let's handle it async.
            }
        }

        setContent {
            val deepLinkTxnId = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
            val isAccountCreated by accountPreferences.isAccountCreated.collectAsState(initial = null)
            val appLockEnabled by accountPreferences.appLockEnabled.collectAsState(initial = null)
            val appLockType by accountPreferences.appLockType.collectAsState(initial = null)
            val appLockHash by accountPreferences.appLockHash.collectAsState(initial = null)
            
            var isAppUnlocked by remember { mutableStateOf(false) }

            androidx.compose.runtime.LaunchedEffect(intent) {
                if (intent.action == "com.chirag.arthix.CATEGORIZE_SMS") {
                    val notificationId = intent.getStringExtra("notification_id")
                    if (notificationId != null) {
                        val txnId = reconciliationEngine.forceTimeoutNotification(notificationId)
                        if (txnId != null) {
                            deepLinkTxnId.value = txnId
                            // Clear action so it doesn't refire on rotation
                            intent.action = ""
                        }
                    }
                }
            }

            // Don't render until we know if the account is created and app lock states are loaded
            if (isAccountCreated == null || appLockEnabled == null) return@setContent

            if (appLockEnabled == true) {
                // Wait for the lock states if app lock is enabled
                if (appLockType == null || appLockHash == null) return@setContent
                
                if (!isAppUnlocked) {
                    AppLockVerifyScreen(
                        lockType = appLockType ?: "PIN",
                        lockHash = appLockHash ?: "",
                        onUnlocked = { isAppUnlocked = true }
                    )
                } else {
                    ArthixApp(
                        isAccountCreated = isAccountCreated!!,
                        onboardingCompleted = onboardingCompleted,
                        deepLinkTxnId = deepLinkTxnId.value,
                        onRequestSmsPermission = {
                            smsPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                            )
                        }
                    )
                }
            } else {
                ArthixApp(
                    isAccountCreated = isAccountCreated!!,
                    onboardingCompleted = onboardingCompleted,
                    deepLinkTxnId = deepLinkTxnId.value,
                    onRequestSmsPermission = {
                        smsPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                        )
                    }
                )
            }
        }
    }

    private fun startShakeService() {
        val intent = Intent(this, ShakeDetectionService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}