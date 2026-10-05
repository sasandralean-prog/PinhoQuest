package com.pinhoquest.core.research

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import java.security.MessageDigest

@JvmInline
value class GameIdentityKey(val value: String) {
    init { require(value.isNotBlank()) { "game identity must not be blank" } }
}

@JvmInline
value class GameCandidateCycleId(val value: String) {
    init { require(value.isNotBlank()) { "candidate cycle id must not be blank" } }

    companion object {
        fun create(researchedAtEpochMillis: Long, sequence: Long): GameCandidateCycleId =
            GameCandidateCycleId("cycle-$researchedAtEpochMillis-$sequence")
    }
}

@JvmInline
value class GameQuestUsageId(val value: String) {
    init {
        require(value.matches(Regex("[0-9a-f]{64}"))) { "usage id must be a SHA-256 hex digest" }
    }

    companion object {
        fun create(
            cycleId: GameCandidateCycleId,
            gameIdentity: GameIdentityKey,
            variant: GameQuestVariant,
        ): GameQuestUsageId {
            val canonical = buildString {
                append("pinhoquest/game-quest-usage/v1|")
                append(cycleId.value)
                append("|")
                append(gameIdentity.value)
                append("|")
                append(variant.canonicalFingerprint)
            }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
            return GameQuestUsageId(
                digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) },
            )
        }
    }
}

enum class GameQuestActivity {
    EXPLORATION,
    CONSTRUCTION,
    COMBAT,
    COLLECTION,
    PUZZLE,
    CREATION,
    STORY,
    DISCOVERY,
}

data class GameQuestVariant(
    val activity: GameQuestActivity,
    val category: QuestCategory,
    val environment: QuestEnvironment,
    val objectivePattern: String,
) {
    init {
        require(objectivePattern.isNotBlank()) { "objectivePattern must not be blank" }
    }

    val canonicalFingerprint: String
        get() = listOf(
            activity.name,
            category.name,
            environment.name,
            normalize(objectivePattern),
        ).joinToString("|")

    companion object {
        fun normalize(value: String): String =
            value.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}

data class GameQuestUsage(
    val usageId: GameQuestUsageId,
    val cycleId: GameCandidateCycleId,
    val gameIdentity: GameIdentityKey,
    val variantFingerprint: String,
    val usedAtEpochMillis: Long,
) {
    init {
        require(variantFingerprint.isNotBlank()) { "variantFingerprint must not be blank" }
        require(usedAtEpochMillis > 0L) { "usedAtEpochMillis must be positive" }
    }
}

interface GameQuestUsageStore {
    suspend fun usedIdentityKeys(cycleId: GameCandidateCycleId): Set<GameIdentityKey>
    suspend fun record(usage: GameQuestUsage)
    suspend fun clearCycle(cycleId: GameCandidateCycleId)
}
