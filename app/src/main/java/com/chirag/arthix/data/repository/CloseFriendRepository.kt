package com.chirag.arthix.data.repository

import com.chirag.arthix.data.entity.CloseFriendEntity
import kotlinx.coroutines.flow.Flow

interface CloseFriendRepository {
    fun observeAll(): Flow<List<CloseFriendEntity>>
    suspend fun getAll(): List<CloseFriendEntity>
    suspend fun getById(id: Long): CloseFriendEntity?
    suspend fun findByName(name: String): CloseFriendEntity?
    suspend fun create(friend: CloseFriendEntity): Long
    suspend fun update(friend: CloseFriendEntity)
    suspend fun delete(id: Long)
}
