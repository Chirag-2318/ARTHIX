package com.chirag.arthix.sensor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controller class encapsulating the bounding logic for capture grace periods.
 * Separated for deterministic unit testing.
 */
class CaptureGraceWindowState(
    private val initialGraceMs: Long = 10_000L,
    private val maxGraceMs: Long = 120_000L,
) {
    private var _startTimeMonotonic: Long = 0L
    private var _currentDurationMs: Long = initialGraceMs
    private var _hasExtended: Boolean = false
    private var _isActive: Boolean = false
    private var _correlationId: String? = null

    fun start(nowMonotonic: Long, correlationId: String? = null): Long {
        _startTimeMonotonic = nowMonotonic
        _currentDurationMs = initialGraceMs
        _hasExtended = false
        _isActive = true
        _correlationId = correlationId
        return _currentDurationMs
    }

    fun extend(nowMonotonic: Long): Long? {
        if (!_isActive || _hasExtended) return null
        _hasExtended = true
        val elapsed = (nowMonotonic - _startTimeMonotonic).coerceAtLeast(0L)
        val newTotalDuration = maxGraceMs
        _currentDurationMs = newTotalDuration
        val remaining = (newTotalDuration - elapsed).coerceAtLeast(0L)
        return remaining
    }

    fun isExpired(nowMonotonic: Long): Boolean {
        if (!_isActive) return true
        val elapsed = nowMonotonic - _startTimeMonotonic
        return elapsed >= _currentDurationMs
    }

    fun stop() {
        _isActive = false
        _hasExtended = false
        _correlationId = null
    }

    val isActive: Boolean get() = _isActive
    val hasExtended: Boolean get() = _hasExtended
    val currentDurationMs: Long get() = _currentDurationMs
    val correlationId: String? get() = _correlationId
}

/**
 * Bounded foreground service that maintains active sensor listening
 * and reconciliation readiness for a bounded grace window (10s initial, extendable
 * once up to 120s upon detected payment activity or motion).
 *
 * Self-terminates when the grace period expires to guarantee zero persistent
 * battery drain.
 */
@AndroidEntryPoint
class CaptureGraceWindowService : Service() {

    companion object {
        private const val TAG = "CaptureGraceWindow"

        const val CHANNEL_ID = "capture_grace_channel"
        private const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.chirag.arthix.sensor.grace.START"
        const val ACTION_EXTEND = "com.chirag.arthix.sensor.grace.EXTEND"
        const val ACTION_STOP = "com.chirag.arthix.sensor.grace.STOP"

        const val EXTRA_CORRELATION_ID = "extra_correlation_id"
        const val EXTRA_INITIAL_GRACE_MS = "extra_initial_grace_ms"
        const val EXTRA_MAX_GRACE_MS = "extra_max_grace_ms"

        const val DEFAULT_INITIAL_GRACE_MS = 10_000L
        const val DEFAULT_MAX_GRACE_MS = 120_000L

        fun start(
            context: Context,
            correlationId: String? = null,
            initialGraceMs: Long = DEFAULT_INITIAL_GRACE_MS,
            maxGraceMs: Long = DEFAULT_MAX_GRACE_MS,
        ) {
            val intent = Intent(context, CaptureGraceWindowService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CORRELATION_ID, correlationId)
                putExtra(EXTRA_INITIAL_GRACE_MS, initialGraceMs)
                putExtra(EXTRA_MAX_GRACE_MS, maxGraceMs)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start CaptureGraceWindowService", e)
            }
        }

        fun extend(context: Context) {
            val intent = Intent(context, CaptureGraceWindowService::class.java).apply {
                action = ACTION_EXTEND
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extend CaptureGraceWindowService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CaptureGraceWindowService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop CaptureGraceWindowService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private val state = CaptureGraceWindowState()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Listening for payment confirmation..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val correlationId = intent.getStringExtra(EXTRA_CORRELATION_ID)
                val initialGraceMs = intent.getLongExtra(EXTRA_INITIAL_GRACE_MS, DEFAULT_INITIAL_GRACE_MS)
                val maxGraceMs = intent.getLongExtra(EXTRA_MAX_GRACE_MS, DEFAULT_MAX_GRACE_MS)
                startGraceWindow(correlationId, initialGraceMs, maxGraceMs)
            }
            ACTION_EXTEND -> {
                extendGraceWindow()
            }
            ACTION_STOP -> {
                stopGraceWindow()
            }
        }
        return START_NOT_STICKY
    }

    private fun startGraceWindow(correlationId: String?, initialGraceMs: Long, maxGraceMs: Long) {
        timerJob?.cancel()
        val duration = state.start(SystemClock.elapsedRealtime(), correlationId)
        Log.d(TAG, "Capture grace window started ($duration ms) for $correlationId")

        timerJob = serviceScope.launch {
            delay(duration)
            Log.d(TAG, "Capture grace window expired — self-terminating")
            stopSelf()
        }
    }

    private fun extendGraceWindow() {
        val remaining = state.extend(SystemClock.elapsedRealtime())
        if (remaining != null) {
            Log.d(TAG, "Capture grace window extended by remaining $remaining ms (up to 120s cap)")
            timerJob?.cancel()
            timerJob = serviceScope.launch {
                delay(remaining)
                Log.d(TAG, "Extended grace window expired — self-terminating")
                stopSelf()
            }
        }
    }

    private fun stopGraceWindow() {
        timerJob?.cancel()
        state.stop()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capture Grace Window",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Temporary window to capture background payment confirmation"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("ARTHIX Active")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
