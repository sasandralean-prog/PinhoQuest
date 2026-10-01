package com.pinhoquest.data.progression

import com.pinhoquest.domain.progression.GoalRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedGoalLoaderTest {
    @Test
    fun initialGoalSetIsVersionedUniqueAndCoversV1RuleFamilies() {
        val loaded = EmbeddedGoalLoader.loadV1()

        assertEquals(1, loaded.version)
        assertTrue(loaded.goals.isNotEmpty())
        assertEquals(loaded.goals.size, loaded.goals.map { it.id }.distinct().size)
        assertTrue(loaded.goals.all { it.rule.target > 0 })

        val ruleTypes = loaded.goals.map { it.rule::class }.toSet()
        assertTrue(GoalRule.CompletionCount::class in ruleTypes)
        assertTrue(GoalRule.CategoryCompletion::class in ruleTypes)
        assertTrue(GoalRule.DifficultyCompletion::class in ruleTypes)
        assertTrue(GoalRule.BonusObjectiveCount::class in ruleTypes)
        assertTrue(GoalRule.LifetimeXpMilestone::class in ruleTypes)
    }
}
