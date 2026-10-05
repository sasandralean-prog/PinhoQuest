package com.pinhoquest.core.research

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogCachePolicyTest {
    private val policy = GameCatalogCachePolicy(
        minimumAgeMillis = 100L,
        maximumAgeMillis = 1_000L,
        referenceAgeMillis = 500L,
    )

    @Test
    fun successfulBroadResearchReceivesLongerAdaptiveFreshnessWindow() {
        val sparse = policy.effectiveMaxAgeMillis(GameCatalogCacheSignals(1, 2, 0))
        val rich = policy.effectiveMaxAgeMillis(GameCatalogCacheSignals(2, 2, 20))
        assertTrue(rich > sparse)
        assertTrue(rich <= 1_000L)
        assertTrue(sparse >= 100L)
    }

    @Test
    fun heavilyUsedCatalogReceivesShorterFreshnessWindow() {
        val fresh = policy.effectiveMaxAgeMillis(GameCatalogCacheSignals(2, 2, 20, 0))
        val used = policy.effectiveMaxAgeMillis(GameCatalogCacheSignals(2, 2, 20, 18))
        assertTrue(used < fresh)
    }

    @Test
    fun usedGamesAreExcludedFromCandidateRotation() {
        val items = (0 until 4).map { game(it, researchedAt = 100L + it) }
        val selected = policy.selectCandidates(items, setOf("game 1", "game 3"))
        assertEquals(listOf("game 2", "game 0"), selected.map { it.identityKey })
    }

    @Test
    fun candidateRotationIsBoundedToTwenty() {
        val items = (0 until 30).map { game(it, researchedAt = 100L + it) }
        val selected = policy.selectCandidates(items, emptySet())
        assertEquals(20, selected.size)
    }

    @Test
    fun fewerThanTwentyUnusedCandidatesRequestUsageRefresh() {
        val items = (0 until 20).map { game(it, researchedAt = 100L + it) }
        val used = items.take(5).map { it.identityKey }.toSet()
        assertTrue(policy.shouldRefreshForUsage(items, used))
    }

    @Test
    fun twentyUnusedCandidatesDoNotRequestUsageRefresh() {
        val items = (0 until 20).map { game(it, researchedAt = 100L + it) }
        assertFalse(policy.shouldRefreshForUsage(items, emptySet()))
    }

    @Test
    fun candidateOrderIsDeterministic() {
        val items = listOf(
            game(2, 200L),
            game(1, 200L),
            game(3, 300L),
        )
        val selected = policy.selectCandidates(items, emptySet())
        assertEquals(listOf("game 3", "game 1", "game 2"), selected.map { it.identityKey })
    }

    @Test
    fun cacheDecisionPreservesStaleCatalogOffline() {
        val snapshot = snapshot(itemCount = 3, researchedAt = 100L)
        val decision = policy.decision(snapshot, 2_000L, online = false)
        assertEquals(GameCatalogCacheDecision.ServeStale, decision)
    }

    @Test
    fun cacheDecisionRequestsRefreshWhenOnlineAndStale() {
        val snapshot = snapshot(itemCount = 3, researchedAt = 100L)
        val decision = policy.decision(snapshot, 2_000L, online = true)
        assertEquals(GameCatalogCacheDecision.RefreshRequired, decision)
    }

    @Test
    fun missingCacheIsDifferentFromEmptySuccessfulCatalog() {
        val signals = GameCatalogCacheSignals(1, 1, 0)
        assertEquals(GameCatalogCacheDecision.NoCache, policy.decision(null, 1_000L, false))
        val empty = GameCatalogSnapshot(
            researchedAtEpochMillis = 100L,
            expiresAtEpochMillis = 200L,
            items = emptyList(),
            cycleId = GameCandidateCycleId("cycle-empty"),
        )
        assertEquals(GameCatalogCacheDecision.NoUsableCache, policy.decision(empty, 1_000L, false))
    }

    @Test
    fun effectiveFreshnessIsDeterministicForSameSignals() {
        val signals = GameCatalogCacheSignals(1, 2, 4, 1)
        assertEquals(policy.effectiveMaxAgeMillis(signals), policy.effectiveMaxAgeMillis(signals))
    }

    @Test
    fun staleRefreshFailureDoesNotEraseExistingSnapshot() {
        val snapshot = snapshot(itemCount = 2, researchedAt = 100L)
        val decision = policy.decision(snapshot, 2_000L, online = true)
        assertFalse(decision == GameCatalogCacheDecision.NoCache)
        assertEquals(GameCatalogCacheDecision.RefreshRequired, decision)
        assertEquals(2, snapshot.items.size)
    }

    private fun game(index: Int, researchedAt: Long): GameDiscovery =
        GameDiscovery(
            canonicalName = "Game $index",
            platforms = emptySet(),
            genres = emptySet(),
            availability = GameAvailability.UNKNOWN,
            provenance = setOf(ResearchProvenance("https://example.test/$index", researchedAt)),
        )

    private fun snapshot(itemCount: Int, researchedAt: Long): GameCatalogSnapshot =
        GameCatalogSnapshot(
            researchedAtEpochMillis = researchedAt,
            expiresAtEpochMillis = researchedAt + 1_000L,
            items = (0 until itemCount).map { index -> game(index, researchedAt) },
            cycleId = GameCandidateCycleId("cycle-$researchedAt"),
        )
}
