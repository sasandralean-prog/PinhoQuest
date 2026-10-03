package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceBudget
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.LocalInferencePort
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.quest.QuestValidationResult
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestComposerPipelineGateTest {
    private val plan: QuestGenerationPlan = QuestPlanner().plan(
        QuestRequest(QuestMode.NORMAL),
        QuestContext(),
    )

    @Test
    fun nativeFunctionCallConvergesThroughRendererAndValidator() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.ToolCall(
                name = MicroQuestToolContract.NAME,
                arguments = canonicalArguments(),
            )
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.LOCAL_MODEL, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
        assertEquals("Caca aos detalhes", rendered.draft.title)
        assertEquals(2, rendered.draft.objectives.size)
    }

    @Test
    fun invalidNativeArgumentsConvergeToProceduralFallback() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.ToolCall(
                name = MicroQuestToolContract.NAME,
                arguments = mapOf(
                    MicroQuestToolContract.TITLE to "Titulo valido",
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Descricao valida para a quest.",
                    MicroQuestToolContract.OBJECTIVES to emptyList<String>(),
                ),
            )
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.PROCEDURAL_FALLBACK, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
    }

    @Test
    fun nonToolOutcomeConvergesToProceduralFallback() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.TechnicalFailure
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.PROCEDURAL_FALLBACK, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
    }

    @Test
    fun composerRequestsOnlyTheConfiguredNativeGenerationProtocol() = runTest {
        var received: GenerationRequest? = null
        val inference = LocalInferencePort {
            received = it
            InferenceOutcome.TechnicalFailure
        }

        MicroQuestComposer(inference).composeWithDetails(plan)

        assertTrue(received!!.prompt.isNotBlank())
        assertEquals(InferenceBudget.P3, received!!.budget)
        assertEquals(InferenceBudget.P3.maxOutputTokens, received!!.maxOutputTokens)
    }

    private fun canonicalArguments(): Map<String, Any?> = mapOf(
        MicroQuestToolContract.TITLE to "Caca aos detalhes",
        MicroQuestToolContract.DESCRIPTION_FIELD to "Encontre tres detalhes novos ao seu redor.",
        MicroQuestToolContract.OBJECTIVES to listOf(
            "Observe o ambiente",
            "Anote tres detalhes",
        ),
    )
}
