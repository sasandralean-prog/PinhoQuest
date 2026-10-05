package com.pinhoquest.core.research

data class GameCatalogCacheSignals(
    val successfulProviderCount: Int,
    val attemptedProviderCount: Int,
    val catalogItemCount: Int,
    val usedCatalogItemCount: Int = 0,
) {
    init {
        require(successfulProviderCount >= 0)
        require(attemptedProviderCount > 0)
        require(successfulProviderCount <= attemptedProviderCount)
        require(catalogItemCount >= 0)
        require(usedCatalogItemCount in 0..catalogItemCount)
    }

    val successRatio: Double
        get() = successfulProviderCount.toDouble() / attemptedProviderCount

    val unusedRatio: Double
        get() = if (catalogItemCount == 0) 0.0
        else (catalogItemCount - usedCatalogItemCount).toDouble() / catalogItemCount
}

data class GameCatalogCachePolicy(
    val minimumAgeMillis: Long,
    val maximumAgeMillis: Long,
    val referenceAgeMillis: Long,
    val maxCandidates: Int = DEFAULT_MAX_CANDIDATES,
) {
    init {
        require(minimumAgeMillis > 0L)
        require(maximumAgeMillis >= minimumAgeMillis)
        require(referenceAgeMillis in minimumAgeMillis..maximumAgeMillis)
        require(maxCandidates > 0)
    }

    fun effectiveMaxAgeMillis(signals: GameCatalogCacheSignals): Long {
        val providerStability = signals.successRatio
        val catalogRichness = signals.catalogItemCount.toDouble() / (signals.catalogItemCount + 1.0)
        val usageCoverage = signals.unusedRatio
        val confidence = (providerStability * 0.60) + (catalogRichness * 0.20) + (usageCoverage * 0.20)
        val span = maximumAgeMillis - minimumAgeMillis
        return (referenceAgeMillis + ((confidence - 0.5) * span))
            .toLong()
            .coerceIn(minimumAgeMillis, maximumAgeMillis)
    }

    fun isFresh(snapshot: GameCatalogSnapshot, nowEpochMillis: Long): Boolean {
        require(nowEpochMillis >= snapshot.researchedAtEpochMillis)
        return snapshot.isFresh(nowEpochMillis)
    }

    fun expiresAt(
        researchedAtEpochMillis: Long,
        signals: GameCatalogCacheSignals,
    ): Long {
        require(researchedAtEpochMillis > 0L)
        return Math.addExact(researchedAtEpochMillis, effectiveMaxAgeMillis(signals))
    }

    fun selectCandidates(
        items: List<GameDiscovery>,
        usedIdentityKeys: Set<String>,
    ): List<GameDiscovery> =
        items
            .distinctBy { it.identityKey }
            .asSequence()
            .filter { it.identityKey !in usedIdentityKeys }
            .sortedWith(
                compareByDescending<GameDiscovery> {
                    it.provenance.maxOf { evidence -> evidence.researchedAtEpochMillis }
                }.thenBy { it.identityKey },
            )
            .take(maxCandidates)
            .toList()

    fun shouldRefreshForUsage(
        items: List<GameDiscovery>,
        usedIdentityKeys: Set<String>,
    ): Boolean =
        items.count { it.identityKey !in usedIdentityKeys } < maxCandidates

    fun decision(
        snapshot: GameCatalogSnapshot?,
        nowEpochMillis: Long,
        online: Boolean,
    ): GameCatalogCacheDecision = when {
        snapshot == null && online -> GameCatalogCacheDecision.RefreshRequired
        snapshot == null -> GameCatalogCacheDecision.NoCache
        isFresh(snapshot, nowEpochMillis) -> GameCatalogCacheDecision.UseFresh
        online -> GameCatalogCacheDecision.RefreshRequired
        snapshot.items.isNotEmpty() -> GameCatalogCacheDecision.ServeStale
        else -> GameCatalogCacheDecision.NoUsableCache
    }

    companion object {
        const val DEFAULT_MAX_CANDIDATES = 20
        val DEFAULT = GameCatalogCachePolicy(
            minimumAgeMillis = 15 * 60 * 1000L,
            maximumAgeMillis = 24 * 60 * 60 * 1000L,
            referenceAgeMillis = 6 * 60 * 60 * 1000L,
        )
    }
}

enum class GameCatalogCacheDecision {
    UseFresh,
    RefreshRequired,
    ServeStale,
    ServeStaleAfterRefreshFailure,
    NoCache,
    NoUsableCache,
}

data class GameCatalogSnapshot(
    val researchedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val items: List<GameDiscovery>,
    val cycleId: GameCandidateCycleId,
) {
    init {
        require(researchedAtEpochMillis > 0L)
        require(expiresAtEpochMillis >= researchedAtEpochMillis)
        require(items.map { it.identityKey }.distinct().size == items.size)
    }

    fun isFresh(nowEpochMillis: Long): Boolean = nowEpochMillis <= expiresAtEpochMillis
}

interface GameDiscoveryCatalogStore {
    suspend fun read(): GameCatalogSnapshot?
    suspend fun write(snapshot: GameCatalogSnapshot)
}
