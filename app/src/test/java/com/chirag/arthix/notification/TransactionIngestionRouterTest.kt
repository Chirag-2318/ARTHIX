package com.chirag.arthix.notification

import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.notification.model.ConfidenceLevel
import com.chirag.arthix.notification.model.NotificationOutcome
import com.chirag.arthix.notification.model.ParsedOutflow
import com.chirag.arthix.notification.model.TransactionCandidate
import com.chirag.arthix.notification.model.TransactionSourceType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class TransactionIngestionRouterTest {

    private lateinit var engine: ReconciliationEngine
    private lateinit var router: TransactionIngestionRouter

    @Before
    fun setup() {
        engine = mock(ReconciliationEngine::class.java)
        router = TransactionIngestionRouter(engine)
    }

    @Test
    fun ingest_rejectedOutcome_ignored() {
        val candidate = createCandidate(outcome = NotificationOutcome.REJECTED)
        router.ingest(candidate)
        
        verifyNoInteractions(engine)
    }

    @Test
    fun ingest_refundOutcome_routedToRefund() {
        val candidate = createCandidate(
            outcome = NotificationOutcome.REFUND,
            direction = Direction.INFLOW,
            amountPaise = 50000L,
            payee = "Zomato"
        )
        router.ingest(candidate)
        
        verify(engine).onRefundNotification(50000L, "Zomato")
    }

    @Test
    fun ingest_crossSourceDedup_samePayment_deduped() {
        val smsCandidate = createCandidate(
            sourceType = TransactionSourceType.BANK_SMS,
            amountPaise = 15000L,
            timestampMs = 1000L
        )
        
        val upiCandidate = createCandidate(
            sourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
            amountPaise = 15000L,
            timestampMs = 1500L // 500ms later
        )
        
        router.ingest(smsCandidate)
        router.ingest(upiCandidate)
        
        // Should only be routed to engine ONCE
        verify(engine, org.mockito.Mockito.times(1)).onNotificationCandidate(
            org.mockito.ArgumentMatchers.any(ParsedOutflow::class.java) ?: ParsedOutflow(0, "", "", "", ConfidenceFlag.CLEAN),
            org.mockito.ArgumentMatchers.anyString() ?: ""
        )
    }

    @Test
    fun ingest_crossSourceDedup_differentAmount_bothRouted() {
        val smsCandidate = createCandidate(
            sourceType = TransactionSourceType.BANK_SMS,
            amountPaise = 15000L,
            timestampMs = 1000L
        )
        
        val upiCandidate = createCandidate(
            sourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
            amountPaise = 20000L,
            timestampMs = 1500L
        )
        
        router.ingest(smsCandidate)
        router.ingest(upiCandidate)
        
        // Both should be routed
        verify(engine, org.mockito.Mockito.times(2)).onNotificationCandidate(
            org.mockito.ArgumentMatchers.any(ParsedOutflow::class.java) ?: ParsedOutflow(0, "", "", "", ConfidenceFlag.CLEAN),
            org.mockito.ArgumentMatchers.anyString() ?: ""
        )
    }

    private fun createCandidate(
        sourceType: TransactionSourceType = TransactionSourceType.UPI_APP_NOTIFICATION,
        outcome: NotificationOutcome = NotificationOutcome.COMPLETED,
        direction: Direction = Direction.OUTFLOW,
        amountPaise: Long = 10000L,
        payee: String = "Test",
        timestampMs: Long = 0L
    ) = TransactionCandidate(
        sourceType = sourceType,
        sourcePackage = if (sourceType == TransactionSourceType.UPI_APP_NOTIFICATION) "com.phonepe.app" else null,
        senderAddress = if (sourceType == TransactionSourceType.BANK_SMS) "VM-HDFCBK" else null,
        amountPaise = amountPaise,
        payee = payee,
        direction = direction,
        referenceId = "123456789012",
        rawFingerprint = "hash123",
        confidence = ConfidenceLevel.HIGH,
        timestampMs = timestampMs,
        outcome = outcome
    )
}
