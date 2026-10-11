package com.pinhoquest.core.session

import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState

interface QuestRepository {
    suspend fun upsert(quest: Quest)
    suspend fun get(questId: QuestId): Quest?

    /**
     * Updates state without rewriting the generated quest record when the backing store supports it.
     * This keeps generated-history ordering independent from accept/start/abandon/reject transitions.
     */
    suspend fun updateState(questId: QuestId, state: QuestState) {
        get(questId)?.let { existing -> upsert(existing.copy(state = state)) }
    }

    /**
     * Recent categories from persisted quest history, newest generated records first.
     * The default preserves compatibility for in-memory/test repositories without history.
     */
    suspend fun recentCategories(limit: Int = 12): List<QuestCategory> = emptyList()
}

interface QuestSessionRepository {
    suspend fun upsert(session: QuestSession)
    suspend fun get(sessionId: QuestSessionId): QuestSession?
    suspend fun getByQuestId(questId: QuestId): QuestSession?
    suspend fun active(): QuestSession?
}

fun interface QuestContextProvider {
    suspend fun contextFor(request: QuestRequest): QuestContext
}
