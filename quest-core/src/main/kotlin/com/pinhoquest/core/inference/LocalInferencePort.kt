package com.pinhoquest.core.inference

data class GenerationRequest(
    val prompt: String,
    val budget: InferenceBudget = InferenceBudget.P3,
) {
    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(prompt.length <= budget.maxPromptCharacters) {
            "prompt exceeds inference budget"
        }
    }

    val maxOutputTokens: Int
        get() = budget.maxOutputTokens
}

sealed interface InferenceOutcome {
    /**
     * Structured native tool call returned by the model adapter.
     *
     * Raw model output is intentionally absent from the core boundary.
     */
    data class ToolCall(
        val name: String,
        val arguments: Map<String, Any?>,
    ) : InferenceOutcome

    data object ModelUnavailable : InferenceOutcome
    data object InsufficientResources : InferenceOutcome
    data object RuntimeUnavailable : InferenceOutcome
    data object InvalidOutput : InferenceOutcome
    data object TechnicalFailure : InferenceOutcome
}

fun interface LocalInferencePort {
    suspend fun generate(request: GenerationRequest): InferenceOutcome
}
