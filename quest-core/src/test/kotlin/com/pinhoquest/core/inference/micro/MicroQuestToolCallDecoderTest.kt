package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class MicroQuestToolCallDecoderTest {
    private val decoder = MicroQuestToolCallDecoder()

    @Test
    fun decodesCanonicalArgumentsWithoutRawText() {
        val result = decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to "Explorar",
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Procure um detalhe novo ao seu redor.",
                    MicroQuestToolContract.OBJECTIVES to listOf("Encontre um detalhe"),
                ),
            ),
        )

        assertEquals(
            MicroQuestText(
                "Explorar",
                "Procure um detalhe novo ao seu redor.",
                listOf("Encontre um detalhe"),
            ),
            result,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNullTitle() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to null,
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Descricao valida para a quest.",
                    MicroQuestToolContract.OBJECTIVES to listOf("Fazer"),
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonStringTitle() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to 7,
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Descricao valida para a quest.",
                    MicroQuestToolContract.OBJECTIVES to listOf("Fazer"),
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyObjectives() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to "Titulo",
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Descricao valida para a quest.",
                    MicroQuestToolContract.OBJECTIVES to emptyList<String>(),
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfBoundsDescription() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to "Titulo",
                    MicroQuestToolContract.DESCRIPTION_FIELD to "curta",
                    MicroQuestToolContract.OBJECTIVES to listOf("Fazer"),
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedToolName() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                "other_tool",
                mapOf(
                    MicroQuestToolContract.TITLE to "Titulo",
                    MicroQuestToolContract.DESCRIPTION_FIELD to "Descricao valida para a quest.",
                    MicroQuestToolContract.OBJECTIVES to listOf("Fazer"),
                ),
            ),
        )
    }
}
