package com.pinhoquest.core.progression

import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalEvaluationResult
import com.pinhoquest.domain.progression.GoalId
import com.pinhoquest.domain.progression.GoalProgress
import com.pinhoquest.domain.progression.GoalRule
import com.pinhoquest.domain.progression.QuestCompletionFacts
import kotlin.math.min

class GoalEvaluator(
    private val definitions: List<GoalDefinition>,
) {
    init {
        require(definitions.map { it.id }.distinct().size == definitions.size) {
            "goal ids must be unique"
        }
        definitions.forEach { require(it.rule.target > 0) { "goal target must be positive" } }
    }

    fun evaluate(
        completion: QuestCompletionFacts,
        current: List<GoalProgress>,
        lifetimeXpAfter: Int,
    ): GoalEvaluationResult {
        require(lifetimeXpAfter >= 0) { "lifetimeXpAfter must be non-negative" }
        val currentById = current.associateBy { it.goalId }
        val newlyCompleted = linkedSetOf<GoalId>()

        val next = definitions.map { definition ->
            val before = currentById[definition.id] ?: GoalProgress(
                goalId = definition.id,
                currentValue = 0,
                targetValue = definition.rule.target,
                completed = false,
            )
            if (completion.completionId in before.appliedCompletionIds) {
                before
            } else {
                val nextValue = calculateValue(
                    definition = definition,
                    before = before,
                    completion = completion,
                    lifetimeXpAfter = lifetimeXpAfter,
                )
                val completed = nextValue >= definition.rule.target
                if (!before.completed && completed) {
                    newlyCompleted += definition.id
                }
                before.copy(
                    currentValue = min(nextValue, definition.rule.target),
                    targetValue = definition.rule.target,
                    completed = completed,
                    appliedCompletionIds = before.appliedCompletionIds + completion.completionId,
                )
            }
        }

        return GoalEvaluationResult(
            progress = next,
            newlyCompletedGoalIds = newlyCompleted,
        )
    }

    private fun calculateValue(
        definition: GoalDefinition,
        before: GoalProgress,
        completion: QuestCompletionFacts,
        lifetimeXpAfter: Int,
    ): Int = when (val rule = definition.rule) {
        is GoalRule.CompletionCount -> before.currentValue + 1
        is GoalRule.CategoryCompletion ->
            before.currentValue + if (completion.category == rule.category) 1 else 0
        is GoalRule.DifficultyCompletion ->
            before.currentValue + if (completion.difficulty == rule.difficulty) 1 else 0
        is GoalRule.BonusObjectiveCount ->
            before.currentValue + completion.completedBonusObjectiveCount
        is GoalRule.LifetimeXpMilestone -> lifetimeXpAfter
    }
}
