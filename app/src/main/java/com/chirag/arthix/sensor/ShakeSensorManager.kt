package com.chirag.arthix.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

/**
 * Glues the sensor pipeline together: SensorEventListener → OscillationDetector
 * → GestureStateMachine → Flow emissions.
 *
 * Implements [SensorEventListener] for Android sensor callbacks and
 * [ShakeEventSource] for downstream consumers (Phase 2 / Phase 5).
 *
 * Lifecycle:
 * - Call [start] to register the sensor listener.
 * - Call [stop] to unregister.
 * - Call [reregisterSensor] to change sampling rate (screen on/off — PRD §2.3).
 *
 * All timestamps use [SystemClock.elapsedRealtime] exclusively (EC-19).
 *
 * Thread model: [onSensorChanged] runs on the sensor callback thread.
 * The [MutableSharedFlow] emissions are consumed by Phase 2's collector
 * on its own serial dispatcher (EC-18). The boundary is the Flow, not
 * a shared mutable structure (PRD §8.1).
 */
class ShakeSensorManager(
    private val sensorManager: SensorManager,
    config: ShakeDetectorConfigSnapshot = ShakeDetectorConfigSnapshot(),
    private val context: Context? = null,
    private val onShakeFeedback: (() -> Unit)? = null,
) : SensorEventListener, ShakeEventSource {

    companion object {
        private const val TAG = "ShakeSensorManager"
    }

    // ── Core detection pipeline ────────────────────────────────────────

    private val oscillationDetector = OscillationDetector(config)
    private val debounceGate = DebounceGate(config.debounceMs)
    private val gestureStateMachine = GestureStateMachine(config, debounceGate)

    // ── Flow emitters (ShakeEventSource implementation) ────────────────

    private val _shakeEvents = MutableSharedFlow<ShakeEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val shakeEvents: Flow<ShakeEvent> = _shakeEvents.asSharedFlow()

    private val _shakeAndHoldEvents = MutableSharedFlow<ShakeAndHoldEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val shakeAndHoldEvents: Flow<ShakeAndHoldEvent> = _shakeAndHoldEvents.asSharedFlow()

    private val _cancellationSignals = MutableSharedFlow<ShakeCancellationSignal>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val cancellationSignals: Flow<ShakeCancellationSignal> = _cancellationSignals.asSharedFlow()

    // ── Sensor reference ───────────────────────────────────────────────

    private val linearAccelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    /** Current sampling delay — tracks what we've registered with. */
    private var currentDelay: Int = SensorManager.SENSOR_DELAY_GAME

    /** Whether the sensor listener is currently registered. */
    private var isRegistered = false

    // ── Initialization: wire callbacks ─────────────────────────────────

    init {
        // Wire OscillationDetector onset → GestureStateMachine
        oscillationDetector.onShakeOnset = { timestampMs ->
            gestureStateMachine.onShakeOnset(timestampMs)
        }

        // Wire GestureStateMachine outputs → Flow emissions
        gestureStateMachine.onShakeEvent = { event ->
            triggerHapticFeedback()
            _shakeEvents.tryEmit(event)
        }
        gestureStateMachine.onShakeAndHoldEvent = { event ->
            _shakeAndHoldEvents.tryEmit(event)
        }
        gestureStateMachine.onCancellationSignal = { signal ->
            _cancellationSignals.tryEmit(signal)
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerHapticFeedback() {
        try {
            onShakeFeedback?.invoke()
            val ctx = context ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                ) ?: run {
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform shake haptic feedback", e)
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────

    /**
     * Start listening for linear acceleration sensor events.
     *
     * @param delay sensor sampling delay, default [SensorManager.SENSOR_DELAY_GAME].
     * @return true if the sensor was registered successfully.
     */
    fun start(delay: Int = SensorManager.SENSOR_DELAY_GAME): Boolean {
        val sensor = linearAccelSensor ?: return false
        if (isRegistered) stop()

        currentDelay = delay
        isRegistered = sensorManager.registerListener(this, sensor, delay)
        return isRegistered
    }

    /** Stop listening for sensor events and reset detection state. */
    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
        oscillationDetector.reset()
        gestureStateMachine.reset()
    }

    /**
     * Re-register the sensor listener with a new sampling delay.
     * Required for screen-state-adaptive sampling (PRD §2.3) — Android
     * requires re-registration to change delay on an already-registered listener.
     *
     * @param delay new sensor delay, e.g. [SensorManager.SENSOR_DELAY_GAME] or
     *              [SensorManager.SENSOR_DELAY_NORMAL].
     */
    fun reregisterSensor(delay: Int) {
        if (delay == currentDelay && isRegistered) return
        stop()
        start(delay)
    }

    // ── SensorEventListener ────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        // Compute magnitude: sqrt(x² + y² + z²)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Use monotonic time — NEVER wall clock (EC-19 / PRD §8.2)
        val timestampMs = SystemClock.elapsedRealtime()

        // Feed into the detection pipeline
        oscillationDetector.onSensorSample(timestampMs, magnitude)

        // Feed into the state machine for hold tracking
        // (the state machine also needs continuous magnitude updates
        //  to determine when motion stops or crosses the hold threshold)
        gestureStateMachine.onSensorUpdate(timestampMs, magnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed — accuracy changes don't affect shake detection
    }
}
