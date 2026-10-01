package com.pinhoquest.core.session

import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestGenerationResult
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import java.util.UUID

enum class RejectReason {
    NOT_FOR_ME,
    REPEATED,
    OTHER,
}

sealed interface SessionCommandResult<out T> {
    data class Success<T>(val value: T) : SessionCommandResult<T>
    data class NotFound(val entity: String, val id: String) : SessionCommandResult<Nothing>
    data class InvalidTransition(
        val current: QuestState,
        val requested: QuestState,
    ) : SessionCommandResult<Nothing>
    data class GenerationFailed(
        val result: QuestGenerationResult,
    ) : SessionCommandResult<Nothing>
}

fun interface QuestSessionIdFactory {
    fun newId(): QuestSessionId
}

class QuestSessionService(
    private val engine: QuestEngine,
    private val questRepository: QuestRepository,
    private val sessionRepository: QuestSessionRepository,
    private val contextProvider: QuestContextProvider,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val sessionIdFactory: QuestSessionIdFactory = QuestSessionIdFactory {
        QuestSessionId(UUID.randomUUID().toString())
    },
) {
    suspend fun generate(request: QuestRequest): SessionCommandResult<Quest> {
        return when (
            val result = engine.generate(
                request = request,
                context = contextProvider.contextFor(request),
            )
        ) {
            is QuestGenerationResult.Success -> {
                questRepository.upsert(result.quest)
                SessionCommandResult.Success(result.quest)
            }
            else -> SessionCommandResult.GenerationFailed(result)
        }
    }

    suspend fun accept(questId: QuestId): SessionCommandResult<QuestSession> {
        val quest = questRepository.get(questId)
            ?: return SessionCommandResult.NotFound("quest", questId.value)
        sessionRepository.getByQuestId(questId)?.let { existing ->
            if (existing.state == QuestState.ACCEPTED || existing.state == QuestState.ACTIVE) {
                return SessionCommandResult.Success(existing)
            }
        }
        if (quest.state != QuestState.GENERATED) {
            return SessionCommandResult.InvalidTransition(
                current = quest.state,
                requested = QuestState.ACCEPTED,
            )
        }

        val now = nowEpochMillis()
        val session = QuestSession(
            id = sessionIdFactory.newId(),
            questId = questId,
            state = QuestState.ACCEPTED,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        questRepository.upsert(quest.copy(state = QuestState.ACCEPTED))
        sessionRepository.upsert(session)
        return SessionCommandResult.Success(session)
    }

    suspend fun start(sessionId: QuestSessionId): SessionCommandResult<QuestSession> =
        transition(
            sessionId = sessionId,
            target = QuestState.ACTIVE,
            allowedFrom = setOf(QuestState.ACCEPTED),
        )

    suspend fun abandon(sessionId: QuestSessionId): SessionCommandResult<QuestSession> =
        transition(
            sessionId = sessionId,
            target = QuestState.ABANDONED,
            allowedFrom = setOf(QuestState.ACCEPTED, QuestState.ACTIVE),
        )
    suspend fun reject(
        questId: QuestId,
        reason: RejectReason? = null,
    ): SessionCommandResult<Unit> {
        @Suppress("UNUSED_VARIABLE")
        val explicitReason = reason
        val quest = questRepository.get(questId)
            ?: return SessionCommandResult.NotFound("quest", questId.value)
        if (quest.state == QuestState.REJECTED) {
            return SessionCommandResult.Success(Unit)
        }
        if (quest.state != QuestState.GENERATED) {
            return SessionCommandResult.InvalidTransition(
                current = quest.state,
                requested = QuestState.REJECTED,
            )
        }
        questRepository.upsert(quest.copy(state = QuestState.REJECTED))
        return SessionCommandResult.Success(Unit)
    }

    private suspend fun transition(
        sessionId: QuestSessionId,
        target: QuestState,
        allowedFrom: Set<QuestState>,
    ): SessionCommandResult<QuestSession> {
        val current = sessionRepository.get(sessionId)
            ?: return SessionCommandResult.NotFound("questSession", sessionId.value)
        if (current.state == target) {
            return SessionCommandResult.Success(current)
        }
        if (current.state !in allowedFrom) {
            return SessionCommandResult.InvalidTransition(
                current = current.state,
                requested = target,
            )
        }

        val updated = current.copy(
            state = target,
            updatedAtEpochMillis = nowEpochMillis(),
        )
        sessionRepository.upsert(updated)
        questRepository.get(current.questId)?.let { quest ->
            questRepository.upsert(quest.copy(state = target))
        }
        return SessionCommandResult.Success(updated)
    }
}
