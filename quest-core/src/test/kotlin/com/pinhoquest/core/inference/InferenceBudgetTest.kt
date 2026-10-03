package com.pinhoquest.core.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceBudgetTest {
    @Test
    fun p3BudgetCentralizesPreviouslyScatteredLimits() {
        assertEquals(1200, InferenceBudget.P3.maxPromptCharacters)
        assertEquals(1280, InferenceBudget.P3.maxContextTokens)
        assertEquals(128, InferenceBudget.P3.maxOutputTokens)
        assertEquals(1, InferenceBudget.P3.maxToolCalls)
    }

    @Test(expected = IllegalArgumentException::class)
    fun requestRejectsPromptAboveBudgetBeforeInference() {
        GenerationRequest(
            prompt = "x".repeat(InferenceBudget.P3.maxPromptCharacters + 1),
            budget = InferenceBudget.P3,
        )
    }

    @Test
    fun requestUsesTheSameBudgetForOutputAndContextGovernance() {
        val request = GenerationRequest(
            prompt = "quest prompt",
            budget = InferenceBudget.P3,
        )

        assertEquals(InferenceBudget.P3.maxOutputTokens, request.maxOutputTokens)
        assertEquals(InferenceBudget.P3, request.budget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroToolCallBudget() {
        InferenceBudget(
            maxPromptCharacters = 100,
            maxContextTokens = 128,
            maxOutputTokens = 32,
            maxToolCalls = 0,
        )
    }

    @Test
    fun budgetIsValueComparableAcrossLayers() {
        val sameValues = InferenceBudget(
            maxPromptCharacters = 1200,
            maxContextTokens = 1280,
            maxOutputTokens = 128,
            maxToolCalls = 1,
        )

        assertTrue(InferenceBudget.P3 == sameValues)
    }
}
