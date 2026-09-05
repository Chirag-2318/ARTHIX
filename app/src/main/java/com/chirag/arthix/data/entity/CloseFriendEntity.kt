package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved Close Friend.
 *
 * Used for:
 * - One-tap quick add in split bill creation
 * - Phonetic and fuzzy voice name resolution
 * - SMS reminder phone number association
 */
@Entity(tableName = "close_friends")
data class CloseFriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phoneNumber: String,
    val aliases: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
