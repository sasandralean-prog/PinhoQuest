package com.pinhoquest.core.inference

enum class InferenceStopReason {
    NATIVE_TOOL_CALL,
    INVALID_NATIVE_TOOL_CALL,
    MODEL_UNAVAILABLE,
    INSUFFICIENT_RESOURCES,
    RUNTIME_UNAVAILABLE,
    TECHNICAL_FAILURE,
    BUDGET_REJECTED,
}

data class InferenceTelemetry(
    val requestedPromptCharacters: Int,
    val promptCharacterLimit: Int,
    val requestedContextTokens: Int,
    val requestedOutputTokens: Int,
    val requestedToolCalls: Int,
    val observedConversationTokensBefore: Int?,
    val observedConversationTokensAfter: Int?,
    val observedConversationTokenDelta: Int?,
    val observedToolCalls: Int,
    val stopReason: InferenceStopReason,
)

fun interface InferenceTelemetrySink {
    fun record(event: InferenceTelemetry)
}
