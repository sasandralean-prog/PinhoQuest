package com.pinhoquest.domain.progression

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty

@JvmInline value class GoalId(val value: String)

sealed interface GoalRule {
    val target: Int

    data class CompletionCount(override val target: Int) : GoalRule
    data class CategoryCompletion(
        val category: QuestCategory,
        override val target: Int,
    ) : GoalRule
    data class DifficultyCompletion(
        val difficulty: QuestDifficulty,
        override val target: Int,
    ) : GoalRule
    data class BonusObjectiveCount(override val target: Int) : GoalRule
    data class LifetimeXpMilestone(override val target: Int) : GoalRule
}

data class GoalDefinition(
    val id: GoalId,
    val title: String,
    val rule: GoalRule,
)

data class GoalProgress(
    val goalId: GoalId,
    val currentValue: Int,
    val targetValue: Int,
    val completed: Boolean,
    val appliedCompletionIds: Set<CompletionId> = emptySet(),
)

data class GoalEvaluationResult(
    val progress: List<GoalProgress>,
    val newlyCompletedGoalIds: Set<GoalId>,
)
