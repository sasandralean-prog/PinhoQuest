package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceOutcome
import java.text.Normalizer

/**
 * Decodes typed native tool-call arguments and rejects internal prompt/schema
 * text before it can cross the user-facing quest boundary.
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

        val text = MicroQuestText(
            title = title,
            description = description,
            objectives = objectives,
        )
        require(text.fields().none(::containsPromptOrSchemaEcho)) {
            "model output contains prompt instructions or contract placeholders"
        }
        return text
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

    private fun containsPromptOrSchemaEcho(value: String): Boolean {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .replace(NON_ALPHANUMERIC, " ")
            .trim()

        return INTERNAL_PROMPT_PHRASES.any { phrase -> normalized.contains(phrase) } ||
            CONTRACT_RANGE_PATTERN.containsMatchIn(normalized) ||
            OBJECTIVE_COUNT_PATTERN.containsMatchIn(normalized)
    }

    private fun MicroQuestText.fields(): List<String> =
        listOf(title, description) + objectives

    private companion object {
        val DIACRITICS = Regex("\\p{Mn}+")
        val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
        val CONTRACT_RANGE_PATTERN = Regex("""\b\d+\s+\d+\s+(?:chars?|caracteres)\b""")
        val OBJECTIVE_COUNT_PATTERN = Regex(
            """\b\d+\s+\d+\s+textos?\s+de\s+\d+\s+\d+\s+chars?\b""",
        )
        val INTERNAL_PROMPT_PHRASES = listOf(
            "nao invente fatos",
            "nao repita instrucoes",
            "nao repita marcadores",
            "nao copie instrucoes",
            "nao copie estas instrucoes",
            "fatos aprovados",
            "objetivo aprovado",
            "exemplos aprovados",
            "pistas factuais aprovadas",
            "chame exatamente compose quest text",
            "chame a funcao compose quest text",
            "tarefa compose quest text",
            "retorno chame a funcao",
            "limites do contrato",
            "argumentos canonicos",
            "preencha os argumentos",
        )
    }
}
