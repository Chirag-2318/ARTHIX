package com.chirag.arthix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.enableEdgeToEdge
import com.chirag.arthix.sensor.ShakeDetectionService
import com.chirag.arthix.ui.ArthixApp
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startShakeService()
        }
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        android.util.Log.d("Onboarding", "SMS permissions granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permStatus = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startShakeService()
            }
        } else {
            startShakeService()
        }

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

            // Don't render until we know if the account is created
            if (isAccountCreated == null) return@setContent

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

    private fun startShakeService() {
        val intent = Intent(this, ShakeDetectionService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}