package com.pinhoquest.core.research

import kotlin.random.Random

data class GameQuestContext(
    val cycleId: GameCandidateCycleId,
    val game: GameDiscovery,
    val variant: GameQuestVariant? = null,
) {
    val gameIdentity: GameIdentityKey
        get() = GameIdentityKey(game.identityKey)

    fun usageId(variant: GameQuestVariant): GameQuestUsageId =
        GameQuestUsageId.create(cycleId, gameIdentity, variant)
}

class GameQuestCandidateSelector(
    private val policy: GameCatalogCachePolicy,
    private val random: Random = Random.Default,
) {
    fun select(
        snapshot: GameCatalogSnapshot,
        usedIdentityKeys: Set<GameIdentityKey>,
    ): GameQuestContext? =
        selectFrom(snapshot, usedIdentityKeys, randomVariant = false)

    fun selectRandom(
        snapshot: GameCatalogSnapshot,
        usedIdentityKeys: Set<GameIdentityKey>,
    ): GameQuestContext? =
        selectFrom(snapshot, usedIdentityKeys, randomVariant = true)

    private fun selectFrom(
        snapshot: GameCatalogSnapshot,
        usedIdentityKeys: Set<GameIdentityKey>,
        randomVariant: Boolean,
    ): GameQuestContext? {
        val used = usedIdentityKeys.map { it.value }.toSet()
        val candidates = policy.selectCandidates(snapshot.items, used)
        val candidate = candidates
            .takeIf { it.isNotEmpty() }
            ?.let { if (randomVariant) it.random(random) else it.first() }
            ?: return null

        val variant = if (randomVariant) randomVariant(candidate) else null
        return GameQuestContext(snapshot.cycleId, candidate, variant)
    }

    private fun randomVariant(game: GameDiscovery): GameQuestVariant {
        val activity = listOf(
            GameQuestActivity.DISCOVERY,
            GameQuestActivity.CONSTRUCTION,
            GameQuestActivity.EXPLORATION,
            GameQuestActivity.COMBAT,
        ).random(random)

        val genre = game.genres
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .firstOrNull()
            ?: "gameplay"

        val objectivePattern = listOf(
            "experimentar uma mecânica marcante",
            "explorar uma possibilidade diferente",
            "testar uma estratégia nova",
            "descobrir um detalhe que normalmente passa despercebido",
            "revisitar uma parte conhecida com outra abordagem",
        ).random(random)

        return GameQuestVariant(
            activity = activity,
            category = com.pinhoquest.domain.quest.QuestCategory.GAMING,
            environment = if (game.platforms.any { it.equals("Android", ignoreCase = true) }) {
                com.pinhoquest.domain.quest.QuestEnvironment.ANDROID
            } else {
                com.pinhoquest.domain.quest.QuestEnvironment.ANYWHERE
            },
            objectivePattern = "$genre: $objectivePattern",
        )
    }

    fun needsUsageRefresh(
        snapshot: GameCatalogSnapshot,
        usedIdentityKeys: Set<GameIdentityKey>,
    ): Boolean =
        policy.shouldRefreshForUsage(
            snapshot.items,
            usedIdentityKeys.map { it.value }.toSet(),
        )
}
