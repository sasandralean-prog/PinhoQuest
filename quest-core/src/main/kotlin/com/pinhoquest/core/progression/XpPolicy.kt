package com.pinhoquest.core.progression

import com.pinhoquest.domain.progression.QuestCompletionFacts
import com.pinhoquest.domain.quest.QuestDifficulty
import kotlin.math.max
import kotlin.math.min

object XpPolicyV1 {
    const val VERSION = 1
    private const val MAX_BONUS_OBJECTIVES = 2

    private val baseByDifficulty = mapOf(
        QuestDifficulty.CASUAL to 10,
        QuestDifficulty.EASY to 25,
        QuestDifficulty.MEDIUM to 50,
        QuestDifficulty.CHALLENGE to 100,
    )

    fun awardFor(completion: QuestCompletionFacts): Int {
        val base = baseByDifficulty.getValue(completion.difficulty)
        val completedBonus = min(
            completion.completedBonusObjectiveCount,
            MAX_BONUS_OBJECTIVES,
        )
        val bonusPerObjective = max(5, base / 10)
        return base + (completedBonus * bonusPerObjective)
    }
}
