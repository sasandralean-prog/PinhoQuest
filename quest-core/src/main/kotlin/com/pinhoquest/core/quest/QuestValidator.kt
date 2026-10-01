package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestId
import java.util.UUID

enum class QuestValidationError {
    BLANK_TITLE,
    BLANK_DESCRIPTION,
    NO_OBJECTIVES,
    BLANK_OBJECTIVE,
    INVALID_DURATION,
    MISSING_DIFFICULTY,
}

sealed interface QuestValidationResult {
    data class Valid(val quest: Quest) : QuestValidationResult
    data class Invalid(val errors: Set<QuestValidationError>) : QuestValidationResult
}

fun interface QuestIdFactory {
    fun newId(): QuestId
}

class QuestValidator(
    private val idFactory: QuestIdFactory = QuestIdFactory {
        QuestId(UUID.randomUUID().toString())
    },
) {
    fun validate(draft: QuestDraft): QuestValidationResult {
        val errors = buildSet {
            if (draft.title.isBlank()) add(QuestValidationError.BLANK_TITLE)
            if (draft.description.isBlank()) add(QuestValidationError.BLANK_DESCRIPTION)
            if (draft.objectives.isEmpty()) add(QuestValidationError.NO_OBJECTIVES)
            if (draft.objectives.any { it.text.isBlank() }) add(QuestValidationError.BLANK_OBJECTIVE)
            if (
                draft.estimatedDuration.minMinutes <= 0 ||
                draft.estimatedDuration.maxMinutes < draft.estimatedDuration.minMinutes
            ) {
                add(QuestValidationError.INVALID_DURATION)
            }
            if (draft.difficulty == null) add(QuestValidationError.MISSING_DIFFICULTY)
        }
        if (errors.isNotEmpty()) return QuestValidationResult.Invalid(errors)

        return QuestValidationResult.Valid(
            Quest(
                id = idFactory.newId(),
                title = draft.title.trim(),
                description = draft.description.trim(),
                objectives = draft.objectives,
                category = draft.category,
                environment = draft.environment,
                estimatedDuration = draft.estimatedDuration,
                difficulty = requireNotNull(draft.difficulty),
            ),
        )
    }
}
