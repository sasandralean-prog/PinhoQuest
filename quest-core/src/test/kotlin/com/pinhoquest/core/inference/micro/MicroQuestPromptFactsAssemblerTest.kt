package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.quest.GameQuestSeed
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.core.research.GameIdentityKey
import com.pinhoquest.core.research.GameQuestActivity
import com.pinhoquest.core.research.GameQuestVariant
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSessionFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestPromptFactsAssemblerTest {
    private val assembler = MicroQuestPromptFactsAssembler()

    @Test
    fun tagsAreSanitizedSortedDeduplicatedAndBoundedBeforeSerialization() {
        val facts = assembler.assemble(
            plan(),
            tags = listOf(
                "  zeta\n  ",
                "Alpha",
                "alpha",
                "a".repeat(100),
                "ignored-1",
                "ignored-2",
                "ignored-3",
            ),
            examples = emptyList(),
        )

        assertEquals(
            listOf("a".repeat(32), "Alpha", "ignored-1", "ignored-2", "ignored-3", "zeta"),
            facts.envelope.selectedTags.map { it.label },
        )
        assertTrue(facts.envelope.selectedTags.size <= 6)
        assertFalse(facts.envelope.selectedTags.any { it.label.contains("\n") })
    }

    @Test
    fun examplesAreSanitizedAndBoundedBeforeTheyReachTheSerializer() {
        val facts = assembler.assemble(
            plan(),
            tags = emptyList(),
            examples = (1..5).map { index ->
                QuestTextExample(
                    title = "  Exemplo\n$index  ",
                    description = "  Descricao\t$index  ",
                    objectives = listOf(
                        " Objetivo $index ",
                        "  Segundo $index ",
                        "Terceiro $index",
                        "Quarto $index",
                        "Excedente $index",
                    ),
                )
            },
        )

        assertEquals(3, facts.examples.size)
        assertEquals("Exemplo 1", facts.examples.first().title)
        assertEquals("Descricao 1", facts.examples.first().description)
        assertEquals(4, facts.examples.first().objectives.size)
        assertTrue(facts.examples.all { it.title.length <= MicroQuestToolContract.MAX_TITLE_LENGTH })
        assertTrue(facts.examples.all {
            it.description.length <= MicroQuestToolContract.MAX_DESCRIPTION_LENGTH
        })
        assertTrue(facts.examples.all { example ->
            example.objectives.all {
                it.length <= MicroQuestToolContract.MAX_OBJECTIVE_LENGTH
            }
        })
    }

    @Test
    fun gamePlanCarriesCanonicalGameAndRandomSemanticFocusToModelBoundary() {
        val seed = GameQuestSeed(
            gameIdentity = GameIdentityKey("minecraft"),
            title = "Minecraft",
            environment = QuestEnvironment.ANDROID,
            cycleId = "cycle-1",
            variant = GameQuestVariant(
                activity = GameQuestActivity.DISCOVERY,
                category = QuestCategory.GAMING,
                environment = QuestEnvironment.ANDROID,
                objectivePattern = "sandbox: descobrir uma mecânica diferente",
            ),
            genres = listOf("Sandbox"),
            platforms = listOf("Android"),
        )

        val facts = assembler.assemble(
            plan().copy(
                mode = QuestMode.GAME,
                selectedCategory = QuestCategory.GAMING,
                selectedEnvironment = QuestEnvironment.ANDROID,
                gameCandidate = seed,
            ),
            tags = emptyList(),
            examples = emptyList(),
        )

        val serialized = MicroQuestPromptSerializer().serialize(
            MicroQuestCompositionRequest(prompt = facts),
        )

        assertEquals("Minecraft", facts.envelope.researchHints.single().let {
            (it as com.pinhoquest.core.inference.prompt.PromptResearchHint.Game).canonicalName
        })
        assertTrue(serialized.contains("GAME(name=Minecraft"))
        assertTrue(serialized.contains("foco=sandbox: descobrir uma mecânica diferente"))
    }

    @Test
    fun boundedEnvelopeDoesNotCarryRawExampleControlCharacters() {
        val facts = assembler.assemble(
            plan(),
            tags = listOf("tag\u0000com\u0001controle"),
            examples = listOf(
                QuestTextExample(
                    "Titulo\u0000",
                    "Descricao\u0001",
                    listOf("Objetivo\u0002"),
                ),
            ),
        )

        val serialized = MicroQuestPromptSerializer().serialize(
            MicroQuestCompositionRequest(prompt = facts),
        )

        assertFalse(serialized.contains("\u0000"))
        assertFalse(serialized.contains("\u0001"))
        assertFalse(serialized.contains("\u0002"))
        assertTrue(serialized.contains("tag com controle"))
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
