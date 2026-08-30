package com.chirag.arthix.report

import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.AmountLock
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.report.split.SplitGroupSuggestionHeuristic
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SplitGroupSuggestionHeuristicTest {

    private lateinit var splitDao: SplitDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var heuristic: SplitGroupSuggestionHeuristic

    @Before
    fun setup() {
        splitDao = mock(SplitDao::class.java)
        transactionDao = mock(TransactionDao::class.java)
        heuristic = SplitGroupSuggestionHeuristic(
            splitDao = splitDao,
            transactionDao = transactionDao,
        )
    }

    @Test
    fun suggestGroup_coldStart_returnsNull() = runTest {
        // EC-41 Critical Test:
        // When no split history exists, heuristic must return null (prompting manual selection)
        // rather than guessing blindly.
        val nowMs = 5000L
        val startMs = nowMs - (90L * 86_400_000L)
        `when`(transactionDao.getInRange(startMs, nowMs)).thenReturn(emptyList())

        val suggestion = heuristic.suggestGroup("food", timestampMs = nowMs)

        assertThat(suggestion).isNull()
    }

    @Test
    fun suggestGroup_withMatchingHistory_returnsTopCandidateGroup() = runTest {
        val nowMs = 1700000000000L
        val startMs = nowMs - (90L * 86_400_000L)
        val pastTxn = TransactionEntity(
            id = 10,
            amountPaise = 60_000L,
            payee = "Swiggy",
            category = "food",
            timestamp = nowMs - 3600000L, // 1 hour ago
            direction = Direction.OUTFLOW,
            source = CaptureSource.MANUAL,
            status = TransactionStatus.CONFIRMED,
            sourceCaptureId = null,
            sourceNotificationId = null,
            confidenceFlag = ConfidenceFlag.CLEAN,
            createdAt = nowMs - 3600000L,
        )

        val splitRecord = SplitRecordEntity(
            id = 101,
            transactionId = 10,
            confirmedVia = SplitConfirmedVia.TAP,
            amountLock = AmountLock.LIVE,
            lockedAmountPaise = null,
            createdAt = nowMs - 3600000L,
        )

        val participants = listOf(
            SplitParticipantEntity(id = 1, splitRecordId = 101, contactName = "Aman", sharePaise = 30_000L),
            SplitParticipantEntity(id = 2, splitRecordId = 101, contactName = "Rohan", sharePaise = 30_000L),
        )

        `when`(transactionDao.getInRange(startMs, nowMs)).thenReturn(listOf(pastTxn))
        `when`(splitDao.getSplitsForTransaction(10)).thenReturn(listOf(splitRecord))
        `when`(splitDao.getParticipants(101)).thenReturn(participants)

        val suggestion = heuristic.suggestGroup("food", timestampMs = nowMs)

        assertThat(suggestion).isNotNull()
        assertThat(suggestion?.participantNames).containsExactly("Aman", "Rohan")
        assertThat(suggestion?.confidence).isAtLeast(0.5f)
    }
}
