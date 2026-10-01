package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest

enum class GenerationUnavailableReason {
    GAME_CANDIDATE_REQUIRED,
}

sealed interface QuestGenerationResult {
    data class Success(val quest: Quest) : QuestGenerationResult
    data class InvalidDraft(val errors: Set<QuestValidationError>) : QuestGenerationResult
    data class Unavailable(val reason: GenerationUnavailableReason) : QuestGenerationResult
}

class QuestEngine(
    private val planner: QuestPlanner,
    private val composer: ComposerPort,
    private val validator: QuestValidator,
) {
    suspend fun generate(
        request: QuestRequest,
        context: QuestContext,
    ): QuestGenerationResult {
        val plan = planner.plan(request, context)
        if (request.mode == QuestMode.GAME && plan.gameCandidate == null) {
            return QuestGenerationResult.Unavailable(
                GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED,
            )
        }

        return when (val validated = validator.validate(composer.compose(plan))) {
            is QuestValidationResult.Valid -> QuestGenerationResult.Success(validated.quest)
            is QuestValidationResult.Invalid -> QuestGenerationResult.InvalidDraft(validated.errors)
        }
    }
}
