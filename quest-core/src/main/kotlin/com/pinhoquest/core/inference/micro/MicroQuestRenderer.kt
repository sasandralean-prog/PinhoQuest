package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestObjective

class MicroQuestRenderer {
    fun render(text: MicroQuestText, plan: QuestGenerationPlan): QuestDraft {
        val minMinutes = plan.filters.minMinutes ?: 15
        val maxMinutes = plan.filters.maxMinutes ?: maxOf(minMinutes, 30)
        val difficulty = plan.filters.desiredDifficulty ?: QuestDifficulty.EASY

        return QuestDraft(
            title = text.title,
            description = text.description,
            objectives = text.objectives.mapIndexed { index, objective ->
                QuestObjective(
                    id = ObjectiveId("main-${index + 1}"),
                    text = objective,
                )
            },
            category = plan.selectedCategory,
            environment = plan.selectedEnvironment,
            estimatedDuration = EstimatedDuration(minMinutes, maxMinutes),
            difficulty = difficulty,
        )
    }
}
