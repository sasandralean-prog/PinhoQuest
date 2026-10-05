package com.pinhoquest.core.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class GameQuestApplicationCoordinatorTest {
    private val policy = GameCatalogCachePolicy(100L, 1_000L, 500L, maxCandidates = 2)

    @Test
    fun freshCatalogReachesCandidateSelection() = runBlocking {
        val game = game("Pinho Quest")
        val cycle = GameCandidateCycleId("cycle-1")
        val snapshot = GameCatalogSnapshot(
            researchedAtEpochMillis = 1_000L,
            expiresAtEpochMillis = 2_000L,
            items = listOf(game),
            cycleId = cycle,
        )
        val store = FakeStore(snapshot)
        val orchestrator = GameResearchOrchestrator(
            catalogStore = store,
            researchCoordinator = coordinator(),
            cachePolicy = policy,
            cycleSequence = GameCandidateCycleSequence(),
            nowEpochMillis = { 1_500L },
            online = { true },
        )
        val generation = GameQuestGenerationCoordinator(
            catalogStore = store,
            usageStore = FakeUsageStore(),
            candidateSelector = GameQuestCandidateSelector(policy),
        )

        val result = GameQuestApplicationCoordinator(orchestrator, generation)
            .prepare(GameResearchRequest("explicit query", "Android"))

        assertTrue(result is GameQuestApplicationResult.Selected)
        assertEquals("Pinho Quest", (result as GameQuestApplicationResult.Selected).seed.title)
    }

    @Test
    fun failedRefreshPreservesStaleCatalogForCandidateSelection() = runBlocking {
        val game = game("Cached Game")
        val cycle = GameCandidateCycleId("cycle-cached")
        val snapshot = GameCatalogSnapshot(1_000L, 1_100L, listOf(game), cycle)
        val store = FakeStore(snapshot)
        val orchestrator = GameResearchOrchestrator(
            catalogStore = store,
            researchCoordinator = coordinator(success = null),
            cachePolicy = policy,
            cycleSequence = GameCandidateCycleSequence(),
            nowEpochMillis = { 2_000L },
            online = { true },
        )
        val generation = GameQuestGenerationCoordinator(
            catalogStore = store,
            usageStore = FakeUsageStore(),
            candidateSelector = GameQuestCandidateSelector(policy),
        )

        val result = GameQuestApplicationCoordinator(orchestrator, generation)
            .prepare(GameResearchRequest("explicit query"))

        assertTrue(result is GameQuestApplicationResult.Selected)
        assertEquals("Cached Game", (result as GameQuestApplicationResult.Selected).seed.title)
    }

    @Test
    fun noCatalogRemainsUnavailable() = runBlocking {
        val store = FakeStore(null)
        val orchestrator = GameResearchOrchestrator(
            catalogStore = store,
            researchCoordinator = coordinator(success = null),
            cachePolicy = policy,
            cycleSequence = GameCandidateCycleSequence(),
            nowEpochMillis = { 2_000L },
            online = { false },
        )
        val generation = GameQuestGenerationCoordinator(
            catalogStore = store,
            usageStore = FakeUsageStore(),
            candidateSelector = GameQuestCandidateSelector(policy),
        )

        val result = GameQuestApplicationCoordinator(orchestrator, generation)
            .prepare(GameResearchRequest("explicit query"))

        assertEquals(GameQuestApplicationResult.NoUsableCatalog, result)
    }

    private fun game(name: String) = GameDiscovery(
        canonicalName = name,
        platforms = setOf("Android"),
        genres = setOf("Adventure"),
        availability = GameAvailability.UNKNOWN,
        provenance = setOf(ResearchProvenance("https://example.test/$name", 1_000L)),
    )

    private fun coordinator(success: GameDiscovery? = game("Fresh Game")) =
        GameResearchCoordinator(
            providers = listOf(
                GameResearchProvider("fake", object : GameResearchPort {
                    override suspend fun research(
                        query: String,
                        platform: String?,
                    ): ResearchOutcome<GameDiscovery> =
                        success?.let { ResearchOutcome.Success(listOf(it)) }
                            ?: ResearchOutcome.TechnicalFailure
                }),
            ),
            nowEpochMillis = { 2_000L },
        )

    private class FakeStore(
        private var snapshot: GameCatalogSnapshot?,
    ) : GameDiscoveryCatalogStore {
        override suspend fun read(): GameCatalogSnapshot? = snapshot
        override suspend fun write(snapshot: GameCatalogSnapshot) {
            this.snapshot = snapshot
        }
    }

    private class FakeUsageStore : GameQuestUsageStore {
        override suspend fun usedIdentityKeys(cycleId: GameCandidateCycleId): Set<GameIdentityKey> = emptySet()
        override suspend fun record(usage: GameQuestUsage) = Unit
        override suspend fun clearCycle(cycleId: GameCandidateCycleId) = Unit
    }
}
