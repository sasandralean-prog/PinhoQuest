package com.pinhoquest.core.session

import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestGenerationResult
import com.pinhoquest.core.research.GameQuestCandidateResult
import com.pinhoquest.core.research.GameQuestGenerationCoordinator
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val gameQuestGenerationCoordinator: GameQuestGenerationCoordinator? = null,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val sessionIdFactory: QuestSessionIdFactory = QuestSessionIdFactory {
        QuestSessionId(UUID.randomUUID().toString())
    },
) {
    private val commandMutex = Mutex()

    suspend fun generate(request: QuestRequest): SessionCommandResult<Quest> {
        val selectedGameContext = if (request.mode == QuestMode.GAME) {
            when (val selection = gameQuestGenerationCoordinator?.selectFreshRandomCandidate()) {
                null -> return SessionCommandResult.GenerationFailed(
                    QuestGenerationResult.Unavailable(
                        com.pinhoquest.core.quest.GenerationUnavailableReason.GAME_CATALOG_NOT_CONFIGURED,
                    ),
                )

                is GameQuestCandidateResult.Selected -> selection.context
                is GameQuestCandidateResult.NoCatalog ->
                    return generationUnavailable(
                        com.pinhoquest.core.quest.GenerationUnavailableReason.GAME_CATALOG_UNAVAILABLE,
                    )
                is GameQuestCandidateResult.StaleCatalog ->
                    return generationUnavailable(
                        com.pinhoquest.core.quest.GenerationUnavailableReason.GAME_CATALOG_NOT_FRESH,
                    )
                is GameQuestCandidateResult.CycleExhausted ->
                    return generationUnavailable(
                        com.pinhoquest.core.quest.GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED,
                    )
            }
        } else {
            null
        }

        val baseContext = contextProvider.contextFor(request)
        val context = selectedGameContext?.let { base ->
            baseContext.copy(
                gameCandidate = com.pinhoquest.core.quest.GameQuestSeed.from(base),
            )
        } ?: baseContext

        return when (
            val result = engine.generate(
                request = request,
                context = context,
            )
        ) {
            is QuestGenerationResult.Success -> {
                // The generation event is the only operation that inserts a quest into history.
                questRepository.upsert(result.quest)
                selectedGameContext?.variant?.let { variant ->
                    gameQuestGenerationCoordinator?.recordUsage(
                        context = selectedGameContext,
                        variant = variant,
                        usedAtEpochMillis = nowEpochMillis(),
                    )
                }
                SessionCommandResult.Success(result.quest)
            }
            else -> SessionCommandResult.GenerationFailed(result)
        }
    }

    private fun generationUnavailable(
        reason: com.pinhoquest.core.quest.GenerationUnavailableReason,
    ): SessionCommandResult<Quest> =
        SessionCommandResult.GenerationFailed(QuestGenerationResult.Unavailable(reason))

    suspend fun accept(questId: QuestId): SessionCommandResult<QuestSession> =
        commandMutex.withLock {
            val quest = questRepository.get(questId)
                ?: return@withLock SessionCommandResult.NotFound("quest", questId.value)
            sessionRepository.getByQuestId(questId)?.let { existing ->
                if (existing.state == QuestState.ACCEPTED || existing.state == QuestState.ACTIVE) {
                    return@withLock SessionCommandResult.Success(existing)
                }
            }
            if (quest.state != QuestState.GENERATED) {
                return@withLock SessionCommandResult.InvalidTransition(
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
            questRepository.updateState(questId, QuestState.ACCEPTED)
            sessionRepository.upsert(session)
            SessionCommandResult.Success(session)
        }

    suspend fun start(sessionId: QuestSessionId): SessionCommandResult<QuestSession> =
        commandMutex.withLock {
            transition(
                sessionId = sessionId,
                target = QuestState.ACTIVE,
                allowedFrom = setOf(QuestState.ACCEPTED),
            )
        }

    suspend fun abandon(sessionId: QuestSessionId): SessionCommandResult<QuestSession> =
        commandMutex.withLock {
            transition(
                sessionId = sessionId,
                target = QuestState.ABANDONED,
                allowedFrom = setOf(QuestState.ACCEPTED, QuestState.ACTIVE),
            )
        }

    suspend fun reject(
        questId: QuestId,
        reason: RejectReason? = null,
    ): SessionCommandResult<Unit> = commandMutex.withLock {
        @Suppress("UNUSED_VARIABLE")
        val explicitReason = reason
        val quest = questRepository.get(questId)
            ?: return@withLock SessionCommandResult.NotFound("quest", questId.value)
        if (quest.state == QuestState.REJECTED) {
            return@withLock SessionCommandResult.Success(Unit)
        }
        if (quest.state != QuestState.GENERATED) {
            return@withLock SessionCommandResult.InvalidTransition(
                current = quest.state,
                requested = QuestState.REJECTED,
            )
        }
        questRepository.updateState(questId, QuestState.REJECTED)
        SessionCommandResult.Success(Unit)
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
        questRepository.updateState(current.questId, target)
        return SessionCommandResult.Success(updated)
    }
}
