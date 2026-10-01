package com.pinhoquest.core.progression

import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalId
import com.pinhoquest.domain.progression.GoalProgress
import com.pinhoquest.domain.progression.GoalRule
import com.pinhoquest.domain.progression.QuestCompletionFacts
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalEvaluatorTest {
    private val goals = listOf(
        GoalDefinition(GoalId("all-2"), "Duas quests", GoalRule.CompletionCount(2)),
        GoalDefinition(GoalId("coding-1"), "Uma de código", GoalRule.CategoryCompletion(QuestCategory.CODING, 1)),
        GoalDefinition(GoalId("challenge-1"), "Um desafio", GoalRule.DifficultyCompletion(QuestDifficulty.CHALLENGE, 1)),
        GoalDefinition(GoalId("bonus-2"), "Dois bônus", GoalRule.BonusObjectiveCount(2)),
        GoalDefinition(GoalId("xp-100"), "100 XP", GoalRule.LifetimeXpMilestone(100)),
    )
    private val evaluator = GoalEvaluator(goals)

    @Test
    fun categoryDifficultyCountBonusAndXpGoalsAdvanceFromOneCompletion() {
        val completion = facts(
            id = "c1",
            category = QuestCategory.CODING,
            difficulty = QuestDifficulty.CHALLENGE,
            completedBonus = 2,
        )

        val result = evaluator.evaluate(
            completion = completion,
            current = emptyList(),
            lifetimeXpAfter = 100,
        )

        assertEquals(1, result.progress.single { it.goalId.value == "all-2" }.currentValue)
        assertTrue(result.progress.single { it.goalId.value == "coding-1" }.completed)
        assertTrue(result.progress.single { it.goalId.value == "challenge-1" }.completed)
        assertTrue(result.progress.single { it.goalId.value == "bonus-2" }.completed)
        assertTrue(result.progress.single { it.goalId.value == "xp-100" }.completed)
        assertEquals(
            setOf("coding-1", "challenge-1", "bonus-2", "xp-100"),
            result.newlyCompletedGoalIds.map { it.value }.toSet(),
        )
    }

    @Test
    fun repeatedEvaluationOfSameCompletionIsIdempotent() {
        val completion = facts("c1")
        val first = evaluator.evaluate(completion, emptyList(), lifetimeXpAfter = 50)
        val second = evaluator.evaluate(completion, first.progress, lifetimeXpAfter = 50)

        val firstCount = first.progress.single { it.goalId.value == "all-2" }
        val secondCount = second.progress.single { it.goalId.value == "all-2" }

        assertEquals(1, firstCount.currentValue)
        assertEquals(firstCount, secondCount)
        assertTrue(second.newlyCompletedGoalIds.isEmpty())
    }

    @Test
    fun unrelatedCategoryAndDifficultyDoNotAdvanceConditionalGoals() {
        val result = evaluator.evaluate(
            completion = facts(
                id = "c2",
                category = QuestCategory.LEARNING,
                difficulty = QuestDifficulty.EASY,
                completedBonus = 0,
            ),
            current = emptyList(),
            lifetimeXpAfter = 25,
        )

        assertEquals(0, result.progress.single { it.goalId.value == "coding-1" }.currentValue)
        assertEquals(0, result.progress.single { it.goalId.value == "challenge-1" }.currentValue)
        assertEquals(0, result.progress.single { it.goalId.value == "bonus-2" }.currentValue)
        assertEquals(25, result.progress.single { it.goalId.value == "xp-100" }.currentValue)
    }

    private fun facts(
        id: String,
        category: QuestCategory = QuestCategory.CODING,
        difficulty: QuestDifficulty = QuestDifficulty.MEDIUM,
        completedBonus: Int = 0,
    ): QuestCompletionFacts {
        val main = setOf(ObjectiveId("main"))
        val bonus = (1..2).map { ObjectiveId("bonus-$it") }.toSet()
        return QuestCompletionFacts(
            completionId = CompletionId(id),
            questId = QuestId("quest-$id"),
            category = category,
            difficulty = difficulty,
            mainObjectiveIds = main,
            bonusObjectiveIds = bonus,
            completedObjectiveIds = main + bonus.take(completedBonus),
        )
    }
}
