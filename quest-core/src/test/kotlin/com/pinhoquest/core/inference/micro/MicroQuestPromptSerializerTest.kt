package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSessionFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestPromptSerializerTest {
    @Test
    fun keepsExamplesAndTrustedFieldsButNoGovernedRewards() {
        val request = MicroQuestCompositionRequest(
            plan = plan(),
            tags = listOf("natureza", "observacao"),
            examples = listOf(
                QuestTextExample("Perto daqui", "Observe um detalhe novo.", listOf("Encontre um detalhe")),
            ),
        )

        val prompt = MicroQuestPromptSerializer().serialize(request)

        assertTrue(prompt.contains("CATEGORIA=EXPLORATION"))
        assertTrue(prompt.contains("EXEMPLOS_APROVADOS"))
        assertTrue(prompt.contains("Perto daqui"))
        assertTrue(prompt.contains("RETORNO=chame a funcao compose_quest_text"))
        assertTrue(!prompt.contains("XP="))
        assertTrue(!prompt.contains("RECOMPENSA="))
    }

    @Test
    fun serializationIsDeterministic() {
        val request = MicroQuestCompositionRequest(plan(), tags = listOf("b", "a"))
        val serializer = MicroQuestPromptSerializer()
        assertEquals(serializer.serialize(request), serializer.serialize(request))
    }

    private fun plan() = QuestGenerationPlan(
        mode = QuestMode.NORMAL,
        filters = QuestSessionFilters(minMinutes = 10, maxMinutes = 20),
        selectedCategory = QuestCategory.EXPLORATION,
        selectedEnvironment = QuestEnvironment.OUTDOOR,
        affinityWeight = 1.0,
        gameCandidate = null,
    )
}
