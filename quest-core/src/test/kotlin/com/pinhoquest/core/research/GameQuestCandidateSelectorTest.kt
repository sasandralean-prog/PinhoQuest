package com.pinhoquest.core.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameQuestCandidateSelectorTest {
    private val policy = GameCatalogCachePolicy(100L, 1_000L, 500L, maxCandidates = 2)
    private val provenance = setOf(ResearchProvenance("https://example.test", 1_000L))

    private fun game(name: String, at: Long = 1_000L) = GameDiscovery(
        canonicalName = name,
        platforms = setOf("PC"),
        genres = setOf("RPG"),
        availability = GameAvailability.FREE,
        provenance = setOf(ResearchProvenance("https://example.test/$name", at)),
    )

    private fun snapshot(items: List<GameDiscovery>) = GameCatalogSnapshot(
        researchedAtEpochMillis = 1_000L,
        expiresAtEpochMillis = 2_000L,
        items = items,
        cycleId = GameCandidateCycleId("cycle-1"),
    )

    @Test fun selectedContextCarriesCanonicalIdentityAndCycle() {
        val context = GameQuestCandidateSelector(policy).select(
            snapshot(listOf(game("Minecraft"))),
            emptySet(),
        )
        assertNotNull(context)
        assertEquals("minecraft", context!!.gameIdentity.value)
        assertEquals("cycle-1", context.cycleId.value)
    }

    @Test fun usedIdentityCannotBeSelectedAgainInSameCycle() {
        val context = GameQuestCandidateSelector(policy).select(
            snapshot(listOf(game("Minecraft"), game("Celeste"))),
            setOf(GameIdentityKey("minecraft")),
        )
        assertEquals("celeste", context?.gameIdentity?.value)
    }

    @Test fun exhaustedCycleHasNoCandidate() {
        val context = GameQuestCandidateSelector(policy).select(
            snapshot(listOf(game("Minecraft"))),
            setOf(GameIdentityKey("minecraft")),
        )
        assertNull(context)
    }

    @Test fun usageRefreshIsRequestedWhenPoolHasTooFewUnusedCandidates() {
        val selector = GameQuestCandidateSelector(policy)
        assertTrue(
            selector.needsUsageRefresh(
                snapshot(listOf(game("Minecraft"), game("Celeste"))),
                setOf(GameIdentityKey("minecraft")),
            ),
        )
    }

    @Test fun newCycleCanReintroduceSameGame() {
        val context = GameQuestCandidateSelector(policy).select(
            snapshot(
                listOf(game("Minecraft")),
            ).copy(cycleId = GameCandidateCycleId("cycle-2")),
            emptySet(),
        )
        assertEquals("minecraft", context?.gameIdentity?.value)
    }
}
