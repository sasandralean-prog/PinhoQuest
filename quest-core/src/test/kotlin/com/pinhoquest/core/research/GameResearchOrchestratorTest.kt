package com.pinhoquest.core.research

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameResearchOrchestratorTest {
    private val policy = GameCatalogCachePolicy(100L, 1_000L, 500L)
    private val existing = GameCatalogSnapshot(
        researchedAtEpochMillis = 100L,
        expiresAtEpochMillis = 200L,
        items = listOf(
            GameDiscovery(
                canonicalName = "Cached Game",
                platforms = emptySet(),
                genres = emptySet(),
                availability = GameAvailability.UNKNOWN,
                provenance = setOf(ResearchProvenance("https://example.test/cached", 100L)),
            ),
        ),
        cycleId = GameCandidateCycleId("cycle-existing"),
    )

    @Test fun freshCacheDoesNotResearch() = runBlocking {
        val store = FakeStore(existing)
        val research = FakeResearchCoordinator(success = null)
        val orchestrator = orchestrator(store, research, now = 150L, online = true)

        val result = orchestrator.resolve(GameResearchRequest("anything"))

        assertTrue(result is GameResearchOrchestrationResult.Ready)
        assertSame(existing, (result as GameResearchOrchestrationResult.Ready).snapshot)
        assertEquals(0, research.calls)
    }

    @Test fun staleOnlineResearchCreatesNewCycleAndPersistsOnlySuccessfulSnapshot() = runBlocking {
        val store = FakeStore(existing)
        val research = FakeResearchCoordinator(
            success = GameDiscovery(
                canonicalName = "Fresh Game",
                platforms = setOf("Android"),
                genres = setOf("RPG"),
                availability = GameAvailability.FREE,
                provenance = setOf(ResearchProvenance("https://example.test/fresh", 2_000L)),
            ),
        )
        val orchestrator = orchestrator(store, research, now = 2_000L, online = true)

        val result = orchestrator.resolve(GameResearchRequest("fresh games", "Android"))

        assertTrue(result is GameResearchOrchestrationResult.Ready)
        val snapshot = (result as GameResearchOrchestrationResult.Ready).snapshot
        assertEquals(1, snapshot.items.size)
        assertEquals("Fresh Game", snapshot.items.single().canonicalName)
        assertTrue(snapshot.cycleId != existing.cycleId)
        assertSame(snapshot, store.written)
        assertEquals(1, research.calls)
    }

    @Test fun failedRefreshPreservesExistingSnapshot() = runBlocking {
        val store = FakeStore(existing)
        val orchestrator = orchestrator(
            store,
            FakeResearchCoordinator(success = null),
            now = 2_000L,
            online = true,
        )

        val result = orchestrator.resolve(GameResearchRequest("fresh games"))

        assertTrue(result is GameResearchOrchestrationResult.ServedStaleAfterRefreshFailure)
        assertSame(existing, (result as GameResearchOrchestrationResult.ServedStaleAfterRefreshFailure).snapshot)
        assertEquals(null, store.written)
    }

    @Test fun offlineWithoutCacheHasNoUsableCatalog() = runBlocking {
        val store = FakeStore(null)
        val orchestrator = orchestrator(store, FakeResearchCoordinator(success = null), now = 2_000L, online = false)

        val result = orchestrator.resolve(GameResearchRequest("fresh games"))

        assertEquals(GameResearchOrchestrationResult.NoUsableCatalog, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankQueryIsRejectedAtCanonicalInputBoundary() {
        GameResearchRequest("   ")
    }

    private fun orchestrator(
        store: FakeStore,
        research: FakeResearchCoordinator,
        now: Long,
        online: Boolean,
    ) = GameResearchOrchestrator(
        catalogStore = store,
        researchCoordinator = research.coordinator,
        cachePolicy = policy,
        cycleSequence = GameCandidateCycleSequence(),
        nowEpochMillis = { now },
        online = { online },
    )

    private class FakeResearchCoordinator(
        private val success: GameDiscovery?,
    ) {
        var calls = 0

        val coordinator = GameResearchCoordinator(
            providers = listOf(
                GameResearchProvider("fake", object : GameResearchPort {
                    override suspend fun research(
                        query: String,
                        platform: String?,
                    ): ResearchOutcome<GameDiscovery> {
                        calls++
                        return if (success != null) {
                            ResearchOutcome.Success(listOf(success))
                        } else {
                            ResearchOutcome.TechnicalFailure
                        }
                    }
                }),
            ),
            nowEpochMillis = { 2_000L },
        )
    }

    private class FakeStore(
        private val current: GameCatalogSnapshot?,
    ) : GameDiscoveryCatalogStore {
        var written: GameCatalogSnapshot? = null
        override suspend fun read(): GameCatalogSnapshot? = current
        override suspend fun write(snapshot: GameCatalogSnapshot) {
            written = snapshot
        }
    }
}
