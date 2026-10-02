package com.pinhoquest.core.inference

data class GenerationRequest(
    val prompt: String,
    val maxOutputTokens: Int,
) {
    init {
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(maxOutputTokens > 0) { "maxOutputTokens must be positive" }
    }
}

sealed interface InferenceOutcome {
    data class Success(val text: String) : InferenceOutcome
    data object ModelUnavailable : InferenceOutcome
    data object InsufficientResources : InferenceOutcome
    data object RuntimeUnavailable : InferenceOutcome
    data object InvalidOutput : InferenceOutcome
    data object TechnicalFailure : InferenceOutcome
}

fun interface LocalInferencePort {
    suspend fun generate(request: GenerationRequest): InferenceOutcome
}