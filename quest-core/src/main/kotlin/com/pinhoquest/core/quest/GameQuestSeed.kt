package com.pinhoquest.core.quest

import com.pinhoquest.core.research.GameIdentityKey
import com.pinhoquest.core.research.GameQuestContext
import com.pinhoquest.core.research.GameQuestVariant
import com.pinhoquest.domain.quest.QuestEnvironment

data class GameQuestSeed(
    val gameIdentity: GameIdentityKey,
    val title: String,
    val environment: QuestEnvironment,
    val cycleId: String,
    val variant: GameQuestVariant? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
) {
    companion object {
        fun from(context: GameQuestContext): GameQuestSeed =
            GameQuestSeed(
                gameIdentity = context.gameIdentity,
                title = context.game.canonicalName,
                environment = context.variant?.environment ?: context.game.platforms
                    .let { platforms ->
                        if (platforms.any { it.equals("Android", ignoreCase = true) }) {
                            QuestEnvironment.ANDROID
                        } else {
                            QuestEnvironment.ANYWHERE
                        }
                    },
                cycleId = context.cycleId.value,
                variant = context.variant,
                genres = context.game.genres.sortedWith(String.CASE_INSENSITIVE_ORDER),
                platforms = context.game.platforms.sortedWith(String.CASE_INSENSITIVE_ORDER),
            )
    }
}
