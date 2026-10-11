package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest

enum class GenerationUnavailableReason {
    GAME_CANDIDATE_REQUIRED,
    GAME_CATALOG_NOT_CONFIGURED,
    GAME_CATALOG_UNAVAILABLE,
    GAME_CATALOG_NOT_FRESH,
    FILTERS_UNSATISFIABLE,
}

sealed interface QuestGenerationResult {
    data class Success(val quest: Quest) : QuestGenerationResult
    data class InvalidDraft(val errors: Set<QuestValidationError>) : QuestGenerationResult
    data class Unavailable(val reason: GenerationUnavailableReason) : QuestGenerationResult
}

enum class QuestGenerationStatus { SUCCESS, INVALID_DRAFT, UNAVAILABLE }

data class QuestGenerationDiagnostic(
    val mode: QuestMode,
    val category: QuestCategory?,
    val origin: QuestCompositionOrigin,
    val status: QuestGenerationStatus,
    /** Enum-like reason only; never include prompts, profile values, or generated text. */
    val reason: String? = null,
)

fun interface QuestGenerationObserver {
    fun onDiagnostic(diagnostic: QuestGenerationDiagnostic)
}

class QuestEngine(
    private val planner: QuestPlanner,
    private val composer: ComposerPort,
    private val validator: QuestValidator,
    private val fallbackComposer: ComposerPort = ProceduralComposer(),
    private val observer: QuestGenerationObserver = QuestGenerationObserver {},
) {
    suspend fun generate(
        request: QuestRequest,
        context: QuestContext,
    ): QuestGenerationResult {
        val plan = planner.plan(request, context)
        if (
            request.filters.categories.isNotEmpty() &&
            plan.selectedCategory !in request.filters.categories
        ) {
            return unavailable(
                request = request,
                category = plan.selectedCategory,
                reason = GenerationUnavailableReason.FILTERS_UNSATISFIABLE,
            )
        }
        if (request.mode == QuestMode.GAME && plan.gameCandidate == null) {
            return unavailable(
                request = request,
                category = plan.selectedCategory,
                reason = GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED,
            )
        }
        if (
            plan.selectedCategory == QuestCategory.GAMING &&
            plan.gameCandidate == null
        ) {
            return unavailable(
                request = request,
                category = plan.selectedCategory,
                reason = GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED,
            )
        }

        val composed = composer.composeWithOutcome(plan)
        return when (val validated = validator.validate(composed.draft)) {
            is QuestValidationResult.Valid -> {
                observe(
                    QuestGenerationDiagnostic(
                        mode = request.mode,
                        category = plan.selectedCategory,
                        origin = composed.origin,
                        status = QuestGenerationStatus.SUCCESS,
                        reason = composed.fallbackReason?.name,
                    ),
                )
                QuestGenerationResult.Success(validated.quest)
            }

            is QuestValidationResult.Invalid -> {
                // A model-originated draft that fails domain validation is not success.
                // Try the governed procedural composer and validate that draft independently.
                if (composed.origin == QuestCompositionOrigin.LOCAL_MODEL) {
                    val fallback = fallbackComposer.composeWithOutcome(plan).copy(
                        origin = QuestCompositionOrigin.PROCEDURAL_FALLBACK,
                        fallbackReason = QuestFallbackReason.INVALID_MODEL_OUTPUT,
                    )
                    when (val fallbackValidation = validator.validate(fallback.draft)) {
                        is QuestValidationResult.Valid -> {
                            observe(
                                QuestGenerationDiagnostic(
                                    mode = request.mode,
                                    category = plan.selectedCategory,
                                    origin = fallback.origin,
                                    status = QuestGenerationStatus.SUCCESS,
                                    reason = fallback.fallbackReason?.name,
                                ),
                            )
                            QuestGenerationResult.Success(fallbackValidation.quest)
                        }

                        is QuestValidationResult.Invalid -> {
                            observe(
                                QuestGenerationDiagnostic(
                                    mode = request.mode,
                                    category = plan.selectedCategory,
                                    origin = fallback.origin,
                                    status = QuestGenerationStatus.INVALID_DRAFT,
                                    reason = fallbackValidation.errors
                                        .map { it.name }
                                        .sorted()
                                        .joinToString(","),
                                ),
                            )
                            QuestGenerationResult.InvalidDraft(fallbackValidation.errors)
                        }
                    }
                } else {
                    observe(
                        QuestGenerationDiagnostic(
                            mode = request.mode,
                            category = plan.selectedCategory,
                            origin = composed.origin,
                            status = QuestGenerationStatus.INVALID_DRAFT,
                            reason = validated.errors.map { it.name }.sorted().joinToString(","),
                        ),
                    )
                    QuestGenerationResult.InvalidDraft(validated.errors)
                }
            }
        }
    }

    private fun unavailable(
        request: QuestRequest,
        category: QuestCategory,
        reason: GenerationUnavailableReason,
    ): QuestGenerationResult.Unavailable {
        observe(
            QuestGenerationDiagnostic(
                mode = request.mode,
                category = category,
                origin = QuestCompositionOrigin.UNKNOWN,
                status = QuestGenerationStatus.UNAVAILABLE,
                reason = reason.name,
            ),
        )
        return QuestGenerationResult.Unavailable(reason)
    }

    private fun observe(diagnostic: QuestGenerationDiagnostic) {
        // Observability must never change quest-generation correctness.
        runCatching { observer.onDiagnostic(diagnostic) }
    }
}
