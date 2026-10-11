package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceBudget
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.LocalInferencePort
import com.pinhoquest.core.quest.ComposerPort
import com.pinhoquest.core.quest.QuestCompositionOrigin
import com.pinhoquest.core.quest.QuestCompositionOutcome
import com.pinhoquest.core.quest.QuestFallbackReason
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestDraft

class MicroQuestComposer(
    private val inference: LocalInferencePort,
    private val budget: InferenceBudget = InferenceBudget.P3,
    private val promptFactsAssembler: MicroQuestPromptFactsAssembler = MicroQuestPromptFactsAssembler(),
    private val promptSerializer: MicroQuestPromptSerializer = MicroQuestPromptSerializer(budget),
    private val toolCallDecoder: MicroQuestToolCallDecoder = MicroQuestToolCallDecoder(),
    private val renderer: MicroQuestRenderer = MicroQuestRenderer(),
    private val fallback: ComposerPort = ProceduralComposer(),
) : ComposerPort {
    override suspend fun compose(plan: QuestGenerationPlan): QuestDraft =
        composeWithOutcome(plan).draft

    override suspend fun composeWithOutcome(plan: QuestGenerationPlan): QuestCompositionOutcome {
        val rendered = composeWithDetails(plan)
        return QuestCompositionOutcome(
            draft = rendered.draft,
            origin = when (rendered.origin) {
                MicroQuestOrigin.LOCAL_MODEL -> QuestCompositionOrigin.LOCAL_MODEL
                MicroQuestOrigin.PROCEDURAL_FALLBACK -> QuestCompositionOrigin.PROCEDURAL_FALLBACK
            },
            fallbackReason = rendered.fallbackReason,
        )
    }

    suspend fun composeWithDetails(
        plan: QuestGenerationPlan,
        tags: List<String> = emptyList(),
        examples: List<QuestTextExample> = emptyList(),
    ): RenderedMicroQuest {
        val boundedPrompt = promptFactsAssembler.assemble(
            plan = plan,
            tags = tags,
            examples = examples,
        )
        val request = MicroQuestCompositionRequest(prompt = boundedPrompt)
        val prompt = promptSerializer.serialize(request)

        return when (val outcome = inference.generate(
            GenerationRequest(prompt = prompt, budget = budget),
        )) {
            is InferenceOutcome.ToolCall -> {
                runCatching { toolCallDecoder.decode(outcome) }
                    .map { text ->
                        RenderedMicroQuest(
                            draft = renderer.render(text, plan),
                            origin = MicroQuestOrigin.LOCAL_MODEL,
                        )
                    }
                    .getOrElse {
                        RenderedMicroQuest(
                            draft = fallback.composeWithOutcome(plan).draft,
                            origin = MicroQuestOrigin.PROCEDURAL_FALLBACK,
                            fallbackReason = QuestFallbackReason.INVALID_MODEL_OUTPUT,
                        )
                    }
            }

            else -> RenderedMicroQuest(
                draft = fallback.composeWithOutcome(plan).draft,
                origin = MicroQuestOrigin.PROCEDURAL_FALLBACK,
                fallbackReason = QuestFallbackReason.INFERENCE_FAILED,
            )
        }
    }
}
