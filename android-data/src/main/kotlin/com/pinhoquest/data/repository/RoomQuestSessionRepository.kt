package com.pinhoquest.data.repository

import com.pinhoquest.data.db.dao.QuestSessionDao
import com.pinhoquest.data.db.entity.QuestSessionEntity
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState

class RoomQuestSessionRepository(
    private val dao: QuestSessionDao,
) {
    suspend fun upsert(session: QuestSession) {
        dao.upsert(session.toEntity())
    }

    suspend fun get(sessionId: QuestSessionId): QuestSession? =
        dao.get(sessionId.value)?.toDomain()

    suspend fun active(): QuestSession? =
        dao.active()?.toDomain()

    private fun QuestSession.toEntity() = QuestSessionEntity(
        sessionId = id.value,
        questId = questId.value,
        state = state.name,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    private fun QuestSessionEntity.toDomain() = QuestSession(
        id = QuestSessionId(sessionId),
        questId = QuestId(questId),
        state = QuestState.valueOf(state),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}
