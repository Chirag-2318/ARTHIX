package com.chirag.arthix.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single participant's share within a [SplitRecordEntity].
 *
 * Separate child table (rather than a JSON blob or fixed-width columns)
 * allows variable participant counts and per-person editable shares
 * as required by FR-6.
 */
@Entity(
    tableName = "split_participants",
    indices = [Index(value = ["splitRecordId"])]
)
data class SplitParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitRecordId: Long,                    // soft FK -> SplitRecordEntity.id
    val participantId: String,                  // Phase 6: contactId if from contacts, else generated UUID
    val displayName: String,
    val contactId: String?,                     // null marks an ad hoc participant (EC-37)
    val isAppUser: Boolean,                     // true only for the device owner; conventionally index 0
    val sharePaise: Long,
    val previousSharePaise: Long? = null        // Phase 6: snapshot for before/after badge view
)
