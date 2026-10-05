package com.pinhoquest.core.research

data class GameResearchProvider(
    val id: String,
    val port: GameResearchPort,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
    }
}

data class GameResearchProviderResult(
    val providerId: String,
    val outcome: ResearchOutcome<GameDiscovery>,
)

data class GameResearchRun(
    val catalog: List<GameDiscovery>,
    val providerResults: List<GameResearchProviderResult>,
    val researchedAtEpochMillis: Long,
) {
    val successfulProviders: List<String>
        get() = providerResults
            .filter { it.outcome is ResearchOutcome.Success }
            .map { it.providerId }

    val hasUsableResults: Boolean
        get() = catalog.isNotEmpty()

    fun snapshot(
        policy: GameCatalogCachePolicy,
        cycleId: GameCandidateCycleId,
    ): GameCatalogSnapshot {
        val boundedCatalog = policy.selectCandidates(catalog, emptySet())
        val signals = GameCatalogCacheSignals(
            successfulProviderCount = successfulProviders.size,
            attemptedProviderCount = providerResults.size,
            catalogItemCount = boundedCatalog.size,
        )
        return GameCatalogSnapshot(
            researchedAtEpochMillis = researchedAtEpochMillis,
            expiresAtEpochMillis = policy.expiresAt(researchedAtEpochMillis, signals),
            items = boundedCatalog,
            cycleId = cycleId,
        )
    }
}

class GameResearchCoordinator(
    private val providers: List<GameResearchProvider>,
    private val nowEpochMillis: () -> Long,
) {
    init {
        require(providers.map { it.id }.distinct().size == providers.size) {
            "provider ids must be unique"
        }
    }

    suspend fun research(
        query: String,
        platform: String? = null,
    ): GameResearchRun {
        val results = providers.map { provider ->
            GameResearchProviderResult(
                providerId = provider.id,
                outcome = provider.port.research(query, platform),
            )
        }

        val catalog = GameDiscoveryCatalog()
        results.forEach { result ->
            val outcome = result.outcome
            if (outcome is ResearchOutcome.Success) {
                catalog.merge(outcome.items)
            }
        }

        return GameResearchRun(
            catalog = catalog.snapshot(),
            providerResults = results,
            researchedAtEpochMillis = nowEpochMillis(),
        )
    }
}
