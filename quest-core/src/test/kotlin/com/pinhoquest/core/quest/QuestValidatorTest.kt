package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestObjective
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestValidatorTest {
    private val validator = QuestValidator(
        idFactory = QuestIdFactory { QuestId("quest-fixed") },
    )

    @Test
    fun blankTitleNeverBecomesQuest() {
        val result = validator.validate(validDraft(title = " "))

        assertTrue(result is QuestValidationResult.Invalid)
    }

    @Test
    fun emptyObjectivesNeverBecomeQuest() {
        val result = validator.validate(validDraft(objectives = emptyList()))

        assertTrue(result is QuestValidationResult.Invalid)
    }

    @Test
    fun invalidDurationNeverBecomesQuest() {
        val result = validator.validate(
            validDraft(estimatedDuration = EstimatedDuration(30, 10)),
        )

        assertTrue(result is QuestValidationResult.Invalid)
    }

    @Test
    fun missingDifficultyNeverBecomesQuest() {
        val result = validator.validate(validDraft(difficulty = null))

        assertTrue(result is QuestValidationResult.Invalid)
    }

    private fun validDraft(
        title: String = "Quest de teste",
        objectives: List<QuestObjective> = listOf(
            QuestObjective(ObjectiveId("objective-1"), "Faça uma coisa"),
        ),
        estimatedDuration: EstimatedDuration = EstimatedDuration(10, 20),
        difficulty: QuestDifficulty? = QuestDifficulty.EASY,
    ) = QuestDraft(
        title = title,
        description = "Uma proposta simples e divertida.",
        objectives = objectives,
        category = QuestCategory.CREATIVE,
        environment = QuestEnvironment.ANYWHERE,
        estimatedDuration = estimatedDuration,
        difficulty = difficulty,
    )
}
