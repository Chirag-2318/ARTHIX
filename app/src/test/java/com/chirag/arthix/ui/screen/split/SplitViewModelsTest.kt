package com.chirag.arthix.ui.screen.split

import androidx.lifecycle.SavedStateHandle
import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.AmountLock
import com.chirag.arthix.data.model.CaptureSource
import com.chirag.arthix.data.model.ConfidenceFlag
import com.chirag.arthix.data.model.Direction
import com.chirag.arthix.data.model.SplitConfirmedVia
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionEvent
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.report.split.SplitGroupSuggestionHeuristic
import com.chirag.arthix.ui.navigation.ArthixRoute
import com.chirag.arthix.voice.WhisperSttEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SplitViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sttEngine = mock(WhisperSttEngine::class.java)
    private val splitGroupSuggestionHeuristic = mock(SplitGroupSuggestionHeuristic::class.java)

    private lateinit var fakeSplitRepo: FakeSplitRepository
    private lateinit var fakeTxnRepo: FakeTransactionRepository

    class FakeSplitRepository : SplitRepository {
        val splits = mutableListOf<Pair<SplitRecordEntity, List<SplitParticipantEntity>>>()
        var createSplitCount = 0
        var updateSplitCount = 0

        override suspend fun createSplit(split: SplitRecordEntity, participants: List<SplitParticipantEntity>) {
            createSplitCount++
            val id = (splits.size + 1).toLong()
            splits.add(split.copy(id = id) to participants.map { it.copy(splitRecordId = id) })
        }

        override suspend fun getSplitsForTransaction(txnId: Long): List<Pair<SplitRecordEntity, List<SplitParticipantEntity>>> {
            return splits.filter { it.first.transactionId == txnId }
        }

        override suspend fun updateSplit(split: SplitRecordEntity, participants: List<SplitParticipantEntity>) {
            updateSplitCount++
            splits.removeAll { it.first.id == split.id }
            splits.add(split to participants)
        }

        override suspend fun getAllSplits(): List<Pair<SplitRecordEntity, List<SplitParticipantEntity>>> {
            return splits.toList()
        }
    }

    class FakeTransactionRepository : TransactionRepository {
        val txns = mutableMapOf<Long, TransactionEntity>()
        private val _events = MutableSharedFlow<TransactionEvent>()
        override val events: SharedFlow<TransactionEvent> = _events

        suspend fun emitEvent(event: TransactionEvent) {
            _events.emit(event)
        }

        override suspend fun commit(txn: TransactionEntity): Long {
            val id = if (txn.id == 0L) (txns.size + 1).toLong() else txn.id
            val saved = txn.copy(id = id)
            txns[id] = saved
            _events.emit(TransactionEvent.TransactionCommitted(id))
            return id
        }

        override suspend fun update(txn: TransactionEntity) {
            txns[txn.id] = txn
        }

        override suspend fun discard(id: Long) {
            txns.remove(id)
        }

        override suspend fun getById(id: Long): TransactionEntity? = txns[id]

        override fun observeHistory(): Flow<List<TransactionEntity>> = flowOf(txns.values.toList())

        override suspend fun getInRange(start: Long, end: Long): List<TransactionEntity> = txns.values.toList()

        override suspend fun getCategorySums(start: Long, end: Long): List<CategorySum> = emptyList()

        override suspend fun getUncategorizedTotal(start: Long, end: Long): Long = 0L

        override suspend fun hasPendingVoiceRecords(): Boolean = false

        override suspend fun getPendingVoiceRecords(limit: Int): List<TransactionEntity> = emptyList()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSplitRepo = FakeSplitRepository()
        fakeTxnRepo = FakeTransactionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleTransaction(id: Long = 100L, amount: Long = 30000L) = TransactionEntity(
        id = id,
        source = CaptureSource.MANUAL,
        status = TransactionStatus.CONFIRMED,
        direction = Direction.OUTFLOW,
        amountPaise = amount,
        payee = "Cafe Bistro",
        category = "Food & Dining",
        timestamp = 1000L,
        sourceCaptureId = null,
        sourceNotificationId = null,
        confidenceFlag = ConfidenceFlag.CLEAN,
        createdAt = 1000L
    )

    private fun sampleSplit(txnId: Long = 100L, splitId: Long = 1L) = Pair(
        SplitRecordEntity(
            id = splitId,
            transactionId = txnId,
            confirmedVia = SplitConfirmedVia.TAP,
            amountLock = AmountLock.LIVE,
            lockedAmountPaise = null,
            createdAt = 1000L
        ),
        listOf(
            SplitParticipantEntity(
                id = 1L,
                splitRecordId = splitId,
                participantId = "user_1",
                displayName = "You",
                contactId = null,
                isAppUser = true,
                sharePaise = 15000L,
                isPaid = false
            ),
            SplitParticipantEntity(
                id = 2L,
                splitRecordId = splitId,
                participantId = "user_2",
                displayName = "Alex",
                contactId = null,
                isAppUser = false,
                sharePaise = 15000L,
                isPaid = true
            )
        )
    )

    @Test
    fun splitBillViewModel_loadsExistingSplit_preservesPaidStatusAndId() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        val split = sampleSplit()
        fakeTxnRepo.txns[100L] = txn
        fakeSplitRepo.splits.add(split)

        val savedStateHandle = SavedStateHandle(mapOf(ArthixRoute.SplitBill.ARG_TXN_ID to 100L))
        val viewModel = SplitBillViewModel(savedStateHandle, fakeSplitRepo, fakeTxnRepo, sttEngine)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.existingSplitRecordId).isEqualTo(1L)
        assertThat(state.participants).hasSize(2)
        assertThat(state.participants[1].isPaid).isTrue()
    }

    @Test
    fun splitBillViewModel_togglePaidStatus_updatesUiState() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        val split = sampleSplit()
        fakeTxnRepo.txns[100L] = txn
        fakeSplitRepo.splits.add(split)

        val savedStateHandle = SavedStateHandle(mapOf(ArthixRoute.SplitBill.ARG_TXN_ID to 100L))
        val viewModel = SplitBillViewModel(savedStateHandle, fakeSplitRepo, fakeTxnRepo, sttEngine)
        advanceUntilIdle()

        viewModel.togglePaidStatus("user_2")
        assertThat(viewModel.uiState.value.participants[1].isPaid).isFalse()

        viewModel.togglePaidStatus("user_2")
        assertThat(viewModel.uiState.value.participants[1].isPaid).isTrue()
    }

    @Test
    fun splitBillViewModel_confirmExistingSplit_callsUpdateSplitNotCreate() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        val split = sampleSplit()
        fakeTxnRepo.txns[100L] = txn
        fakeSplitRepo.splits.add(split)

        val savedStateHandle = SavedStateHandle(mapOf(ArthixRoute.SplitBill.ARG_TXN_ID to 100L))
        val viewModel = SplitBillViewModel(savedStateHandle, fakeSplitRepo, fakeTxnRepo, sttEngine)
        advanceUntilIdle()

        viewModel.confirmSplit()
        advanceUntilIdle()

        assertThat(fakeSplitRepo.updateSplitCount).isEqualTo(1)
        assertThat(fakeSplitRepo.createSplitCount).isEqualTo(0)
        assertThat(fakeSplitRepo.splits).hasSize(1)
        assertThat(fakeSplitRepo.splits[0].second[1].isPaid).isTrue()
    }

    @Test
    fun splitBillViewModel_applyPrefill_setsAmountPayeeAndParticipants() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf(ArthixRoute.SplitBill.ARG_TXN_ID to 0L))
        val viewModel = SplitBillViewModel(savedStateHandle, fakeSplitRepo, fakeTxnRepo, sttEngine)
        advanceUntilIdle()

        viewModel.applyPrefill(
            SplitPrefill(
                amountPaise = 45000L,
                payee = "Ojas",
                participantNames = listOf("Ojas", "Priya")
            )
        )

        val state = viewModel.uiState.value
        assertThat(state.totalAmountPaise).isEqualTo(45000L)
        assertThat(state.payee).isEqualTo("Ojas")
        assertThat(state.participants).hasSize(3) // You + Ojas + Priya
        assertThat(state.participants[0].sharePaise).isEqualTo(15000L)
        assertThat(state.participants[1].sharePaise).isEqualTo(15000L)
        assertThat(state.participants[2].sharePaise).isEqualTo(15000L)
    }

    @Test
    fun splitBillViewModel_addParticipants_recalculatesEvenShares() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf(ArthixRoute.SplitBill.ARG_TXN_ID to 0L))
        val viewModel = SplitBillViewModel(savedStateHandle, fakeSplitRepo, fakeTxnRepo, sttEngine)
        viewModel.updateAmount(60000L)
        advanceUntilIdle()

        viewModel.addParticipants(listOf("Rohit", "Sneha"))
        val state = viewModel.uiState.value
        assertThat(state.participants).hasSize(3) // You + Rohit + Sneha
        assertThat(state.participants[0].sharePaise).isEqualTo(20000L)
        assertThat(state.participants[1].sharePaise).isEqualTo(20000L)
        assertThat(state.participants[2].sharePaise).isEqualTo(20000L)
    }

    @Test
    fun splitEditViewModel_initForTransaction_preservesIsPaidAndInfersCustom() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        val unevenSplit = Pair(
            SplitRecordEntity(
                id = 2L,
                transactionId = 100L,
                confirmedVia = SplitConfirmedVia.TAP,
                amountLock = AmountLock.LIVE,
                lockedAmountPaise = null,
                createdAt = 1000L
            ),
            listOf(
                SplitParticipantEntity(
                    id = 10L,
                    splitRecordId = 2L,
                    participantId = "user_1",
                    displayName = "You",
                    contactId = null,
                    isAppUser = true,
                    sharePaise = 20000L,
                    isPaid = false
                ),
                SplitParticipantEntity(
                    id = 11L,
                    splitRecordId = 2L,
                    participantId = "user_2",
                    displayName = "Alex",
                    contactId = null,
                    isAppUser = false,
                    sharePaise = 10000L,
                    isPaid = true
                )
            )
        )
        fakeTxnRepo.txns[100L] = txn
        fakeSplitRepo.splits.add(unevenSplit)

        val viewModel = SplitEditViewModel(fakeTxnRepo, fakeSplitRepo, sttEngine)
        viewModel.initForTransaction(100L, null)
        advanceUntilIdle()

        val state = viewModel.state.value as SplitEditState.Active
        assertThat(state.isCustomMode).isTrue()
        assertThat(state.participants).hasSize(2)
        assertThat(state.participants[1].isPaid).isTrue()
        assertThat(state.participants[1].sharePaise).isEqualTo(10000L)
    }

    @Test
    fun splitEditViewModel_updateCustomShare_updatesImmutably() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        fakeTxnRepo.txns[100L] = txn

        val viewModel = SplitEditViewModel(fakeTxnRepo, fakeSplitRepo, sttEngine)
        viewModel.initForTransaction(100L, null)
        advanceUntilIdle()

        viewModel.addParticipant("Sam", null)
        val pId = (viewModel.state.value as SplitEditState.Active).participants[0].participantId
        viewModel.updateCustomShare(pId, "150")
        
        val state = viewModel.state.value as SplitEditState.Active
        assertThat(state.isCustomMode).isTrue()
        assertThat(state.participants[0].sharePaise).isEqualTo(15000L)
    }

    @Test
    fun splitTriggerViewModel_doesNotPrompt_whenSplitAlreadyExists() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        val split = sampleSplit()
        fakeTxnRepo.txns[100L] = txn
        fakeSplitRepo.splits.add(split)

        val viewModel = SplitTriggerViewModel(fakeTxnRepo, fakeSplitRepo, splitGroupSuggestionHeuristic)
        advanceUntilIdle()

        fakeTxnRepo.emitEvent(TransactionEvent.TransactionCommitted(100L))
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(SplitTriggerState.Idle)
    }

    @Test
    fun splitTriggerViewModel_prompts_whenNoSplitExists() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        fakeTxnRepo.txns[100L] = txn

        val viewModel = SplitTriggerViewModel(fakeTxnRepo, fakeSplitRepo, splitGroupSuggestionHeuristic)
        advanceUntilIdle()

        fakeTxnRepo.emitEvent(TransactionEvent.TransactionCommitted(100L))
        advanceUntilIdle()

        assertThat(viewModel.state.value).isInstanceOf(SplitTriggerState.Prompting::class.java)
        val prompting = viewModel.state.value as SplitTriggerState.Prompting
        assertThat(prompting.transactionId).isEqualTo(100L)
    }

    @Test
    fun splitTriggerViewModel_triggerManualPrompt_carriesInitialNames() = runTest(testDispatcher) {
        val txn = sampleTransaction()
        fakeTxnRepo.txns[100L] = txn

        val viewModel = SplitTriggerViewModel(fakeTxnRepo, fakeSplitRepo, splitGroupSuggestionHeuristic)
        viewModel.triggerManualPrompt(100L, listOf("Ojas", "Priya"))
        advanceUntilIdle()

        val prompting = viewModel.state.value as SplitTriggerState.Prompting
        assertThat(prompting.initialParticipantNames).containsExactly("Ojas", "Priya")
    }
}
