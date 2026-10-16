package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSessionFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestPromptSerializerTest {
    private val factsAssembler = MicroQuestPromptFactsAssembler()

    @Test
    fun keepsApprovedExamplesAndProvidesCategorySpecificTaskSeed() {
        val request = MicroQuestCompositionRequest(
            prompt = factsAssembler.assemble(
                plan = plan(),
                tags = listOf("natureza", "observacao"),
                examples = listOf(
                    QuestTextExample(
                        "Perto daqui",
                        "Observe um detalhe novo.",
                        listOf("Encontre um detalhe"),
                    ),
                ),
            ),
        )

        val prompt = MicroQuestPromptSerializer().serialize(request)

        assertTrue(prompt.contains("Categoria=EXPLORATION"))
        assertTrue(prompt.contains("Exemplos aprovados"))
        assertTrue(prompt.contains("Perto daqui"))
        assertTrue(prompt.contains("Fatos aprovados: Procure tres coisas ao seu redor"))
        assertTrue(prompt.contains("Objetivo aprovado: Encontre e registre tres detalhes interessantes."))
        assertTrue(prompt.contains("Tempo=10-20"))
        assertFalse(prompt.contains("RETORNO="))
        assertFalse(prompt.contains("XP="))
        assertFalse(prompt.contains("RECOMPENSA="))
    }

    @Test
    fun defaultTimeMatchesDurationShownForAnUnfilteredQuest() {
        val request = MicroQuestCompositionRequest(
            prompt = factsAssembler.assemble(
                plan = plan(
                    filters = QuestSessionFilters(),
                    environment = QuestEnvironment.ANYWHERE,
                ),
                tags = emptyList(),
                examples = emptyList(),
            ),
        )

        val prompt = MicroQuestPromptSerializer().serialize(request)

        assertTrue(prompt.contains("Tempo=15-30"))
        assertTrue(prompt.contains("Ambiente=ANY"))
        assertFalse(prompt.contains("Ambiente=ANYWHERE"))
        assertFalse(prompt.contains("Tempo=ANY"))
        assertFalse(prompt.contains("Exemplos aprovados"))
    }

    @Test
    fun everySupportedCategoryHasApprovedFactsAndAnActionableSeedObjective() {
        val serializer = MicroQuestPromptSerializer()

        QuestCategory.entries.forEach { category ->
            val prompt = serializer.serialize(
                MicroQuestCompositionRequest(
                    prompt = factsAssembler.assemble(
                        plan = plan(category = category),
                        tags = emptyList(),
                        examples = emptyList(),
                    ),
                ),
            )

            assertTrue("$category must include category facts", prompt.contains("Fatos aprovados: "))
            assertTrue("$category must include a seed objective", prompt.contains("Objetivo aprovado: "))
        }
    }

    @Test
    fun serializationIsDeterministic() {
        val request = MicroQuestCompositionRequest(
            prompt = factsAssembler.assemble(
                plan = plan(),
                tags = listOf("b", "a"),
                examples = emptyList(),
            ),
        )
        val serializer = MicroQuestPromptSerializer()
        assertEquals(serializer.serialize(request), serializer.serialize(request))
    }

    private fun plan(
        category: QuestCategory = QuestCategory.EXPLORATION,
        filters: QuestSessionFilters = QuestSessionFilters(minMinutes = 10, maxMinutes = 20),
        environment: QuestEnvironment = QuestEnvironment.OUTDOOR,
    ) = QuestGenerationPlan(
        mode = QuestMode.NORMAL,
        filters = filters,
        selectedCategory = category,
        selectedEnvironment = environment,
        affinityWeight = 1.0,
        gameCandidate = null,
    )
}
