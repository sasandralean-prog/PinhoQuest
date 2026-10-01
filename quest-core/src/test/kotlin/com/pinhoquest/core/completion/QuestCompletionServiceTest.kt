package com.pinhoquest.core.completion

import com.pinhoquest.core.reward.RewardEngine
import com.pinhoquest.core.reward.UniformIndexSource
import com.pinhoquest.domain.garden.CatalogEntry
import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.CatalogPackId
import com.pinhoquest.domain.garden.FlowerDefinition
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.garden.FlowerInventory
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalRule
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestObjective
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import com.pinhoquest.domain.reward.RewardOpportunityId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestCompletionServiceTest {
    @Test
    fun repeatedCompletionReturnsOriginalWithoutDuplicateCommit() = runTest {
        val store = FakeStore(snapshot())
        val service = service(store)

        val first = service.complete(
            QuestSessionId("session"),
            setOf(ObjectiveId("main"), ObjectiveId("bonus")),
        )
        val second = service.complete(
            QuestSessionId("session"),
            setOf(ObjectiveId("main"), ObjectiveId("bonus")),
        )

        assertTrue(first is CompletionResult.Success)
        assertTrue(second is CompletionResult.Success)
        first as CompletionResult.Success
        second as CompletionResult.Success
        assertEquals(first.receipt.completion.id, second.receipt.completion.id)
        assertEquals(first.receipt.rewardResolution, second.receipt.rewardResolution)
        assertEquals(1, store.commitCount)
        assertTrue(!first.wasAlreadyCompleted)
        assertTrue(second.wasAlreadyCompleted)
    }

    @Test
    fun missingRequiredObjectiveDoesNotCommitAnything() = runTest {
        val store = FakeStore(snapshot())
        val result = service(store).complete(
            QuestSessionId("session"),
            setOf(ObjectiveId("bonus")),
        )

        assertTrue(result is CompletionResult.MissingRequiredObjectives)
        assertEquals(0, store.commitCount)
    }

    @Test
    fun concurrentRevisionConflictReplansAgainstFreshSnapshot() = runTest {
        val store = FakeStore(snapshot(), conflictOnce = true)
        val result = service(store).complete(
            QuestSessionId("session"),
            setOf(ObjectiveId("main")),
        )

        assertTrue(result is CompletionResult.Success)
        assertEquals(2, store.commitAttempts)
        assertEquals(1, store.commitCount)
    }

    private fun service(store: QuestCompletionStore) = QuestCompletionService(
        store = store,
        rewardEngine = RewardEngine(UniformIndexSource { 0 }),
        completionIdFactory = { CompletionId("completion-1") },
        xpTransactionIdFactory = { XpTransactionId("xp-1") },
        rewardOpportunityIdFactory = { RewardOpportunityId("reward-1") },
        nowEpochMillis = { 1_000L },
    )

    private fun snapshot() = CompletionSnapshot(
        profileId = ProfileId("profile"),
        quest = Quest(
            id = QuestId("quest"),
            title = "Quest",
            description = "Desc",
            objectives = listOf(
                QuestObjective(ObjectiveId("main"), "Main", optional = false),
                QuestObjective(ObjectiveId("bonus"), "Bonus", optional = true),
            ),
            category = QuestCategory.CODING,
            environment = QuestEnvironment.WINDOWS,
            estimatedDuration = EstimatedDuration(15, 30),
            difficulty = QuestDifficulty.MEDIUM,
            state = QuestState.ACTIVE,
        ),
        session = QuestSession(
            id = QuestSessionId("session"),
            questId = QuestId("quest"),
            state = QuestState.ACTIVE,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 2L,
        ),
        xpLedger = XpLedgerSnapshot(emptyList()),
        goalDefinitions = listOf(
            GoalDefinition(
                id = com.pinhoquest.domain.progression.GoalId("g1"),
                title = "Primeira",
                rule = GoalRule.CompletionCount(1),
            ),
        ),
        goalProgress = emptyList(),
        catalog = CatalogPack(
            id = CatalogPackId("pack"),
            version = 1,
            generatedAtEpochMillis = 1L,
            entries = listOf(
                CatalogEntry(
                    flower = FlowerDefinition(
                        id = FlowerId("flower"),
                        commonName = "Flor",
                        scientificName = "Species flower",
                        description = "",
                        rarity = FlowerRarity.UNKNOWN,
                        rarityScaleVersion = null,
                        populationEstimate = null,
                    ),
                    eligible = true,
                ),
            ),
            sourceUris = emptySet(),
            integrityHash = "hash",
        ),
        inventory = FlowerInventory(emptySet()),
        datasetRevision = 0L,
    )

    private class FakeStore(
        private var ready: CompletionSnapshot,
        private var conflictOnce: Boolean = false,
    ) : QuestCompletionStore {
        var commitCount = 0
        var commitAttempts = 0
        private var existing: CompletionReceipt? = null

        override suspend fun load(sessionId: QuestSessionId): CompletionLoadResult {
            existing?.let { return CompletionLoadResult.Existing(it) }
            return CompletionLoadResult.Ready(ready)
        }

        override suspend fun commit(plan: CompletionPlan): CompletionCommitResult {
            commitAttempts += 1
            existing?.let { return CompletionCommitResult.Existing(it) }
            if (conflictOnce) {
                conflictOnce = false
                ready = ready.copy(datasetRevision = ready.datasetRevision + 1)
                return CompletionCommitResult.Conflict
            }
            commitCount += 1
            val receipt = CompletionReceipt(
                completion = plan.completion,
                rewardResolution = plan.rewardResolution,
            )
            existing = receipt
            return CompletionCommitResult.Committed(receipt)
        }
    }
}
