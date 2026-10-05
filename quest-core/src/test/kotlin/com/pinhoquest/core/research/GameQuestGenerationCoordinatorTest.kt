package com.pinhoquest.core.research

import com.pinhoquest.core.quest.GameQuestSeed
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameQuestGenerationCoordinatorTest {
    private val policy = GameCatalogCachePolicy(100L, 1_000L, 500L, maxCandidates = 2)
    private val provenance = ResearchProvenance("https://example.test/game", 1_000L)

    @Test fun selectedCandidateCrossesCanonicalBoundaryWithoutTextInference() = runBlocking {
        val context = GameQuestContext(
            GameCandidateCycleId("cycle-1"),
            GameDiscovery(
                canonicalName = "Minecraft",
                platforms = setOf("Android"),
                genres = setOf("Sandbox"),
                availability = GameAvailability.UNKNOWN,
                provenance = setOf(provenance),
            ),
        )
        val coordinator = GameQuestGenerationCoordinator(
            FakeCatalogStore(GameCatalogSnapshot(1_000L, 2_000L, listOf(context.game), context.cycleId)),
            FakeUsageStore(),
            GameQuestCandidateSelector(policy),
        )

        val result = coordinator.selectCandidate()

        assertTrue(result is GameQuestCandidateResult.Selected)
        val selected = result as GameQuestCandidateResult.Selected
        assertEquals("minecraft", selected.context.gameIdentity.value)
        assertEquals(GameQuestSeed.from(context), selected.seed)
        assertEquals("cycle-1", selected.seed.cycleId)
    }

    @Test fun freshRandomSelectionUsesOnlyFreshCacheAndCreatesSemanticVariant() = runBlocking {
        val games = listOf("Game A", "Game B", "Game C").map { name ->
            GameDiscovery(
                canonicalName = name,
                platforms = setOf("Android"),
                genres = setOf("RPG"),
                availability = GameAvailability.UNKNOWN,
                provenance = setOf(provenance),
            )
        }
        val snapshot = GameCatalogSnapshot(
            researchedAtEpochMillis = 1_000L,
            expiresAtEpochMillis = 2_000L,
            items = games,
            cycleId = GameCandidateCycleId("cycle-fresh"),
        )
        val coordinator = GameQuestGenerationCoordinator(
            FakeCatalogStore(snapshot),
            FakeUsageStore(),
            GameQuestCandidateSelector(policy, Random(7)),
            nowEpochMillis = { 1_999L },
        )

        val result = coordinator.selectFreshRandomCandidate()

        assertTrue(result is GameQuestCandidateResult.Selected)
        val selected = result as GameQuestCandidateResult.Selected
        assertTrue(selected.context.game.canonicalName in games.map { it.canonicalName })
        assertNotNull(selected.context.variant)
        assertEquals("cycle-fresh", selected.seed.cycleId)
        assertEquals(selected.context.variant, selected.seed.variant)
    }

    @Test fun staleCacheIsRejectedWithoutResearchOrFallback() = runBlocking {
        val snapshot = GameCatalogSnapshot(
            researchedAtEpochMillis = 1_000L,
            expiresAtEpochMillis = 2_000L,
            items = listOf(
                GameDiscovery(
                    canonicalName = "Cached Game",
                    platforms = setOf("Android"),
                    genres = emptySet(),
                    availability = GameAvailability.UNKNOWN,
                    provenance = setOf(provenance),
                ),
            ),
            cycleId = GameCandidateCycleId("cycle-stale"),
        )
        val coordinator = GameQuestGenerationCoordinator(
            FakeCatalogStore(snapshot),
            FakeUsageStore(),
            GameQuestCandidateSelector(policy),
            nowEpochMillis = { 2_001L },
        )

        val result = coordinator.selectFreshRandomCandidate()

        assertEquals(GameQuestCandidateResult.StaleCatalog(snapshot.cycleId), result)
    }

    @Test fun usedGameIsExcludedFromTheSameCycle() = runBlocking {
        val game = GameDiscovery(
            canonicalName = "Minecraft",
            platforms = setOf("Android"),
            genres = emptySet(),
            availability = GameAvailability.UNKNOWN,
            provenance = setOf(provenance),
        )
        val snapshot = GameCatalogSnapshot(1_000L, 2_000L, listOf(game), GameCandidateCycleId("cycle-1"))
        val usage = FakeUsageStore(setOf(GameIdentityKey("minecraft")))
        val coordinator = GameQuestGenerationCoordinator(
            FakeCatalogStore(snapshot),
            usage,
            GameQuestCandidateSelector(policy),
        )

        val result = coordinator.selectCandidate()

        assertTrue(result is GameQuestCandidateResult.CycleExhausted)
        assertTrue((result as GameQuestCandidateResult.CycleExhausted).refreshRequired)
    }

    @Test fun recordingRequiresStructuredVariantAndDoesNotUseRenderedText() = runBlocking {
        val game = GameDiscovery(
            canonicalName = "Minecraft",
            platforms = setOf("Android"),
            genres = emptySet(),
            availability = GameAvailability.UNKNOWN,
            provenance = setOf(provenance),
        )
        val context = GameQuestContext(GameCandidateCycleId("cycle-1"), game)
        val usage = FakeUsageStore()
        val coordinator = GameQuestGenerationCoordinator(
            FakeCatalogStore(GameCatalogSnapshot(1_000L, 2_000L, listOf(game), context.cycleId)),
            usage,
            GameQuestCandidateSelector(policy),
        )
        val variant = GameQuestVariant(
            activity = GameQuestActivity.DISCOVERY,
            category = com.pinhoquest.domain.quest.QuestCategory.GAMING,
            environment = com.pinhoquest.domain.quest.QuestEnvironment.ANDROID,
            objectivePattern = "test a different mechanic",
        )

        val usageId = coordinator.recordUsage(context, variant, 3_000L)

        assertEquals(context.usageId(variant), usageId)
        assertEquals(setOf(GameIdentityKey("minecraft")), usage.usedKeys)
        assertNotNull(usage.recorded)
        assertEquals(variant.canonicalFingerprint, usage.recorded!!.variantFingerprint)
    }

    private class FakeCatalogStore(
        private val snapshot: GameCatalogSnapshot?,
    ) : GameDiscoveryCatalogStore {
        override suspend fun read(): GameCatalogSnapshot? = snapshot
        override suspend fun write(snapshot: GameCatalogSnapshot) {}
    }

    private class FakeUsageStore(
        initial: Set<GameIdentityKey> = emptySet(),
    ) : GameQuestUsageStore {
        var recorded: GameQuestUsage? = null
        var usedKeys: Set<GameIdentityKey> = initial
            private set

        override suspend fun usedIdentityKeys(cycleId: GameCandidateCycleId): Set<GameIdentityKey> = usedKeys

        override suspend fun record(usage: GameQuestUsage) {
            recorded = usage
            usedKeys = usedKeys + usage.gameIdentity
        }

        override suspend fun clearCycle(cycleId: GameCandidateCycleId) {
            usedKeys = emptySet()
        }
    }
}
