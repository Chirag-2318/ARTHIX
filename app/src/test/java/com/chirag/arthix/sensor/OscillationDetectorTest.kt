package com.chirag.arthix.sensor

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Synthetic test harness for [OscillationDetector] (PRD §9 step 4, §10.1).
 *
 * All tests feed pre-authored (timestamp, magnitude) sequences into the
 * detector and assert whether SHAKE_ONSET fires or not. No Android
 * framework dependencies — pure JVM tests.
 *
 * Three distinct false-positive categories are tested independently (PRD §3.4 / EC-01):
 * 1. Walking — rhythmic, moderate-amplitude, ~2 Hz
 * 2. Pocket-jostling — irregular, low-amplitude, higher-frequency
 * 3. Vehicle vibration — near-constant, high-frequency, no clean sign-reversal
 */
class OscillationDetectorTest {

    private lateinit var detector: OscillationDetector
    private val onsets = mutableListOf<Long>()

    @Before
    fun setUp() {
        // Use default config: threshold=12, window=500ms, minReversals=2
        detector = OscillationDetector(ShakeDetectorConfigSnapshot())
        detector.onShakeOnset = { ts -> onsets.add(ts) }
        onsets.clear()
    }

    // ── Positive cases ─────────────────────────────────────────────────

    @Test
    fun `clean double-reversal within window fires onset`() {
        // Simulate: idle → above threshold → below → above → below (2 reversals)
        // 20ms sample interval (50Hz, screen-on rate)
        val samples = listOf(
            0L to 0f,       // idle
            20L to 2f,      // idle
            40L to 5f,      // rising
            60L to 14f,     // above threshold (rising edge → positive crossing)
            80L to 16f,     // sustained above
            100L to 10f,    // dropping
            120L to 5f,     // below threshold (falling edge → negative crossing) — 1st reversal implicit
            140L to 3f,     // below
            160L to 8f,     // rising again
            180L to 15f,    // above threshold (rising edge → positive crossing) — this is a reversal from the falling
            200L to 18f,    // sustained
            220L to 11f,    // dropping
            240L to 6f,     // below threshold (falling edge) — 2nd reversal: positive→negative
        )

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isNotEmpty()
    }

    @Test
    fun `vigorous shake with multiple reversals fires onset`() {
        // Strong back-and-forth shake motion
        val samples = buildVigorousShakeSequence(startMs = 0, durationMs = 400)

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isNotEmpty()
    }

    // ── Negative cases ─────────────────────────────────────────────────

    @Test
    fun `single spike without reversal does not fire onset`() {
        // One bump above threshold, then drops — only 1 crossing pair, no reversal pattern
        val samples = listOf(
            0L to 0f,
            20L to 3f,
            40L to 8f,
            60L to 15f,     // above threshold (positive crossing)
            80L to 18f,
            100L to 13f,
            120L to 8f,     // below threshold (negative crossing) — but only 1 direction change
            140L to 4f,
            160L to 2f,
            180L to 1f,
            // No further crossings → only 1 reversal max, need ≥2
        )

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isEmpty()
    }

    @Test
    fun `two reversals spread beyond window does not fire onset`() {
        // First crossing pair within first 200ms, second crossing pair 700ms later
        // Window is 500ms — second pair falls outside
        val samples = listOf(
            // First up-down cycle
            0L to 0f,
            20L to 14f,     // above (positive crossing)
            40L to 16f,
            60L to 10f,
            80L to 5f,      // below (negative crossing) — 1st reversal
            // Long gap of idle
            100L to 2f,
            200L to 1f,
            300L to 1f,
            400L to 1f,
            500L to 1f,
            600L to 1f,
            // Second up-down cycle — now at 700ms, first crossing was at 80ms (620ms ago > 500ms window)
            700L to 14f,    // above (positive crossing)
            720L to 16f,
            740L to 10f,
            760L to 5f,     // below — but the earlier crossings are outside the window
        )

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isEmpty()
    }

    // ── False-positive category 1: Walking (PRD §3.4) ──────────────────

    @Test
    fun `walking pattern does not fire onset`() {
        // Walking: rhythmic, ~2 Hz, moderate amplitude (4-8 m/s²), never exceeds 12 m/s²
        // 60 seconds of simulated walking at 50Hz = 3000 samples, but we test 2 seconds
        val samples = mutableListOf<Pair<Long, Float>>()
        // 2 Hz sine wave, amplitude 6 m/s², offset 3 m/s² (peak ~9 m/s², well under 12)
        for (i in 0 until 100) { // 2 seconds at 50Hz
            val t = i * 20L
            val magnitude = 3f + 6f * kotlin.math.sin(2.0 * Math.PI * 2.0 * t / 1000.0).toFloat()
            // magnitude ranges from -3 to 9, but magnitude is always positive (it's a sqrt)
            // In practice, linear acceleration magnitude from walking oscillates 2-9 m/s²
            samples.add(t to kotlin.math.abs(magnitude))
        }

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isEmpty()
    }

    // ── False-positive category 2: Pocket-jostling (PRD §3.4) ──────────

    @Test
    fun `pocket jostle pattern does not fire onset`() {
        // Pocket: irregular, low amplitude (2-7 m/s²), higher frequency noise
        // Many small oscillations but none clearing threshold
        val samples = mutableListOf<Pair<Long, Float>>()
        val random = java.util.Random(42) // deterministic seed
        for (i in 0 until 100) { // 2 seconds at 50Hz
            val t = i * 20L
            // Random noise between 1-8 m/s², occasionally spiking to 10 but never 12
            val magnitude = 1f + random.nextFloat() * 7f +
                if (random.nextFloat() > 0.9f) 2f else 0f // occasional bump
            samples.add(t to magnitude)
        }

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isEmpty()
    }

    // ── False-positive category 3: Vehicle vibration (PRD §3.4) ────────

    @Test
    fun `vehicle vibration pattern does not fire onset`() {
        // Vehicle: near-constant, high-frequency buzz, moderate amplitude
        // Characteristically stays in a band without clean directional reversals
        // through the threshold — it hovers around 5-10 m/s² with rapid jitter
        val samples = mutableListOf<Pair<Long, Float>>()
        val random = java.util.Random(99)
        for (i in 0 until 150) { // 3 seconds at 50Hz
            val t = i * 20L
            // Base vibration 6 m/s² + high-freq noise ±4, peaks at ~10 but under 12
            val magnitude = 6f + (random.nextFloat() - 0.5f) * 8f
            samples.add(t to kotlin.math.abs(magnitude).coerceAtMost(11f))
        }

        for ((ts, mag) in samples) {
            detector.onSensorSample(ts, mag)
        }

        assertThat(onsets).isEmpty()
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Builds a vigorous shake sequence: alternating above/below threshold
     * with clean reversals, simulating a deliberate wrist-flick.
     */
    private fun buildVigorousShakeSequence(
        startMs: Long,
        durationMs: Long,
    ): List<Pair<Long, Float>> {
        val samples = mutableListOf<Pair<Long, Float>>()
        val intervalMs = 20L
        var t = startMs
        var phase = 0

        while (t < startMs + durationMs) {
            val magnitude = when (phase % 4) {
                0 -> 2f    // idle
                1 -> 18f   // above threshold (positive peak)
                2 -> 4f    // below threshold (trough)
                3 -> 16f   // above threshold again (second peak)
                else -> 2f
            }
            samples.add(t to magnitude)
            t += intervalMs
            phase++
        }
        return samples
    }
}
