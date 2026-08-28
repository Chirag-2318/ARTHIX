package com.chirag.arthix.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chirag.arthix.data.dao.PendingQueueDao
import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.TransactionStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/**
 * DAO query-pattern sanity checks per PRD §10.
 *
 * Seeds randomized data and verifies query results against brute-force
 * in-memory filters (correctness, not performance).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class DaoQueryTest {

    private lateinit var db: ArthixDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var pendingQueueDao: PendingQueueDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ArthixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        pendingQueueDao = db.pendingQueueDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ---- getUnmatchedCapturesInWindow correctness test ----

    @Test
    fun `getUnmatchedCapturesInWindow matches brute-force filter on 500 rows`() = runTest {
        val random = Random(42) // fixed seed for reproducibility
        val captures = (1..500).map { i ->
            PendingCaptureEntity(
                id = "cap-$i",
                timestampMonotonic = random.nextLong(1_000_000L, 10_000_000L),
                matched = random.nextBoolean(),
                active = random.nextBoolean(),
                category = if (random.nextBoolean()) "Food" else null,
                createdAt = 1700000000000L + i
            )
        }

        // Insert all
        captures.forEach { pendingQueueDao.insertCapture(it) }

        // Query window
        val windowStart = 3_000_000L
        val windowEnd = 7_000_000L
        val queryResult = pendingQueueDao.getUnmatchedCapturesInWindow(windowStart, windowEnd)

        // Brute-force expected result
        val expected = captures.filter { c ->
            !c.matched && c.active &&
                    c.timestampMonotonic in windowStart..windowEnd
        }.sortedBy { it.timestampMonotonic }

        assertThat(queryResult.map { it.id }).isEqualTo(expected.map { it.id })
        assertThat(queryResult).hasSize(expected.size)
    }

    // ---- getUnmatchedNotificationsInWindow correctness test ----

    @Test
    fun `getUnmatchedNotificationsInWindow matches brute-force filter`() = runTest {
        val random = Random(99)
        val notifications = (1..500).map { i ->
            PendingNotificationEntity(
                id = "notif-$i",
                timestampMonotonic = random.nextLong(1_000_000L, 10_000_000L),
                amountPaise = random.nextLong(100L, 100000L),
                payee = "Payee-$i",
                matched = random.nextBoolean(),
                active = random.nextBoolean(),
                rawText = null,
                createdAt = 1700000000000L + i
            )
        }

        notifications.forEach { pendingQueueDao.insertNotification(it) }

        val windowStart = 4_000_000L
        val windowEnd = 6_000_000L
        val queryResult = pendingQueueDao.getUnmatchedNotificationsInWindow(windowStart, windowEnd)

        val expected = notifications.filter { n ->
            !n.matched && n.active &&
                    n.timestampMonotonic in windowStart..windowEnd
        }.sortedBy { it.timestampMonotonic }

        assertThat(queryResult.map { it.id }).isEqualTo(expected.map { it.id })
    }

    // ---- getCategorySums correctness test ----

    @Test
    fun `getCategorySums matches manually computed expectations`() = runTest {
        val baseTimestamp = 1700000000000L

        // Insert controlled set of transactions
        data class TestTxn(val category: String?, val amountPaise: Long?, val status: TransactionStatus, val offset: Long)

        val testData = listOf(
            TestTxn("Food", 10000L, TransactionStatus.CONFIRMED, 100L),
            TestTxn("Food", 20000L, TransactionStatus.CONFIRMED, 200L),
            TestTxn("Food", 5000L, TransactionStatus.DISCARDED, 300L),       // excluded (DISCARDED)
            TestTxn("Transport", 15000L, TransactionStatus.CONFIRMED, 400L),
            TestTxn("Transport", 8000L, TransactionStatus.AWAITING_CATEGORY, 500L),
            TestTxn("Shopping", 50000L, TransactionStatus.CONFIRMED, 600L),
            TestTxn(null, 3000L, TransactionStatus.AWAITING_CATEGORY, 700L),
            TestTxn("Food", null, TransactionStatus.AWAITING_AMOUNT, 800L),  // excluded (null amount)
            TestTxn("Food", 7000L, TransactionStatus.CONFIRMED, 50000000L), // outside range
        )

        for (td in testData) {
            transactionDao.insert(
                TransactionEntity(
                    amountPaise = td.amountPaise,
                    payee = null,
                    category = td.category,
                    timestamp = baseTimestamp + td.offset,
                    direction = Direction.OUTFLOW,
                    source = CaptureSource.MANUAL,
                    status = td.status,
                    sourceCaptureId = null,
                    sourceNotificationId = null,
                    confidenceFlag = ConfidenceFlag.CLEAN,
                    createdAt = baseTimestamp
                )
            )
        }

        val rangeStart = baseTimestamp
        val rangeEnd = baseTimestamp + 1000L

        val sums = transactionDao.getCategorySums(rangeStart, rangeEnd)
        val sumMap = sums.associate { it.category to it.total }

        // Expected: Food=10000+20000=30000 (discarded excluded, null-amount excluded, out-of-range excluded)
        // Transport=15000+8000=23000 (AWAITING_CATEGORY is not DISCARDED, so included)
        // Shopping=50000
        // null=3000
        assertThat(sumMap["Food"]).isEqualTo(30000L)
        assertThat(sumMap["Transport"]).isEqualTo(23000L)
        assertThat(sumMap["Shopping"]).isEqualTo(50000L)
        assertThat(sumMap[null]).isEqualTo(3000L)
    }

    // ---- getUncategorizedTotal correctness test ----

    @Test
    fun `getUncategorizedTotal sums only pending status transactions`() = runTest {
        val baseTimestamp = 1700000000000L

        // Insert mix of statuses
        val txns = listOf(
            TransactionEntity(amountPaise = 10000L, payee = null, category = null,
                timestamp = baseTimestamp + 100, direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE, status = TransactionStatus.AWAITING_MATCH,
                sourceCaptureId = null, sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN, createdAt = baseTimestamp),
            TransactionEntity(amountPaise = 20000L, payee = null, category = null,
                timestamp = baseTimestamp + 200, direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE, status = TransactionStatus.AWAITING_CATEGORY,
                sourceCaptureId = null, sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN, createdAt = baseTimestamp),
            TransactionEntity(amountPaise = 5000L, payee = null, category = null,
                timestamp = baseTimestamp + 300, direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE, status = TransactionStatus.AWAITING_AMOUNT,
                sourceCaptureId = null, sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN, createdAt = baseTimestamp),
            TransactionEntity(amountPaise = 99000L, payee = "Shop", category = "Food",
                timestamp = baseTimestamp + 400, direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE, status = TransactionStatus.CONFIRMED,
                sourceCaptureId = null, sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN, createdAt = baseTimestamp),
            TransactionEntity(amountPaise = 8000L, payee = null, category = null,
                timestamp = baseTimestamp + 500, direction = Direction.OUTFLOW,
                source = CaptureSource.SHAKE, status = TransactionStatus.DISCARDED,
                sourceCaptureId = null, sourceNotificationId = null,
                confidenceFlag = ConfidenceFlag.CLEAN, createdAt = baseTimestamp),
        )

        txns.forEach { transactionDao.insert(it) }

        val total = transactionDao.getUncategorizedTotal(baseTimestamp, baseTimestamp + 1000)

        // Only AWAITING_MATCH (10000) + AWAITING_CATEGORY (20000) + AWAITING_AMOUNT (5000) = 35000
        // CONFIRMED and DISCARDED excluded
        assertThat(total).isEqualTo(35000L)
    }

    // ---- getExpiredCaptures test ----

    @Test
    fun `getExpiredCaptures returns only active unmatched captures before cutoff`() = runTest {
        val captures = listOf(
            PendingCaptureEntity("cap-1", 1000L, matched = false, active = true, category = null, createdAt = 0L),
            PendingCaptureEntity("cap-2", 2000L, matched = false, active = true, category = null, createdAt = 0L),
            PendingCaptureEntity("cap-3", 3000L, matched = true, active = false, category = null, createdAt = 0L),  // matched
            PendingCaptureEntity("cap-4", 4000L, matched = false, active = false, category = null, createdAt = 0L), // inactive
            PendingCaptureEntity("cap-5", 5000L, matched = false, active = true, category = null, createdAt = 0L),  // after cutoff
        )

        captures.forEach { pendingQueueDao.insertCapture(it) }

        val expired = pendingQueueDao.getExpiredCaptures(cutoff = 4500L)

        // Only cap-1 and cap-2: active=true, matched=false, timestampMonotonic < 4500
        assertThat(expired.map { it.id }).containsExactly("cap-1", "cap-2")
    }

    // ---- getByStatus test ----

    @Test
    fun `getByStatus returns only transactions with matching status`() = runTest {
        val base = 1700000000000L
        val statuses = TransactionStatus.values()

        statuses.forEachIndexed { i, status ->
            transactionDao.insert(
                TransactionEntity(
                    amountPaise = (i + 1) * 1000L,
                    payee = null, category = null,
                    timestamp = base + i,
                    direction = Direction.OUTFLOW,
                    source = CaptureSource.MANUAL,
                    status = status,
                    sourceCaptureId = null, sourceNotificationId = null,
                    confidenceFlag = ConfidenceFlag.CLEAN,
                    createdAt = base
                )
            )
        }

        val awaitingMatch = transactionDao.getByStatus(TransactionStatus.AWAITING_MATCH)
        assertThat(awaitingMatch).hasSize(1)
        assertThat(awaitingMatch[0].status).isEqualTo(TransactionStatus.AWAITING_MATCH)
    }

    // ---- Housekeeping cleanup test ----

    @Test
    fun `deleteStaleInactiveCaptures removes only old inactive rows`() = runTest {
        val captures = listOf(
            PendingCaptureEntity("cap-old-inactive", 1000L, matched = true, active = false, category = null, createdAt = 100L),
            PendingCaptureEntity("cap-new-inactive", 2000L, matched = false, active = false, category = null, createdAt = 9999L),
            PendingCaptureEntity("cap-old-active", 3000L, matched = false, active = true, category = null, createdAt = 100L),
        )
        captures.forEach { pendingQueueDao.insertCapture(it) }

        pendingQueueDao.deleteStaleInactiveCaptures(olderThan = 5000L)

        // Only cap-old-inactive should be deleted (active=false AND createdAt<5000)
        // cap-new-inactive survives (createdAt=9999 > 5000)
        // cap-old-active survives (active=true)
        val remaining = pendingQueueDao.getUnmatchedCapturesInWindow(0L, 10000L)
        // This query only returns matched=0 AND active=1, so only cap-old-active
        assertThat(remaining.map { it.id }).containsExactly("cap-old-active")
    }
}
