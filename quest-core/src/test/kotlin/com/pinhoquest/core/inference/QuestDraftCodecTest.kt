package com.pinhoquest.core.inference

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSessionFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestDraftCodecTest {
    private val codec = QuestDraftCodec()

    @Test
    fun acceptsJsonWrappedByRuntimeProseAndUsesTrustedPlanMetadata() {
        val raw = """
            <|im_start|>assistant
            ```json
            {"title":"Quest","description":"Desc","objectives":["Do it"],"bonusObjectives":["Bonus"],"estimatedMinutes":20,"estimatedDifficulty":"EASY"}
            ```
            <|im_end|>
        """.trimIndent()

        val draft = codec.decode(raw, plan())

        assertEquals("Quest", draft.title)
        assertEquals("Desc", draft.description)
        assertEquals(2, draft.objectives.size)
        assertEquals(QuestCategory.CODING, draft.category)
        assertEquals(QuestEnvironment.WINDOWS, draft.environment)
        assertEquals(20, draft.estimatedDuration.minMinutes)
        assertEquals("EASY", draft.difficulty?.name)
        assertTrue(draft.objectives.last().optional)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingField() {
        codec.decode("""{"title":"Quest","description":"Desc","objectives":["Do it"],"estimatedMinutes":20,"estimatedDifficulty":"EASY"}""", plan())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedPayload() {
        codec.decode("""{"title":"Quest","description":""", plan())
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedDifficultyAndOutOfRangeTime() {
        codec.decode(
            """{"title":"Quest","description":"Desc","objectives":["Do"],"bonusObjectives":[],"estimatedMinutes":99,"estimatedDifficulty":"HARD"}""",
            plan(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongJsonTypes() {
        codec.decode(
            """{"title":123,"description":"Desc","objectives":["Do"],"bonusObjectives":[],"estimatedMinutes":20,"estimatedDifficulty":"EASY"}""",
            plan(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExtraOutputFields() {
        codec.decode(
            """{"title":"Quest","description":"Desc","objectives":["Do"],"bonusObjectives":[],"estimatedMinutes":20,"estimatedDifficulty":"EASY","category":"GAMING"}""",
            plan(),
        )
    }

    private fun plan(): QuestGenerationPlan = QuestGenerationPlan(
        mode = QuestMode.NORMAL,
        filters = QuestSessionFilters(
            minMinutes = 15,
            maxMinutes = 30,
            desiredDifficulty = com.pinhoquest.domain.quest.QuestDifficulty.EASY,
        ),
        selectedCategory = QuestCategory.CODING,
        selectedEnvironment = QuestEnvironment.WINDOWS,
        affinityWeight = 1.0,
        gameCandidate = null,
    )
}