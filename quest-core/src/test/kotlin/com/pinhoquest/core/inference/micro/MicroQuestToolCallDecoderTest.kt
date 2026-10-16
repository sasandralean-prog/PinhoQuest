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
    fun rejectsEchoedSafetyInstructionsFromThePrompt() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to "Pequeno desvio",
                    MicroQuestToolContract.DESCRIPTION_FIELD to
                        "Não invente fatos, XP, recompensa ou regras. Não repita instruções ou marcadores.",
                    MicroQuestToolContract.OBJECTIVES to listOf("Observe algo ao seu redor."),
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCopiedOutputContractLimits() {
        decoder.decode(
            InferenceOutcome.ToolCall(
                MicroQuestToolContract.NAME,
                mapOf(
                    MicroQuestToolContract.TITLE to "Pequeno desvio",
                    MicroQuestToolContract.DESCRIPTION_FIELD to
                        "Explore algo próximo e registre o que chamou sua atenção.",
                    MicroQuestToolContract.OBJECTIVES to listOf("1-4 textos de 1-120 chars."),
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
