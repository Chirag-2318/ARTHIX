package com.chirag.arthix.data.repository.impl

import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.CategorySum
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.TransactionStatus
import com.chirag.arthix.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.chirag.arthix.data.repository.TransactionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.chirag.arthix.data.model.Direction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room-DAO-backed implementation of [TransactionRepository].
 *
 * Every method delegates directly to [TransactionDao] — no caching layer,
 * no transformation. Room's Flow return types propagate reactively to
 * any observing ViewModel.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    private val _events = MutableSharedFlow<TransactionEvent>()
    override val events: SharedFlow<TransactionEvent> = _events.asSharedFlow()
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun commit(txn: TransactionEntity): Long {
        val id = dao.insert(txn)
        if (txn.status == TransactionStatus.CONFIRMED && txn.direction == Direction.OUTFLOW) {
            scope.launch { _events.emit(TransactionEvent.TransactionCommitted(id)) }
        }
        return id
    }

    override suspend fun update(txn: TransactionEntity) {
        dao.update(txn)
        if (txn.status == TransactionStatus.CONFIRMED && txn.direction == Direction.OUTFLOW) {
            scope.launch { _events.emit(TransactionEvent.TransactionCommitted(txn.id)) }
        }
    }

    override suspend fun discard(id: Long) {
        dao.updateStatusById(id, TransactionStatus.DISCARDED)
    }

    override suspend fun getById(id: Long): TransactionEntity? {
        return dao.getById(id)
    }

    override fun observeHistory(): Flow<List<TransactionEntity>> {
        return dao.observeAllOrderedByTime()
    }

    override suspend fun getInRange(start: Long, end: Long): List<TransactionEntity> {
        return dao.getInRange(start, end)
    }

    override suspend fun getCategorySums(start: Long, end: Long): List<CategorySum> {
        return dao.getCategorySums(start, end)
    }

    override suspend fun getUncategorizedTotal(start: Long, end: Long): Long {
        return dao.getUncategorizedTotal(start, end)
    }

    override suspend fun hasPendingVoiceRecords(): Boolean {
        return dao.countPendingVoiceRecords() > 0
    }

    override suspend fun getPendingVoiceRecords(limit: Int): List<TransactionEntity> {
        return dao.getPendingVoiceRecords(limit)
    }
}
