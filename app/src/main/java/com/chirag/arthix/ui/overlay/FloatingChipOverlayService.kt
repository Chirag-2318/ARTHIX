package com.chirag.arthix.ui.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chirag.arthix.MainActivity
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.notification.DiscardSource
import com.chirag.arthix.notification.ReconciliationEngine
import com.chirag.arthix.ui.theme.ArthixTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Custom [LifecycleOwner] and [SavedStateRegistryOwner] to host Jetpack Compose
 * inside a standalone floating [WindowManager] overlay without an Activity.
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/**
 * Foreground / Standalone Overlay Service that renders [FloatingChipPopup]
 * directly onto the screen via [WindowManager].
 *
 * This provides 100% isolation from the Android notification tray, ensuring that
 * payment notifications from GPay, PhonePe, and Banks cannot dismiss or hide
 * the Arthix category prompt.
 */
@AndroidEntryPoint
class FloatingChipOverlayService : Service() {

    companion object {
        private const val TAG = "FloatingChipOverlay"

        const val ACTION_SHOW_OVERLAY = "com.chirag.arthix.overlay.SHOW"
        const val ACTION_HIDE_OVERLAY = "com.chirag.arthix.overlay.HIDE"

        const val EXTRA_CORRELATION_ID = "extra_correlation_id"
        const val EXTRA_CATEGORIES = "extra_categories"
        const val EXTRA_AUTO_DISMISS_MS = "extra_auto_dismiss_ms"

        fun show(
            context: Context,
            correlationId: String,
            categories: List<String>,
            autoDismissMs: Long = 7000L,
        ) {
            val intent = Intent(context, FloatingChipOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra(EXTRA_CORRELATION_ID, correlationId)
                putStringArrayListExtra(EXTRA_CATEGORIES, ArrayList(categories))
                putExtra(EXTRA_AUTO_DISMISS_MS, autoDismissMs)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, FloatingChipOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var database: ArthixDatabase

    @Inject
    lateinit var reconciliationEngine: ReconciliationEngine

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val correlationId = intent.getStringExtra(EXTRA_CORRELATION_ID) ?: return START_NOT_STICKY
                val categories = intent.getStringArrayListExtra(EXTRA_CATEGORIES) ?: arrayListOf("Food", "Travel", "Shopping", "Other")
                val autoDismissMs = intent.getLongExtra(EXTRA_AUTO_DISMISS_MS, 7000L)

                showFloatingPopup(correlationId, categories, autoDismissMs)
            }
            ACTION_HIDE_OVERLAY -> {
                hideFloatingPopup()
            }
        }
        return START_NOT_STICKY
    }

    private fun showFloatingPopup(
        correlationId: String,
        categories: List<String>,
        autoDismissMs: Long,
    ) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlay: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        // Clean up any existing overlay view first
        hideFloatingPopup()

        try {
            val lifecycleOwner = OverlayLifecycleOwner()
            overlayLifecycleOwner = lifecycleOwner
            lifecycleOwner.onCreate()

            val density = resources.displayMetrics.density
            val topMarginPx = (48 * density).toInt()

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = topMarginPx
            }

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

                setContent {
                    ArthixTheme {
                        FloatingChipPopup(
                            correlationId = correlationId,
                            categories = categories,
                            durationMs = autoDismissMs,
                            onCategorySelected = { category ->
                                handleCategorySelection(correlationId, category)
                            },
                            onDiscard = {
                                handleDiscard(correlationId)
                            },
                            onOpenApp = {
                                openMainApp()
                            },
                            onDismiss = {
                                hideFloatingPopup()
                            }
                        )
                    }
                }
            }

            overlayView = composeView
            windowManager?.addView(composeView, layoutParams)
            Log.d(TAG, "Floating pop-up shown successfully for capture: $correlationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display floating pop-up overlay", e)
        }
    }

    private fun handleCategorySelection(correlationId: String, category: String) {
        Log.d(TAG, "Overlay category selected: $category for $correlationId")
        vibrateHaptic()

        serviceScope.launch {
            try {
                val capture = database.pendingQueueDao().findCaptureById(correlationId)
                if (capture != null) {
                    database.pendingQueueDao().updateCapture(capture.copy(category = category))

                    val txn = database.transactionDao().findBySourceCaptureId(correlationId)
                    if (txn != null) {
                        val newStatus = if (txn.amountPaise != null) {
                            TransactionStatus.CONFIRMED
                        } else {
                            txn.status
                        }
                        database.transactionDao().update(
                            txn.copy(category = category, status = newStatus)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving category from overlay", e)
            }
        }

        hideFloatingPopup()
    }

    private fun handleDiscard(correlationId: String) {
        Log.d(TAG, "Overlay discard requested for $correlationId")
        vibrateHaptic()

        reconciliationEngine.discardCapture(correlationId, DiscardSource.CHIP_TAP)
        hideFloatingPopup()
    }

    private fun openMainApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        hideFloatingPopup()
    }

    private fun hideFloatingPopup() {
        try {
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager?.removeView(view)
                }
            }
            overlayLifecycleOwner?.onDestroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error hiding floating overlay", e)
        } finally {
            overlayView = null
            overlayLifecycleOwner = null
        }
    }

    private fun vibrateHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform haptic feedback", e)
        }
    }

    override fun onDestroy() {
        hideFloatingPopup()
        serviceScope.cancel()
        super.onDestroy()
    }
}
