package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.LocalInferencePort
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestValidationResult
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestComposerPipelineGateTest {
    private val plan = QuestPlanner().plan(
        QuestRequest(QuestMode.NORMAL),
        QuestContext(),
    )

    @Test
    fun nativeFunctionCallConvergesThroughRendererAndValidator() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.Success(
                "<start_function_call>call:compose_quest_text{" +
                    "\"title\":\"Caca aos detalhes\"," +
                    "\"description\":\"Encontre tres detalhes novos ao seu redor.\"," +
                    "\"objectives\":[\"Observe o ambiente\",\"Anote tres detalhes\"]" +
                    "}<end_function_call>",
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
    fun jsonModelOutputConvergesThroughSanitizerRendererAndValidator() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.Success(
                """{"title":"Curiosidade de bolso","description":"Escolha uma pergunta pequena que sempre ficou sem resposta.","objectives":["Descubra uma resposta","Explique com suas palavras"]}""",
            )
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.LOCAL_MODEL, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
        assertEquals("Curiosidade de bolso", rendered.draft.title)
    }

    @Test
    fun malformedModelOutputFallsBackAndStillConvergesThroughValidator() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.Success(
                "<start_function_call>call:compose_quest_text{\"title\":\"x\"}<end_function_call>",
            )
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.PROCEDURAL_FALLBACK, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
    }

    @Test
    fun inferenceFailureFallsBackAndStillConvergesThroughValidator() = runTest {
        val inference = LocalInferencePort {
            InferenceOutcome.TechnicalFailure
        }

        val rendered = MicroQuestComposer(inference).composeWithDetails(plan)
        val validated = QuestValidator().validate(rendered.draft)

        assertEquals(MicroQuestOrigin.PROCEDURAL_FALLBACK, rendered.origin)
        assertTrue(validated is QuestValidationResult.Valid)
    }
}
