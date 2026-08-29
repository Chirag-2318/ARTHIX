package com.chirag.arthix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.chirag.arthix.sensor.ShakeDetectionService
import com.chirag.arthix.ui.ArthixApp
import dagger.hilt.android.AndroidEntryPoint

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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startShakeService()
        }
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.d("Onboarding", "SMS permission granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContent {
            ArthixApp(
                onboardingCompleted = onboardingCompleted,
                onRequestSmsPermission = {
                    smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                }
            )
        }
    }

    private fun startShakeService() {
        val intent = Intent(this, ShakeDetectionService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}