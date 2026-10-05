package com.pinhoquest.core.research

import com.pinhoquest.core.quest.GameQuestSeed

class GameQuestApplicationCoordinator(
    private val researchOrchestrator: GameResearchOrchestrator,
    private val generationCoordinator: GameQuestGenerationCoordinator,
) {
    suspend fun prepare(request: GameResearchRequest): GameQuestApplicationResult =
        when (val research = researchOrchestrator.resolve(request)) {
            is GameResearchOrchestrationResult.Ready ->
                selectCandidate(research.snapshot)

            is GameResearchOrchestrationResult.ServedStaleAfterRefreshFailure ->
                selectCandidate(research.snapshot)

            GameResearchOrchestrationResult.NoUsableCatalog ->
                GameQuestApplicationResult.NoUsableCatalog
        }

    private suspend fun selectCandidate(
        snapshot: GameCatalogSnapshot,
    ): GameQuestApplicationResult =
        when (val candidate = generationCoordinator.selectCandidate()) {
            GameQuestCandidateResult.NoCatalog ->
                GameQuestApplicationResult.NoUsableCatalog

            is GameQuestCandidateResult.Selected ->
                GameQuestApplicationResult.Selected(
                    seed = candidate.seed,
                    context = candidate.context,
                    snapshot = snapshot,
                )

            is GameQuestCandidateResult.CycleExhausted ->
                GameQuestApplicationResult.CycleExhausted(
                    cycleId = candidate.cycleId,
                    refreshRequired = candidate.refreshRequired,
                )

            is GameQuestCandidateResult.StaleCatalog ->
                GameQuestApplicationResult.NoUsableCatalog
        }
}

sealed interface GameQuestApplicationResult {
    data object NoUsableCatalog : GameQuestApplicationResult

    data class Selected(
        val seed: GameQuestSeed,
        val context: GameQuestContext,
        val snapshot: GameCatalogSnapshot,
    ) : GameQuestApplicationResult

    data class CycleExhausted(
        val cycleId: GameCandidateCycleId,
        val refreshRequired: Boolean,
    ) : GameQuestApplicationResult
}
