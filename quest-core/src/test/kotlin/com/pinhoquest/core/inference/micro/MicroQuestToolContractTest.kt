package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.prompt.QuestPromptContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroQuestToolContractTest {
    @Test
    fun declaresExactlyTheThreeCreativeArguments() {
        assertEquals("compose_quest_text", MicroQuestToolContract.NAME)
        assertEquals(
            listOf("title", "description", "objectives"),
            MicroQuestToolContract.REQUIRED_ARGUMENTS,
        )
        assertEquals(
            MicroQuestToolContract.REQUIRED_ARGUMENTS,
            MicroQuestToolContract.ARGUMENTS.map { it.name },
        )
        assertTrue(MicroQuestToolContract.ARGUMENTS.all { it.required })
    }

    @Test
    fun boundsAreCanonicalAndTyped() {
        val title = MicroQuestToolContract.ARGUMENTS.first { it.name == MicroQuestToolContract.TITLE }
        val description = MicroQuestToolContract.ARGUMENTS.first {
            it.name == MicroQuestToolContract.DESCRIPTION_FIELD
        }
        val objectives = MicroQuestToolContract.ARGUMENTS.first {
            it.name == MicroQuestToolContract.OBJECTIVES
        }

        assertEquals(MicroQuestToolContract.ArgumentType.STRING, title.type)
        assertEquals(1, title.minLength)
        assertEquals(80, title.maxLength)

        assertEquals(MicroQuestToolContract.ArgumentType.STRING, description.type)
        assertEquals(12, description.minLength)
        assertEquals(220, description.maxLength)

        assertEquals(MicroQuestToolContract.ArgumentType.STRING_LIST, objectives.type)
        assertEquals(1, objectives.minItems)
        assertEquals(4, objectives.maxItems)
        assertEquals(1, objectives.itemMinLength)
        assertEquals(120, objectives.itemMaxLength)
    }

    @Test
    fun functionGemmaContractDoesNotOwnDeterministicDraftMetadata() {
        val names = MicroQuestToolContract.ARGUMENTS.map { it.name }.toSet()

        assertFalse(names.contains("category"))
        assertFalse(names.contains("environment"))
        assertFalse(names.contains("estimatedMinutes"))
        assertFalse(names.contains("estimatedDifficulty"))
        assertFalse(names.contains("bonusObjectives"))
        assertFalse(names.contains("xp"))
        assertFalse(names.contains("reward"))
    }

    @Test
    fun structuredSixFieldDraftContractRemainsASeparateProtocol() {
        assertEquals(
            listOf(
                "title",
                "description",
                "objectives",
                "bonusObjectives",
                "estimatedMinutes",
                "estimatedDifficulty",
            ),
            QuestPromptContract.OUTPUT_FIELDS,
        )
        assertTrue(
            QuestPromptContract.OUTPUT_FIELDS.containsAll(
                MicroQuestToolContract.REQUIRED_ARGUMENTS,
            ),
        )
        assertEquals(
            3,
            QuestPromptContract.OUTPUT_FIELDS.toSet().subtract(
                MicroQuestToolContract.REQUIRED_ARGUMENTS.toSet(),
            ).size,
        )
    }
}
