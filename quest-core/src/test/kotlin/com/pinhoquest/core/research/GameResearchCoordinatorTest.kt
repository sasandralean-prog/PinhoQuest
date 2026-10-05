package com.pinhoquest.core.research

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameResearchCoordinatorTest {
    private val sourceA = ResearchProvenance(
        sourceUri = "https://example.test/a",
        researchedAtEpochMillis = 100L,
    )
    private val sourceB = ResearchProvenance(
        sourceUri = "https://example.test/b",
        researchedAtEpochMillis = 200L,
    )

    @Test
    fun combinesSuccessfulProvidersWithoutDiscardingFailures() = runBlocking {
        val coordinator = coordinator(
            listOf(
                GameResearchProvider("a", FixedPort(ResearchOutcome.Success(listOf(discovery("Example", sourceA))))),
                GameResearchProvider("b", FixedPort(ResearchOutcome.RateLimited)),
            ),
        )

        val run = coordinator.research("example")

        assertEquals(listOf("a"), run.successfulProviders)
        assertTrue(run.hasUsableResults)
        assertEquals(1, run.catalog.size)
        assertEquals(500L, run.researchedAtEpochMillis)
        assertTrue(run.providerResults.single { it.providerId == "b" }.outcome is ResearchOutcome.RateLimited)
    }

    @Test
    fun snapshotIsBoundedToTwentyCandidates() = runBlocking {
        val coordinator = coordinator(
            listOf(
                GameResearchProvider(
                    "a",
                    FixedPort(
                        ResearchOutcome.Success(
                            (0 until 30).map { index -> discovery("Game $index", sourceA) },
                        ),
                    ),
                ),
            ),
        )

        val snapshot = coordinator.research("example").snapshot(
            GameCatalogCachePolicy(100L, 1_000L, 500L),
            GameCandidateCycleId("cycle-test"),
        )

        assertEquals(20, snapshot.items.size)
    }

    @Test
    fun duplicateEvidenceIsNormalizedThroughCanonicalCatalog() = runBlocking {
        val coordinator = coordinator(
            listOf(
                GameResearchProvider("a", FixedPort(ResearchOutcome.Success(listOf(discovery("Example", sourceA))))),
                GameResearchProvider("b", FixedPort(ResearchOutcome.Success(listOf(discovery(" example ", sourceB))))),
            ),
        )

        val run = coordinator.research("example")

        assertEquals(1, run.catalog.size)
        assertEquals(setOf(sourceA, sourceB), run.catalog.single().provenance)
    }

    private fun coordinator(providers: List<GameResearchProvider>) =
        GameResearchCoordinator(providers) { 500L }

    private fun discovery(name: String, provenance: ResearchProvenance) = GameDiscovery(
        canonicalName = name,
        platforms = emptySet(),
        genres = emptySet(),
        availability = GameAvailability.UNKNOWN,
        provenance = setOf(provenance),
    )

    private class FixedPort(
        private val outcome: ResearchOutcome<GameDiscovery>,
    ) : GameResearchPort {
        override suspend fun research(query: String, platform: String?) = outcome
    }
}
