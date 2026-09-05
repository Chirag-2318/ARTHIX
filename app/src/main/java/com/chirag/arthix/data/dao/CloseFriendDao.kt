package com.chirag.arthix.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chirag.arthix.data.entity.CloseFriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloseFriendDao {

    @Query("SELECT * FROM close_friends ORDER BY name ASC")
    fun observeAll(): Flow<List<CloseFriendEntity>>

    @Query("SELECT * FROM close_friends ORDER BY name ASC")
    suspend fun getAll(): List<CloseFriendEntity>

    @Query("SELECT * FROM close_friends WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CloseFriendEntity?

    @Query("SELECT * FROM close_friends WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CloseFriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: CloseFriendEntity): Long

    @Update
    suspend fun update(friend: CloseFriendEntity)

    @Query("DELETE FROM close_friends WHERE id = :id")
    suspend fun deleteById(id: Long)
}
