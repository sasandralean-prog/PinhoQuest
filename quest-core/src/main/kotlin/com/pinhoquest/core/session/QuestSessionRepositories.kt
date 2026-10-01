package com.pinhoquest.core.session

import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId

interface QuestRepository {
    suspend fun upsert(quest: Quest)
    suspend fun get(questId: QuestId): Quest?
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
