package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_sessions",
    indices = [Index("state"), Index("questId")],
)
data class QuestSessionEntity(
    @PrimaryKey val sessionId: String,
    val questId: String,
    val state: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
