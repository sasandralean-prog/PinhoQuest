package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.QuestFallbackReason
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
    val prompt: BoundedMicroQuestPrompt,
    val style: MicroQuestStyle = MicroQuestStyle(),
)

data class MicroQuestText(
    val title: String,
    val description: String,
    val objectives: List<String>,
)

enum class MicroQuestOrigin { LOCAL_MODEL, PROCEDURAL_FALLBACK }

data class RenderedMicroQuest(
    val draft: QuestDraft,
    val origin: MicroQuestOrigin,
    val fallbackReason: QuestFallbackReason? = null,
)
