package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.prompt.BoundedPromptEnvelope
import com.pinhoquest.core.inference.prompt.PromptFactsAssembler
import com.pinhoquest.core.inference.prompt.PromptResearchHint
import com.pinhoquest.core.inference.prompt.PromptAvailability
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestMode

/**
 * P3 input boundary.
 *
 * Only the bounded envelope and sanitized approved examples produced here may
 * reach the model-facing serializer. No raw tag/example text is serialized
 * directly from the composition request.
 */
class MicroQuestPromptFactsAssembler(
    private val factsAssembler: PromptFactsAssembler = PromptFactsAssembler(),
    private val maxExamples: Int = 3,
) {
    init {
        require(maxExamples > 0)
    }

    fun assemble(
        plan: QuestGenerationPlan,
        tags: List<String>,
        examples: List<QuestTextExample>,
    ): BoundedMicroQuestPrompt {
        val researchHints = plan.gameCandidate
            ?.takeIf { plan.mode == QuestMode.GAME }
            ?.let { seed ->
                listOf(
                    PromptResearchHint.Game(
                        canonicalName = seed.title,
                        platform = seed.platforms.firstOrNull(),
                        genre = seed.genres.firstOrNull(),
                        availability = PromptAvailability.UNKNOWN,
                        focus = seed.variant?.objectivePattern,
                    ),
                )
            }
            .orEmpty()

        val envelope = factsAssembler.assembleTagLabels(
            plan = plan,
            tagLabels = tags,
            researchHints = researchHints,
        )
        val approvedExamples = examples
            .asSequence()
            .mapNotNull(::sanitizeExample)
            .take(maxExamples)
            .toList()

        return BoundedMicroQuestPrompt(
            envelope = envelope,
            examples = approvedExamples,
        )
    }

    private fun sanitizeExample(example: QuestTextExample): QuestTextExample? {
        val title = sanitize(example.title, MicroQuestToolContract.MAX_TITLE_LENGTH)
        val description = sanitize(example.description, MicroQuestToolContract.MAX_DESCRIPTION_LENGTH)
        val objectives = example.objectives
            .asSequence()
            .map { sanitize(it, MicroQuestToolContract.MAX_OBJECTIVE_LENGTH) }
            .filter { it.isNotBlank() }
            .take(MicroQuestToolContract.MAX_OBJECTIVES)
            .toList()

        if (title.isBlank() || description.isBlank() || objectives.isEmpty()) {
            return null
        }

        return QuestTextExample(title, description, objectives)
    }

    private fun sanitize(value: String, maxLength: Int): String =
        value
            .replace(Regex("\\p{Cntrl}+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(maxLength)
}

data class BoundedMicroQuestPrompt(
    val envelope: BoundedPromptEnvelope,
    val examples: List<QuestTextExample>,
)
