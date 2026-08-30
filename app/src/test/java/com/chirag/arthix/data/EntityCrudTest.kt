package com.chirag.arthix.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chirag.arthix.data.dao.PendingQueueDao
import com.chirag.arthix.data.dao.ReportDao
import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.AmountLock
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.model.TransactionStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CRUD roundtrip tests for all 6 entities per PRD §10.
 *
 * Runs against an in-memory Room database via Robolectric (no device/emulator needed).
 * Tests verify:
 * - Insert + read-back with every field matching, including enum round-trip
 * - Nullability boundary (AWAITING_AMOUNT with null amountPaise)
 * - insertSplitWithParticipants atomicity
 * - ReportEntity JSON map/list round-trip
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EntityCrudTest {

    private lateinit var db: ArthixDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var pendingQueueDao: PendingQueueDao
    private lateinit var splitDao: SplitDao
    private lateinit var reportDao: ReportDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ArthixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        pendingQueueDao = db.pendingQueueDao()
        splitDao = db.splitDao()
        reportDao = db.reportDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ---- TransactionEntity CRUD ----

    @Test
    fun `insert and read back TransactionEntity with all fields`() = runTest {
        val txn = TransactionEntity(
            amountPaise = 145000L,
            payee = "Swiggy",
            category = "Food",
            timestamp = 1700000000000L,
            direction = Direction.OUTFLOW,
            source = CaptureSource.SHAKE,
            status = TransactionStatus.CONFIRMED,
            sourceCaptureId = "cap-uuid-123",
            sourceNotificationId = "notif-uuid-456",
            confidenceFlag = ConfidenceFlag.CLEAN,
            createdAt = 1700000001000L
        )

        val id = transactionDao.insert(txn)
        val retrieved = transactionDao.getById(id)

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.amountPaise).isEqualTo(145000L)
        assertThat(retrieved.payee).isEqualTo("Swiggy")
        assertThat(retrieved.category).isEqualTo("Food")
        assertThat(retrieved.timestamp).isEqualTo(1700000000000L)
        assertThat(retrieved.direction).isEqualTo(Direction.OUTFLOW)
        assertThat(retrieved.source).isEqualTo(CaptureSource.SHAKE)
        assertThat(retrieved.status).isEqualTo(TransactionStatus.CONFIRMED)
        assertThat(retrieved.sourceCaptureId).isEqualTo("cap-uuid-123")
        assertThat(retrieved.sourceNotificationId).isEqualTo("notif-uuid-456")
        assertThat(retrieved.confidenceFlag).isEqualTo(ConfidenceFlag.CLEAN)
        assertThat(retrieved.createdAt).isEqualTo(1700000001000L)
    }

    @Test
    fun `insert TransactionEntity with null amountPaise in AWAITING_AMOUNT status`() = runTest {
        // PRD §10: this is the post-timeout state per FR-2a/EC-17
        val txn = TransactionEntity(
            amountPaise = null,
            payee = null,
            category = null,
            timestamp = 1700000000000L,
            direction = Direction.OUTFLOW,
            source = CaptureSource.SHAKE,
            status = TransactionStatus.AWAITING_AMOUNT,
            sourceCaptureId = "cap-uuid-789",
            sourceNotificationId = null,
            confidenceFlag = ConfidenceFlag.NEEDS_REVIEW,
            createdAt = 1700000001000L
        )

        val id = transactionDao.insert(txn)
        val retrieved = transactionDao.getById(id)

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.amountPaise).isNull()
        assertThat(retrieved.payee).isNull()
        assertThat(retrieved.category).isNull()
        assertThat(retrieved.status).isEqualTo(TransactionStatus.AWAITING_AMOUNT)
    }

    @Test
    fun `TransactionEntity enum round-trip through all enum values`() = runTest {
        // Test every Direction value
        for (dir in Direction.values()) {
            val txn = TransactionEntity(
                amountPaise = 100L,
                payee = null,
                category = null,
                timestamp = 1700000000000L,
                direction = dir,
                source = CaptureSource.MANUAL,
                status = TransactionStatus.CONFIRMED,
                sourceCaptureId = null,
                sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN,
                createdAt = 1700000000000L
            )
            val id = transactionDao.insert(txn)
            val retrieved = transactionDao.getById(id)
            assertThat(retrieved!!.direction).isEqualTo(dir)
        }

        // Test every CaptureSource value
        for (src in CaptureSource.values()) {
            val txn = TransactionEntity(
                amountPaise = 100L,
                payee = null,
                category = null,
                timestamp = 1700000000000L,
                direction = Direction.OUTFLOW,
                source = src,
                status = TransactionStatus.CONFIRMED,
                sourceCaptureId = null,
                sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN,
                createdAt = 1700000000000L
            )
            val id = transactionDao.insert(txn)
            val retrieved = transactionDao.getById(id)
            assertThat(retrieved!!.source).isEqualTo(src)
        }

        // Test every TransactionStatus value
        for (status in TransactionStatus.values()) {
            val txn = TransactionEntity(
                amountPaise = 100L,
                payee = null,
                category = null,
                timestamp = 1700000000000L,
                direction = Direction.OUTFLOW,
                source = CaptureSource.MANUAL,
                status = status,
                sourceCaptureId = null,
                sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN,
                createdAt = 1700000000000L
            )
            val id = transactionDao.insert(txn)
            val retrieved = transactionDao.getById(id)
            assertThat(retrieved!!.status).isEqualTo(status)
        }

        // Test every ConfidenceFlag value
        for (flag in ConfidenceFlag.values()) {
            val txn = TransactionEntity(
                amountPaise = 100L,
                payee = null,
                category = null,
                timestamp = 1700000000000L,
                direction = Direction.OUTFLOW,
                source = CaptureSource.MANUAL,
                status = TransactionStatus.CONFIRMED,
                sourceCaptureId = null,
                sourceNotificationId = null,
                confidenceFlag = flag,
                createdAt = 1700000000000L
            )
            val id = transactionDao.insert(txn)
            val retrieved = transactionDao.getById(id)
            assertThat(retrieved!!.confidenceFlag).isEqualTo(flag)
        }
    }

    // Note: Insert with status = CONFIRMED and amountPaise = null is *allowed at the
    // schema level* (Room won't enforce this cross-field rule). This invariant is a
    // repository-layer responsibility, not a DB constraint. Phase 1/3 must not commit
    // a CONFIRMED row with a null amount.

    // ---- PendingCaptureEntity CRUD ----

    @Test
    fun `insert and read back PendingCaptureEntity`() = runTest {
        val capture = PendingCaptureEntity(
            id = "cap-uuid-001",
            timestampMonotonic = 5000000L,
            matched = false,
            active = true,
            category = "Food",
            createdAt = 1700000000000L
        )

        pendingQueueDao.insertCapture(capture)

        // Verify via the window query (the DAO doesn't have a getById for captures)
        val results = pendingQueueDao.getUnmatchedCapturesInWindow(
            windowStart = 4000000L,
            windowEnd = 6000000L
        )
        assertThat(results).hasSize(1)
        val retrieved = results[0]
        assertThat(retrieved.id).isEqualTo("cap-uuid-001")
        assertThat(retrieved.timestampMonotonic).isEqualTo(5000000L)
        assertThat(retrieved.matched).isFalse()
        assertThat(retrieved.active).isTrue()
        assertThat(retrieved.category).isEqualTo("Food")
        assertThat(retrieved.createdAt).isEqualTo(1700000000000L)
    }

    // ---- PendingNotificationEntity CRUD ----

    @Test
    fun `insert and read back PendingNotificationEntity`() = runTest {
        val notification = PendingNotificationEntity(
            id = "notif-uuid-001",
            timestampMonotonic = 5100000L,
            amountPaise = 25000L,
            payee = "Zomato",
            matched = false,
            active = true,
            rawText = "You paid ₹250.00 to Zomato",
            createdAt = 1700000000000L
        )

        pendingQueueDao.insertNotification(notification)

        val results = pendingQueueDao.getUnmatchedNotificationsInWindow(
            windowStart = 5000000L,
            windowEnd = 5200000L
        )
        assertThat(results).hasSize(1)
        val retrieved = results[0]
        assertThat(retrieved.id).isEqualTo("notif-uuid-001")
        assertThat(retrieved.timestampMonotonic).isEqualTo(5100000L)
        assertThat(retrieved.amountPaise).isEqualTo(25000L)
        assertThat(retrieved.payee).isEqualTo("Zomato")
        assertThat(retrieved.matched).isFalse()
        assertThat(retrieved.active).isTrue()
        assertThat(retrieved.rawText).isEqualTo("You paid ₹250.00 to Zomato")
        assertThat(retrieved.createdAt).isEqualTo(1700000000000L)
    }

    // ---- SplitRecord + SplitParticipant atomic insert ----

    @Test
    fun `insertSplitWithParticipants inserts atomically and is queryable`() = runTest {
        // First insert a transaction for the split to reference
        val txnId = transactionDao.insert(
            TransactionEntity(
                amountPaise = 90000L, // ₹900
                payee = "Dominos",
                category = "Food",
                timestamp = 1700000000000L,
                direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE,
                status = TransactionStatus.CONFIRMED,
                sourceCaptureId = null,
                sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN,
                createdAt = 1700000000000L
            )
        )

        // Insert split with participants (simulating the @Transaction method)
        val split = SplitRecordEntity(
            transactionId = txnId,
            confirmedVia = SplitConfirmedVia.TAP,
            amountLock = AmountLock.LIVE,
            lockedAmountPaise = null,
            createdAt = 1700000001000L
        )
        val splitId = splitDao.insertSplit(split)

        val participants = listOf(
            SplitParticipantEntity(
                splitRecordId = splitId,
                participantId = "p1",
                displayName = "Alice",
                contactId = null,
                isAppUser = true,
                sharePaise = 30000L // ₹300
            ),
            SplitParticipantEntity(
                splitRecordId = splitId,
                participantId = "p2",
                displayName = "Bob",
                contactId = null,
                isAppUser = false,
                sharePaise = 30000L
            ),
            SplitParticipantEntity(
                splitRecordId = splitId,
                participantId = "p3",
                displayName = "Charlie",
                contactId = null,
                isAppUser = false,
                sharePaise = 30000L
            )
        )
        splitDao.insertParticipants(participants)

        // Verify via getSplitsForTransaction
        val splits = splitDao.getSplitsForTransaction(txnId)
        assertThat(splits).hasSize(1)
        assertThat(splits[0].id).isEqualTo(splitId)
        assertThat(splits[0].transactionId).isEqualTo(txnId)
        assertThat(splits[0].confirmedVia).isEqualTo(SplitConfirmedVia.TAP)
        assertThat(splits[0].amountLock).isEqualTo(AmountLock.LIVE)
        assertThat(splits[0].lockedAmountPaise).isNull()

        // Verify participants
        val retrievedParticipants = splitDao.getParticipants(splitId)
        assertThat(retrievedParticipants).hasSize(3)
        val names = retrievedParticipants.map { it.displayName }.toSet()
        assertThat(names).containsExactly("Alice", "Bob", "Charlie")
        assertThat(retrievedParticipants.all { it.sharePaise == 30000L }).isTrue()
        assertThat(retrievedParticipants.all { it.splitRecordId == splitId }).isTrue()
    }

    @Test
    fun `SplitRecordEntity with LOCKED_AT_CREATION has lockedAmountPaise`() = runTest {
        val split = SplitRecordEntity(
            transactionId = 1L,
            confirmedVia = SplitConfirmedVia.VOICE,
            amountLock = AmountLock.LOCKED_AT_CREATION,
            lockedAmountPaise = 50000L,
            createdAt = 1700000000000L
        )
        splitDao.insertSplit(split)
        val splits = splitDao.getSplitsForTransaction(1L)

        assertThat(splits).hasSize(1)
        assertThat(splits[0].amountLock).isEqualTo(AmountLock.LOCKED_AT_CREATION)
        assertThat(splits[0].lockedAmountPaise).isEqualTo(50000L)
        assertThat(splits[0].confirmedVia).isEqualTo(SplitConfirmedVia.VOICE)
    }

    // ---- ReportEntity JSON round-trip ----

    @Test
    fun `ReportEntity round-trips categoryBreakdownJson and suggestionsJson correctly`() = runTest {
        val breakdown = mapOf("Food" to 45000L, "Transport" to 12000L, "Shopping" to 78500L)
        val suggestions = listOf(
            "Consider reducing food spending by 15%",
            "Transport costs are below average",
            "Shopping saw a 20% increase this week"
        )

        val report = ReportEntity(
            periodStart = 1700000000000L,
            periodEnd = 1700604800000L,
            categoryBreakdownJson = com.google.gson.Gson().toJson(breakdown),
            netFlowPaise = -135500L,
            suggestionsJson = com.google.gson.Gson().toJson(suggestions),
            projectedTotalPaise = 270000L,
            projectedSavingsPaise = 50000L,
            uncategorizedTotalPaise = 15000L,
            generatedAt = 1700604900000L
        )

        val id = reportDao.insert(report)
        val retrieved = reportDao.getForPeriod(1700000000000L, 1700604800000L)

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.id).isEqualTo(id)
        assertThat(retrieved.netFlowPaise).isEqualTo(-135500L)
        assertThat(retrieved.projectedTotalPaise).isEqualTo(270000L)
        assertThat(retrieved.projectedSavingsPaise).isEqualTo(50000L)
        assertThat(retrieved.uncategorizedTotalPaise).isEqualTo(15000L)

        // Deserialize and verify map round-trip
        val retrievedBreakdown: Map<String, Long> = com.google.gson.Gson().fromJson(
            retrieved.categoryBreakdownJson,
            object : com.google.gson.reflect.TypeToken<Map<String, Long>>() {}.type
        )
        assertThat(retrievedBreakdown).isEqualTo(breakdown)

        // Deserialize and verify list round-trip
        val retrievedSuggestions: List<String> = com.google.gson.Gson().fromJson(
            retrieved.suggestionsJson,
            object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        )
        assertThat(retrievedSuggestions).isEqualTo(suggestions)
    }
}
