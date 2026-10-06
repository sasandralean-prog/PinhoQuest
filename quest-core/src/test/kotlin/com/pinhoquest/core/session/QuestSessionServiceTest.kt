package com.pinhoquest.core.session

import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestSessionServiceTest {
    private val questRepository = FakeQuestRepository()
    private val sessionRepository = FakeQuestSessionRepository()
    private var now = 1_000L
    private val service
        get() = QuestSessionService(
            engine = QuestEngine(QuestPlanner(), ProceduralComposer(), QuestValidator()),
            questRepository = questRepository,
            sessionRepository = sessionRepository,
            contextProvider = QuestContextProvider { QuestContext() },
            nowEpochMillis = { now },
        )

    @Test
    fun concurrentAcceptsSerializeToOneCanonicalSession() = runTest {
        val slowSessions = FakeQuestSessionRepository(delayQuestLookup = true)
        var nextSessionId = 0
        val concurrentService = QuestSessionService(
            engine = QuestEngine(QuestPlanner(), ProceduralComposer(), QuestValidator()),
            questRepository = questRepository,
            sessionRepository = slowSessions,
            contextProvider = QuestContextProvider { QuestContext() },
            nowEpochMillis = { now },
            sessionIdFactory = QuestSessionIdFactory {
                nextSessionId += 1
                QuestSessionId("session-" + nextSessionId)
            },
        )
        val quest = success(concurrentService.generate(QuestRequest(QuestMode.NORMAL)))

        val accepted = listOf(
            async { concurrentService.accept(quest.id) },
            async { concurrentService.accept(quest.id) },
        ).awaitAll().map(::success)

        assertEquals(1, slowSessions.maxConcurrentQuestLookups)
        assertEquals(1, accepted.map { it.id }.distinct().size)
        assertEquals(accepted.first(), slowSessions.getByQuestId(quest.id))
    }

    @Test
    fun duplicateStartIsIdempotent() = runTest {
        val quest = generatedQuest()
        val accepted = success(service.accept(quest.id))
        now = 2_000L
        val first = success(service.start(accepted.id))
        now = 3_000L
        val second = success(service.start(accepted.id))

        assertEquals(QuestState.ACTIVE, first.state)
        assertEquals(first, second)
        assertEquals(2_000L, second.updatedAtEpochMillis)
    }

    @Test
    fun duplicateAbandonIsIdempotent() = runTest {
        val quest = generatedQuest()
        val accepted = success(service.accept(quest.id))
        val active = success(service.start(accepted.id))
        now = 4_000L
        val first = success(service.abandon(active.id))
        now = 5_000L
        val second = success(service.abandon(active.id))

        assertEquals(QuestState.ABANDONED, first.state)
        assertEquals(first, second)
        assertEquals(4_000L, second.updatedAtEpochMillis)
    }

    @Test
    fun serviceReloadReadsCanonicalRepositoryState() = runTest {
        val quest = generatedQuest()
        val accepted = success(service.accept(quest.id))

        val reloadedService = service
        now = 6_000L
        val active = success(reloadedService.start(accepted.id))

        assertEquals(QuestState.ACTIVE, active.state)
        assertEquals(active, sessionRepository.get(accepted.id))
    }

    @Test
    fun illegalRegressionReturnsSemanticFailureWithoutMutation() = runTest {
        val quest = generatedQuest()
        val accepted = success(service.accept(quest.id))
        val active = success(service.start(accepted.id))
        val abandoned = success(service.abandon(active.id))

        val result = service.start(abandoned.id)

        assertTrue(result is SessionCommandResult.InvalidTransition)
        assertSame(abandoned, sessionRepository.get(abandoned.id))
    }

    @Test
    fun rejectPersistsRejectedQuest() = runTest {
        val quest = generatedQuest()

        val result = service.reject(quest.id, RejectReason.NOT_FOR_ME)

        assertTrue(result is SessionCommandResult.Success)
        assertEquals(QuestState.REJECTED, questRepository.get(quest.id)?.state)
    }
    private suspend fun generatedQuest(): Quest {
        val generated = service.generate(QuestRequest(QuestMode.NORMAL))
        val quest = success(generated)
        assertEquals(QuestState.GENERATED, quest.state)
        assertEquals(quest, questRepository.get(quest.id))
        return quest
    }

    private fun <T> success(result: SessionCommandResult<T>): T {
        assertTrue(result is SessionCommandResult.Success)
        return (result as SessionCommandResult.Success).value
    }

    private class FakeQuestRepository : QuestRepository {
        private val values = linkedMapOf<QuestId, Quest>()
        override suspend fun upsert(quest: Quest) {
            values[quest.id] = quest
        }

        override suspend fun get(questId: QuestId): Quest? = values[questId]
    }

    private class FakeQuestSessionRepository(
        private val delayQuestLookup: Boolean = false,
    ) : QuestSessionRepository {
        private val values = linkedMapOf<QuestSessionId, QuestSession>()
        private var activeQuestLookups = 0
        var maxConcurrentQuestLookups: Int = 0
            private set
        override suspend fun upsert(session: QuestSession) {
            values[session.id] = session
        }

        override suspend fun get(sessionId: QuestSessionId): QuestSession? = values[sessionId]

        override suspend fun getByQuestId(questId: QuestId): QuestSession? {
            val snapshot = values.values.firstOrNull { it.questId == questId }
            if (!delayQuestLookup) return snapshot
            activeQuestLookups += 1
            maxConcurrentQuestLookups = maxOf(maxConcurrentQuestLookups, activeQuestLookups)
            try {
                delay(1)
            } finally {
                activeQuestLookups -= 1
            }
            return snapshot
        }

        override suspend fun active(): QuestSession? =
            values.values.firstOrNull { it.state == QuestState.ACTIVE }
    }
}
