package com.pinhoquest.core.inference

/**
 * Canonical inference budget for a governed local-model execution.
 *
 * The budget is a policy boundary, not a performance hint. Lower layers must
 * enforce the same values rather than expanding them silently.
 */
data class InferenceBudget(
    val maxPromptCharacters: Int,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val maxToolCalls: Int,
) {
    init {
        require(maxPromptCharacters > 0) { "maxPromptCharacters must be positive" }
        require(maxContextTokens > 0) { "maxContextTokens must be positive" }
        require(maxOutputTokens > 0) { "maxOutputTokens must be positive" }
        require(maxToolCalls > 0) { "maxToolCalls must be positive" }
    }

    companion object {
        /**
         * P3 budget is intentionally the already-observed bounded configuration:
         * prompt 1200 chars, context 1280 tokens, output 128 tokens, one tool call.
         */
        val P3: InferenceBudget = InferenceBudget(
            maxPromptCharacters = 1200,
            maxContextTokens = 1280,
            maxOutputTokens = 128,
            maxToolCalls = 1,
        )
    }
}
