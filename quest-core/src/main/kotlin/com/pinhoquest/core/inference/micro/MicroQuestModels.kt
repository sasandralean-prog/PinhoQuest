package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestDraft

data class QuestTextExample(
    val title: String,
    val description: String,
    val objectives: List<String>,
) {
    init {
        require(title.isNotBlank())
        require(description.isNotBlank())
        require(objectives.isNotEmpty())
    }
}

data class MicroQuestStyle(
    val tone: String = "curto, humano, convidativo",
    val language: String = "pt-BR",
)

data class MicroQuestCompositionRequest(
    val plan: QuestGenerationPlan,
    val tags: List<String> = emptyList(),
    val examples: List<QuestTextExample> = emptyList(),
    val style: MicroQuestStyle = MicroQuestStyle(),
) {
    init {
        require(tags.size <= 6)
        require(examples.size <= 3)
    }
}

data class MicroQuestText(
    val title: String,
    val description: String,
    val objectives: List<String>,
)

enum class MicroQuestOrigin { LOCAL_MODEL, PROCEDURAL_FALLBACK }

data class RenderedMicroQuest(
    val draft: QuestDraft,
    val origin: MicroQuestOrigin,
)

object MicroQuestContract {
    const val NAME = "MICRO_QUEST_TEXT_V1"
    const val MAX_TITLE = 80
    const val MAX_DESCRIPTION = 220
    const val MAX_OBJECTIVES = 4
    const val MAX_OBJECTIVE = 120
}
