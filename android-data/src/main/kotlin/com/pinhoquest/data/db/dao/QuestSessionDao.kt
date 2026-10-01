package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinhoquest.data.db.entity.QuestSessionEntity

@Dao
interface QuestSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuestSessionEntity)

    @Query("SELECT * FROM quest_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun get(sessionId: String): QuestSessionEntity?

    @Query(
        "SELECT * FROM quest_sessions " +
            "WHERE state = 'ACTIVE' " +
            "ORDER BY updatedAtEpochMillis DESC, sessionId DESC LIMIT 1",
    )
    suspend fun active(): QuestSessionEntity?
}
