package com.chirag.arthix.voice

import android.content.Context
import com.chirag.arthix.data.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests for [IdleDetector].
 *
 * Tests the 4-part condition gating specified in EC-26:
 * - Screen activity window (30 minutes)
 * - Audio silence / DND state
 * - Pending transaction records availability
 */
class IdleDetectorTest {

    private val context = mock(Context::class.java)
    private val repository = mock(TransactionRepository::class.java)

    private var currentTime = 100_000_000L
    private var isSilenced = false
    private var isInteractive = true

    private val fakeClock = object : MonotonicClock {
        override fun elapsedRealtimeMs(): Long = currentTime
    }

    private val fakeAudio = object : AudioSilenceChecker {
        override fun isSilenced(): Boolean = isSilenced
    }

    private val fakeScreen = object : ScreenInteractivityChecker {
        override fun isInteractive(): Boolean = isInteractive
    }

    private lateinit var detector: IdleDetector

    @Before
    fun setUp() {
        currentTime = 100_000_000L
        isSilenced = false
        isInteractive = true

        detector = IdleDetector(
            transactionRepository = repository,
            clock = fakeClock,
            screenChecker = fakeScreen,
            audioChecker = fakeAudio,
        )
    }

    @Test
    fun `all conditions met - shouldTrigger returns true`() = runBlocking {
        `when`(repository.hasPendingVoiceRecords()).thenReturn(true)
        val lastScreenActive = currentTime - (10 * 60 * 1000L) // 10 min ago

        assertTrue(detector.shouldTrigger(lastScreenActive))
    }

    @Test
    fun `screen inactive for more than 30 mins - shouldTrigger returns false (EC-26)`() = runBlocking {
        `when`(repository.hasPendingVoiceRecords()).thenReturn(true)
        val lastScreenActive = currentTime - (35 * 60 * 1000L) // 35 min ago (exceeds 30 min window)

        assertFalse(detector.shouldTrigger(lastScreenActive))
    }

    @Test
    fun `phone is silenced or in DND - shouldTrigger returns false (EC-26)`() = runBlocking {
        `when`(repository.hasPendingVoiceRecords()).thenReturn(true)
        isSilenced = true
        val lastScreenActive = currentTime - (5 * 60 * 1000L)

        assertFalse(detector.shouldTrigger(lastScreenActive))
    }

    @Test
    fun `no pending voice records - shouldTrigger returns false`() = runBlocking {
        `when`(repository.hasPendingVoiceRecords()).thenReturn(false)
        val lastScreenActive = currentTime - (5 * 60 * 1000L)

        assertFalse(detector.shouldTrigger(lastScreenActive))
    }
}
