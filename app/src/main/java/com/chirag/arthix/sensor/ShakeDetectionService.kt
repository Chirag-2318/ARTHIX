package com.chirag.arthix.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.notification.ReconciliationConfigSnapshot
import com.chirag.arthix.notification.ReconciliationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service hosting the shake detection sensor pipeline.
 *
 * PRD §7: persists for the app's "active" lifetime, keeps Doze from
 * suspending sensor delivery via a persistent low-priority notification.
 *
 * Responsibilities:
 * - Creates and manages [ShakeSensorManager] (sensor listener lifecycle)
 * - Registers a screen-state [BroadcastReceiver] for adaptive sampling (PRD §2.3)
 * - Posts the persistent foreground-service notification (IMPORTANCE_LOW)
 * - Fires [ChipTrigger] on each [ShakeEvent] with FR-1 categories
 * - Records service health markers for gap detection (PRD §7.4)
 * - Declared with START_STICKY for OS restart attempts (best-effort on OriginOS)
 *
 * Manifest declaration (PRD §7.1):
 * ```xml
 * <service
 *     android:name=".sensor.ShakeDetectionService"
 *     android:foregroundServiceType="specialUse"
 *     android:exported="false" />
 * ```
 */
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShakeDetectionService : Service() {

    companion object {
        private const val TAG = "ShakeDetectionService"

        /** Low-importance channel for the persistent foreground-service notification. */
        const val SERVICE_CHANNEL_ID = "shake_service_channel"

        /** Notification ID for the persistent service notification. */
        private const val SERVICE_NOTIFICATION_ID = 1001

        /**
         * Start the shake detection service.
         * Call after onboarding is complete (permissions granted, whitelist shown).
         */
        fun start(context: Context) {
            val intent = Intent(context, ShakeDetectionService::class.java)
            context.startForegroundService(intent)
        }

        /**
         * Stop the shake detection service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, ShakeDetectionService::class.java)
            context.stopService(intent)
        }
    }

    // ── Service internals ──────────────────────────────────────────────

    private lateinit var shakeSensorManager: ShakeSensorManager
    private lateinit var chipTrigger: ChipTrigger
    private lateinit var healthLog: ServiceHealthLog
    
    @Inject
    lateinit var reconciliationEngine: ReconciliationEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Screen-state receiver (PRD §2.3) ───────────────────────────────

    /**
     * Toggles sensor sampling rate on screen on/off transitions.
     * PRD §2.2: Screen ON → SENSOR_DELAY_GAME (~50Hz),
     *           Screen OFF → SENSOR_DELAY_NORMAL (~5Hz).
     */
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON — switching to SENSOR_DELAY_GAME")
                    shakeSensorManager.reregisterSensor(SensorManager.SENSOR_DELAY_GAME)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF — switching to SENSOR_DELAY_NORMAL")
                    shakeSensorManager.reregisterSensor(SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        }
    }

    // ── Service lifecycle ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // Health tracking (PRD §7.4)
        healthLog = ServiceHealthLog(this)
        val wasUncleanRestart = healthLog.recordStartup()
        if (wasUncleanRestart) {
            Log.w(TAG, "Detected unclean restart (OS kill). " +
                "Restart count: ${healthLog.restartCount}, " +
                "Last gap: ~${healthLog.lastGapDurationMs}ms")
        }

        // Create notification channel for persistent service notification
        createServiceNotificationChannel()

        // Start foreground immediately (required within 5s of startForegroundService)
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())

        // Initialize sensor pipeline with context for haptic feedback
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val config = ShakeDetectorConfig(this).snapshot()
        shakeSensorManager = ShakeSensorManager(sensorManager, config, context = this)

        // Initialize chip trigger (Floating overlay with notification fallback)
        chipTrigger = OverlayChipTrigger(this)

        // Wire Ingestion Router (Phase 2.1)
        val router = com.chirag.arthix.notification.TransactionIngestionRouter(reconciliationEngine)
        com.chirag.arthix.notification.UpiNotificationListenerService.transactionRouter = router
        com.chirag.arthix.sms.BankSmsReceiver.router = router

        // Start sensor listening
        val started = shakeSensorManager.start(SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "Sensor listener registered: $started")

        // Register screen-state receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        // Route ShakeEvents through ReconciliationEngine (Phase 2)
        // Engine handles: PendingCapture creation + chip trigger + timeout scheduling
        serviceScope.launch {
            shakeSensorManager.shakeEvents.collect { event ->
                Log.d(TAG, "ShakeEvent emitted: ${event.correlationId}")
                CaptureGraceWindowService.start(this@ShakeDetectionService, event.correlationId)
                reconciliationEngine.onShakeEvent(event)
                healthLog.updateAliveTimestamp()
            }
        }

        // Log shake-and-hold events
        serviceScope.launch {
            shakeSensorManager.shakeAndHoldEvents.collect { event ->
                Log.d(TAG, "ShakeAndHoldEvent emitted: ${event.correlationId}, " +
                    "holdDuration=${event.holdDurationMs}ms")
                CaptureGraceWindowService.extend(this@ShakeDetectionService)
                healthLog.updateAliveTimestamp()
            }
        }

        // Route cancellation signals through ReconciliationEngine
        serviceScope.launch {
            shakeSensorManager.cancellationSignals.collect { signal ->
                Log.d(TAG, "ShakeCancellationSignal: ${signal.correlationId}, " +
                    "reason=${signal.reason}")
                reconciliationEngine.onCancellationSignal(signal)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand (flags=$flags, startId=$startId)")
        // START_STICKY: OS attempts automatic restart after a kill (PRD §7.4)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy — clean shutdown")

        // Record clean shutdown for health gap detection (PRD §7.4)
        healthLog.recordCleanShutdown()

        // Cleanup
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered — safe to ignore
        }
        CaptureGraceWindowService.stop(this)
        shakeSensorManager.stop()
        reconciliationEngine.cancel()
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── ShakeEventSource accessor ──────────────────────────────────────

    /**
     * Returns the [ShakeEventSource] for Phase 2/5 to collect from.
     *
     * In the current architecture, callers access this via a singleton pattern
     * or service binding. For the hackathon build, a companion-level reference
     * is acceptable.
     */
    fun getShakeEventSource(): ShakeEventSource = shakeSensorManager

    // ── Notification setup ─────────────────────────────────────────────

    /**
     * Creates the low-importance notification channel for the persistent
     * foreground-service notification.
     *
     * DISTINCT from the high-importance chip channel in [HeadsUpChipTrigger] —
     * two separate channels, do not conflate (PRD §7.2).
     */
    private fun createServiceNotificationChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Shake detection running",
            NotificationManager.IMPORTANCE_LOW, // no sound/heads-up — just the persistent icon
        ).apply {
            description = "Keeps shake detection active in the background"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildServiceNotification() =
        NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Placeholder — Phase 4 replaces
            .setContentTitle("Arthix")
            .setContentText("Shake detection active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
