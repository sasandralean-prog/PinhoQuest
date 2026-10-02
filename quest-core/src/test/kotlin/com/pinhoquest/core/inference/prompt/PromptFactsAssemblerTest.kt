package com.pinhoquest.core.inference.prompt

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSessionFilters
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFactsAssemblerTest {
    @Test
    fun selectsOnlyEnabledBoundedTagsWithoutAffinityOrSource() {
        val plan = plan()
        val tags = listOf(
            Tag(TagId("z"), "Music", TagSource.USER, affinity = 0.99, enabled = true),
            Tag(TagId("a"), "coding", TagSource.SYSTEM, affinity = 0.01, enabled = true),
            Tag(TagId("b"), "music", TagSource.DERIVED, affinity = 0.5, enabled = true),
            Tag(TagId("off"), "Disabled", TagSource.USER, enabled = false),
        )

        val envelope = PromptFactsAssembler(
            PromptContractLimits(maxTags = 2),
        ).assemble(plan, tags)

        assertEquals(listOf("coding", "Music"), envelope.selectedTags.map { it.label })
        assertEquals(2, envelope.selectedTags.size)
    }

    @Test
    fun researchIsAllowlistedSanitizedAndBounded() {
        val plan = plan()
        val hints = listOf(
            PromptResearchHint.Game(
                canonicalName = "  Game\nName  ",
                platform = " Android\t",
                genre = "Puzzle",
                availability = PromptAvailability.FREE,
            ),
            PromptResearchHint.Flower(
                canonicalName = "Second",
                commonName = "<raw>",
                region = "Brazil",
            ),
            PromptResearchHint.Game("Third", "Windows", null, PromptAvailability.UNKNOWN),
        )

        val envelope = PromptFactsAssembler(
            PromptContractLimits(maxResearchHints = 2, maxResearchFieldCharacters = 12),
        ).assemble(plan, researchHints = hints)

        assertEquals(2, envelope.researchHints.size)
        assertEquals("Game Name", envelope.researchHints[0].canonicalName)
        assertEquals("Second", envelope.researchHints[1].canonicalName)
        assertTrue(envelope.researchHints.none { it.canonicalName.contains("\n") })
    }

    @Test
    fun serializationIsDeterministicAndContainsOnlyContractFields() {
        val assembler = PromptFactsAssembler()
        val serializer = CompactPromptSerializer()
        val envelope = assembler.assemble(
            plan(),
            tags = listOf(Tag(TagId("x"), "photography", TagSource.USER, affinity = 0.77)),
        )

        val first = serializer.serialize(envelope)
        val second = serializer.serialize(envelope)

        assertEquals(first, second)
        assertTrue(first.contains("\"task\":\"QUEST_COMPOSE\""))
        assertTrue(first.contains("\"output\":["))
        assertFalse(first.contains("0.77"))
        assertFalse(first.contains("TagId"))
        assertFalse(first.contains("source"))
    }
    @Test
    fun serializerRejectsPromptOutsideConfiguredBudget() {
        val assembler = PromptFactsAssembler(
            PromptContractLimits(maxTagCharacters = 32, maxTags = 6),
        )
        val envelope = assembler.assemble(
            plan(),
            tags = (1..6).map {
                Tag(TagId("$it"), "a".repeat(32), TagSource.USER)
            },
            researchHints = listOf(
                PromptResearchHint.Game("b".repeat(48), "Android", "Puzzle", PromptAvailability.FREE),
                PromptResearchHint.Flower("c".repeat(48), "Flower", "Brazil"),
            ),
        )

        val exception = runCatching {
            CompactPromptSerializer(PromptContractLimits(maxPromptCharacters = 120)).serialize(envelope)
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun tagOrderingAndResearchOrderingAreStableAcrossInputOrder() {
        val assembler = PromptFactsAssembler()
        val tags = listOf(
            Tag(TagId("2"), "Zulu", TagSource.USER),
            Tag(TagId("1"), "alpha", TagSource.USER),
        )
        val hints = listOf(
            PromptResearchHint.Game("Zulu", "Android", null, PromptAvailability.UNKNOWN),
            PromptResearchHint.Game("Alpha", "Android", null, PromptAvailability.FREE),
        )

        val first = assembler.assemble(plan(), tags, hints)
        val second = assembler.assemble(plan(), tags.reversed(), hints.reversed())

        assertEquals(first, second)
    }

    private fun plan(): QuestGenerationPlan = QuestGenerationPlan(
        mode = QuestMode.NORMAL,
        filters = QuestSessionFilters(
            minMinutes = 15,
            maxMinutes = 30,
        ),
        selectedCategory = QuestCategory.CREATIVE,
        selectedEnvironment = QuestEnvironment.ANYWHERE,
        affinityWeight = 1.0,
        gameCandidate = null,
    )
}