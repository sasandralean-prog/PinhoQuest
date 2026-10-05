package com.pinhoquest.core.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDiscoveryCatalogTest {
    private val sourceA = ResearchProvenance(
        sourceUri = "https://example.test/a",
        researchedAtEpochMillis = 100L,
    )
    private val sourceB = ResearchProvenance(
        sourceUri = "https://example.test/b",
        researchedAtEpochMillis = 200L,
    )

    @Test
    fun semanticFailureStatesAreDistinctFromSuccess() {
        assertNotSame(ResearchOutcome.Unavailable, ResearchOutcome.TechnicalFailure)
        assertNotSame(ResearchOutcome.RateLimited, ResearchOutcome.InvalidResponse)
    }

    @Test
    fun catalogNormalizesIdentityAndMergesDuplicateEvidence() {
        val catalog = GameDiscoveryCatalog()

        catalog.merge(
            listOf(
                GameDiscovery(
                    canonicalName = "  Stardew   Valley ",
                    platforms = setOf("Android"),
                    genres = setOf("RPG"),
                    availability = GameAvailability.UNKNOWN,
                    provenance = setOf(sourceA),
                ),
                GameDiscovery(
                    canonicalName = "stardew valley",
                    platforms = setOf("PC"),
                    genres = setOf("Simulation"),
                    availability = GameAvailability.FREE_TO_PLAY,
                    provenance = setOf(sourceB),
                ),
            ),
        )

        val result = catalog.snapshot()
        assertEquals(1, result.size)
        assertEquals(setOf("Android", "PC"), result.single().platforms)
        assertEquals(setOf("RPG", "Simulation"), result.single().genres)
        assertEquals(GameAvailability.FREE_TO_PLAY, result.single().availability)
        assertEquals(setOf(sourceA, sourceB), result.single().provenance)
    }

    @Test
    fun conflictingAvailabilityEvidenceRemainsUnknown() {
        val catalog = GameDiscoveryCatalog()
        catalog.merge(
            listOf(
                GameDiscovery(
                    canonicalName = "Example",
                    platforms = emptySet(),
                    genres = emptySet(),
                    availability = GameAvailability.FREE,
                    provenance = setOf(sourceA),
                ),
                GameDiscovery(
                    canonicalName = " example ",
                    platforms = emptySet(),
                    genres = emptySet(),
                    availability = GameAvailability.FREE_TO_PLAY,
                    provenance = setOf(sourceB),
                ),
            ),
        )

        assertEquals(GameAvailability.UNKNOWN, catalog.snapshot().single().availability)
    }

    @Test
    fun emptyCatalogRemainsAValidSemanticNoNewOptionsState() {
        val catalog = GameDiscoveryCatalog()
        assertTrue(catalog.isEmpty())
        assertEquals(emptyList<GameDiscovery>(), catalog.snapshot())
    }
}
