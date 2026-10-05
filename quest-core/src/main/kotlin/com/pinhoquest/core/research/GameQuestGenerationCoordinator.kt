package com.pinhoquest.core.research

import com.pinhoquest.core.quest.GameQuestSeed

/**
 * Resolves a bounded researched game candidate into the quest-planning boundary.
 *
 * The coordinator never derives identity or semantic usage from rendered quest text.
 */
class GameQuestGenerationCoordinator(
    private val catalogStore: GameDiscoveryCatalogStore,
    private val usageStore: GameQuestUsageStore,
    private val candidateSelector: GameQuestCandidateSelector,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun selectCandidate(): GameQuestCandidateResult {
        val snapshot = catalogStore.read()
            ?: return GameQuestCandidateResult.NoCatalog

        val usedIdentityKeys = usageStore.usedIdentityKeys(snapshot.cycleId)
        val context = candidateSelector.select(snapshot, usedIdentityKeys)
            ?: return exhausted(snapshot, usedIdentityKeys)

        return GameQuestCandidateResult.Selected(
            context = context,
            seed = GameQuestSeed.from(context),
        )
    }

    /**
     * GAME button boundary: only consumes a fresh persisted catalog.
     * It never invokes research or invents a query.
     */
    suspend fun selectFreshRandomCandidate(): GameQuestCandidateResult {
        val snapshot = catalogStore.read()
            ?: return GameQuestCandidateResult.NoCatalog

        if (nowEpochMillis() >= snapshot.expiresAtEpochMillis) {
            return GameQuestCandidateResult.StaleCatalog(snapshot.cycleId)
        }

        val usedIdentityKeys = usageStore.usedIdentityKeys(snapshot.cycleId)
        val context = candidateSelector.selectRandom(snapshot, usedIdentityKeys)
            ?: return exhausted(snapshot, usedIdentityKeys)

        return GameQuestCandidateResult.Selected(
            context = context,
            seed = GameQuestSeed.from(context),
        )
    }

    private fun exhausted(
        snapshot: GameCatalogSnapshot,
        usedIdentityKeys: Set<GameIdentityKey>,
    ): GameQuestCandidateResult.CycleExhausted =
        GameQuestCandidateResult.CycleExhausted(
            cycleId = snapshot.cycleId,
            refreshRequired = candidateSelector.needsUsageRefresh(snapshot, usedIdentityKeys),
        )

    suspend fun recordUsage(
        context: GameQuestContext,
        variant: GameQuestVariant,
        usedAtEpochMillis: Long,
    ): GameQuestUsageId {
        val usageId = context.usageId(variant)
        usageStore.record(
            GameQuestUsage(
                usageId = usageId,
                cycleId = context.cycleId,
                gameIdentity = context.gameIdentity,
                variantFingerprint = variant.canonicalFingerprint,
                usedAtEpochMillis = usedAtEpochMillis,
            ),
        )
        return usageId
    }
}

sealed interface GameQuestCandidateResult {
    data object NoCatalog : GameQuestCandidateResult

    data class Selected(
        val context: GameQuestContext,
        val seed: GameQuestSeed,
    ) : GameQuestCandidateResult

    data class CycleExhausted(
        val cycleId: GameCandidateCycleId,
        val refreshRequired: Boolean,
    ) : GameQuestCandidateResult

    data class StaleCatalog(
        val cycleId: GameCandidateCycleId,
    ) : GameQuestCandidateResult
}
