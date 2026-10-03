package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceOutcome

/**
 * Decodes only the typed native tool-call arguments produced by the adapter.
 *
 * No raw model text enters this decoder. The adapter owns transport parsing;
 * this class owns the P3 semantic argument contract.
 */
class MicroQuestToolCallDecoder {
    fun decode(outcome: InferenceOutcome.ToolCall): MicroQuestText {
        require(outcome.name == MicroQuestToolContract.NAME) {
            "unexpected function name"
        }

        val arguments = outcome.arguments
        require(arguments.keys == MicroQuestToolContract.REQUIRED_ARGUMENTS.toSet()) {
            "unexpected function arguments"
        }

        val title = stringArgument(arguments, MicroQuestToolContract.TITLE)
        val description = stringArgument(arguments, MicroQuestToolContract.DESCRIPTION_FIELD)
        val objectives = stringListArgument(arguments, MicroQuestToolContract.OBJECTIVES)

        require(title.length in MicroQuestToolContract.MIN_TITLE_LENGTH..MicroQuestToolContract.MAX_TITLE_LENGTH)
        require(
            description.length in
                MicroQuestToolContract.MIN_DESCRIPTION_LENGTH..MicroQuestToolContract.MAX_DESCRIPTION_LENGTH
        )
        require(
            objectives.size in
                MicroQuestToolContract.MIN_OBJECTIVES..MicroQuestToolContract.MAX_OBJECTIVES
        )
        require(
            objectives.all {
                it.length in
                    MicroQuestToolContract.MIN_OBJECTIVE_LENGTH..MicroQuestToolContract.MAX_OBJECTIVE_LENGTH
            }
        )

        return MicroQuestText(
            title = title,
            description = description,
            objectives = objectives,
        )
    }

    private fun stringArgument(arguments: Map<String, Any?>, key: String): String {
        val value = arguments[key] as? String ?: throw IllegalArgumentException("$key must be string")
        return value.trim().also { require(it.isNotBlank()) }
    }

    private fun stringListArgument(arguments: Map<String, Any?>, key: String): List<String> {
        val values = arguments[key] as? List<*>
            ?: throw IllegalArgumentException("$key must be a string list")
        return values.map {
            val value = it as? String
                ?: throw IllegalArgumentException("$key must contain strings")
            value.trim().also { text -> require(text.isNotBlank()) }
        }
    }
}
