package com.pinhoquest.core.inference.prompt

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestEnvironment

data class PromptTag(
    val label: String,
)

enum class PromptResearchKind { GAME, FLOWER }

enum class PromptAvailability {
    FREE,
    FREE_TO_PLAY,
    UNKNOWN,
}

sealed interface PromptResearchHint {
    val kind: PromptResearchKind
    val canonicalName: String

    data class Game(
        override val canonicalName: String,
        val platform: String?,
        val genre: String?,
        val availability: PromptAvailability,
        val focus: String? = null,
    ) : PromptResearchHint {
        override val kind: PromptResearchKind = PromptResearchKind.GAME
    }

    data class Flower(
        override val canonicalName: String,
        val commonName: String?,
        val region: String?,
    ) : PromptResearchHint {
        override val kind: PromptResearchKind = PromptResearchKind.FLOWER
    }
}

data class BoundedPromptEnvelope(
    val schemaVersion: Int,
    val task: String,
    val category: QuestCategory,
    val environment: QuestEnvironment,
    val minMinutes: Int?,
    val maxMinutes: Int?,
    val difficulty: QuestDifficulty?,
    val selectedTags: List<PromptTag>,
    val researchHints: List<PromptResearchHint>,
) {
    init {
        require(schemaVersion > 0) { "schemaVersion must be positive" }
        require(task.isNotBlank()) { "task must not be blank" }
    }
}

data class PromptContractLimits(
    val maxTags: Int = 6,
    val maxTagCharacters: Int = 32,
    val maxResearchHints: Int = 2,
    val maxResearchFieldCharacters: Int = 48,
    val maxPromptCharacters: Int = 1400,
) {
    init {
        require(maxTags > 0)
        require(maxTagCharacters > 0)
        require(maxResearchHints >= 0)
        require(maxResearchFieldCharacters > 0)
        require(maxPromptCharacters > 0)
    }
}

object QuestPromptContract {
    const val SCHEMA_VERSION = 1
    const val TASK = "QUEST_COMPOSE"

    val OUTPUT_FIELDS = listOf(
        "title",
        "description",
        "objectives",
        "bonusObjectives",
        "estimatedMinutes",
        "estimatedDifficulty",
    )
}
