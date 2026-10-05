package com.pinhoquest.core.research

/**
 * Canonical input for a user-triggered GAME research request.
 *
 * Query ownership stays above the research provider: providers only execute this
 * normalized contract and never invent product intent.
 */
data class GameResearchRequest(
    val query: String,
    val platform: String? = null,
) {
    init {
        require(query.isNotBlank()) { "query must not be blank" }
        require(platform?.isNotBlank() != false) { "platform must not be blank" }
    }
}

sealed interface GameResearchOrchestrationResult {
    data class Ready(
        val snapshot: GameCatalogSnapshot,
        val decision: GameCatalogCacheDecision,
    ) : GameResearchOrchestrationResult

    data class ServedStaleAfterRefreshFailure(
        val snapshot: GameCatalogSnapshot,
    ) : GameResearchOrchestrationResult

    data object NoUsableCatalog : GameResearchOrchestrationResult
}

class GameResearchOrchestrator(
    private val catalogStore: GameDiscoveryCatalogStore,
    private val researchCoordinator: GameResearchCoordinator,
    private val cachePolicy: GameCatalogCachePolicy,
    private val cycleSequence: GameCandidateCycleSequence,
    private val nowEpochMillis: () -> Long,
    private val online: () -> Boolean,
) {
    suspend fun resolve(request: GameResearchRequest): GameResearchOrchestrationResult {
        val existing = catalogStore.read()
        val now = nowEpochMillis()
        val decision = cachePolicy.decision(existing, now, online())

        when (decision) {
            GameCatalogCacheDecision.UseFresh ->
                return GameResearchOrchestrationResult.Ready(existing!!, decision)

            GameCatalogCacheDecision.ServeStale ->
                return GameResearchOrchestrationResult.Ready(existing!!, decision)

            GameCatalogCacheDecision.NoCache,
            GameCatalogCacheDecision.NoUsableCache,
            GameCatalogCacheDecision.RefreshRequired,
            GameCatalogCacheDecision.ServeStaleAfterRefreshFailure -> Unit
        }

        if (!online()) {
            return existing?.let {
                GameResearchOrchestrationResult.ServedStaleAfterRefreshFailure(it)
            } ?: GameResearchOrchestrationResult.NoUsableCatalog
        }

        val run = researchCoordinator.research(request.query, request.platform)
        if (!run.hasUsableResults) {
            return existing?.let {
                GameResearchOrchestrationResult.ServedStaleAfterRefreshFailure(it)
            } ?: GameResearchOrchestrationResult.NoUsableCatalog
        }

        val snapshot = run.snapshot(
            cachePolicy,
            cycleSequence.next(run.researchedAtEpochMillis),
        )
        catalogStore.write(snapshot)
        return GameResearchOrchestrationResult.Ready(
            snapshot = snapshot,
            decision = GameCatalogCacheDecision.RefreshRequired,
        )
    }
}
