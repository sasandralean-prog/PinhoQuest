package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.LocalInferencePort
import com.pinhoquest.core.quest.ComposerPort
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestDraft

class MicroQuestComposer(
    private val inference: LocalInferencePort,
    private val promptFactsAssembler: MicroQuestPromptFactsAssembler = MicroQuestPromptFactsAssembler(),
    private val promptSerializer: MicroQuestPromptSerializer = MicroQuestPromptSerializer(),
    private val sanitizer: FunctionGemmaResponseSanitizer = FunctionGemmaResponseSanitizer(),
    private val toolCallExtractor: FunctionGemmaToolCallExtractor = FunctionGemmaToolCallExtractor(),
    private val renderer: MicroQuestRenderer = MicroQuestRenderer(),
    private val fallback: ComposerPort = ProceduralComposer(),
) : ComposerPort {
    override suspend fun compose(plan: QuestGenerationPlan): QuestDraft =
        composeWithDetails(plan).draft

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
            GenerationRequest(prompt = prompt, maxOutputTokens = 128),
        )) {
            is InferenceOutcome.Success -> {
                runCatching {
                    renderer.render(toolCallExtractor.extract(outcome.text), plan)
                }.map { draft ->
                    RenderedMicroQuest(
                        draft = draft,
                        origin = MicroQuestOrigin.LOCAL_MODEL,
                    )
                }.getOrElse {
                    runCatching {
                        renderer.render(sanitizer.extract(outcome.text), plan)
                    }.map { draft ->
                        RenderedMicroQuest(
                            draft = draft,
                            origin = MicroQuestOrigin.LOCAL_MODEL,
                        )
                    }.getOrElse {
                        RenderedMicroQuest(
                            draft = fallback.compose(plan),
                            origin = MicroQuestOrigin.PROCEDURAL_FALLBACK,
                        )
                    }
                }
            }
            else -> RenderedMicroQuest(
                draft = fallback.compose(plan),
                origin = MicroQuestOrigin.PROCEDURAL_FALLBACK,
            )
        }
    }
}
