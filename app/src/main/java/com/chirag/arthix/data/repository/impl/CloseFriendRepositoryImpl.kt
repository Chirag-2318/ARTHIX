package com.chirag.arthix.data.repository.impl

import com.chirag.arthix.data.dao.CloseFriendDao
import com.chirag.arthix.data.entity.CloseFriendEntity
import com.chirag.arthix.data.repository.CloseFriendRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloseFriendRepositoryImpl @Inject constructor(
    private val closeFriendDao: CloseFriendDao
) : CloseFriendRepository {

    override fun observeAll(): Flow<List<CloseFriendEntity>> = closeFriendDao.observeAll()

    override suspend fun getAll(): List<CloseFriendEntity> = closeFriendDao.getAll()

    override suspend fun getById(id: Long): CloseFriendEntity? = closeFriendDao.getById(id)

    override suspend fun findByName(name: String): CloseFriendEntity? = closeFriendDao.findByName(name)

    override suspend fun create(friend: CloseFriendEntity): Long = closeFriendDao.insert(friend)

    override suspend fun update(friend: CloseFriendEntity) = closeFriendDao.update(friend)

    override suspend fun delete(id: Long) = closeFriendDao.deleteById(id)
}
