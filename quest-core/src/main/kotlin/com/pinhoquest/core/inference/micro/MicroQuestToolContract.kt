package com.pinhoquest.core.inference.micro

/**
 * Canonical semantic contract for the FunctionGemma P3 tool call.
 *
 * This contract owns only the creative text produced by the model.
 * Deterministic quest metadata (category, environment, difficulty, duration,
 * game candidate, tags, provenance and rewards) remains outside this tool.
 */
object MicroQuestToolContract {
    const val NAME = "compose_quest_text"
    const val VERSION = "V1"
    const val DESCRIPTION = "Escreve o texto final de uma quest humana curta."

    const val TITLE = "title"
    const val DESCRIPTION_FIELD = "description"
    const val OBJECTIVES = "objectives"

    const val MIN_TITLE_LENGTH = 1
    const val MAX_TITLE_LENGTH = 80

    const val MIN_DESCRIPTION_LENGTH = 12
    const val MAX_DESCRIPTION_LENGTH = 220

    const val MIN_OBJECTIVES = 1
    const val MAX_OBJECTIVES = 4

    const val MIN_OBJECTIVE_LENGTH = 1
    const val MAX_OBJECTIVE_LENGTH = 120

    val REQUIRED_ARGUMENTS = listOf(
        TITLE,
        DESCRIPTION_FIELD,
        OBJECTIVES,
    )

    val ARGUMENTS: List<Argument> = listOf(
        Argument(
            name = TITLE,
            description = "Titulo natural da quest.",
            type = ArgumentType.STRING,
            required = true,
            minLength = MIN_TITLE_LENGTH,
            maxLength = MAX_TITLE_LENGTH,
        ),
        Argument(
            name = DESCRIPTION_FIELD,
            description = "Descricao natural e convidativa da quest.",
            type = ArgumentType.STRING,
            required = true,
            minLength = MIN_DESCRIPTION_LENGTH,
            maxLength = MAX_DESCRIPTION_LENGTH,
        ),
        Argument(
            name = OBJECTIVES,
            description = "Acoes concretas da quest, em ordem.",
            type = ArgumentType.STRING_LIST,
            required = true,
            minItems = MIN_OBJECTIVES,
            maxItems = MAX_OBJECTIVES,
            itemMinLength = MIN_OBJECTIVE_LENGTH,
            itemMaxLength = MAX_OBJECTIVE_LENGTH,
        ),
    )

    init {
        require(ARGUMENTS.map { it.name } == REQUIRED_ARGUMENTS)
        require(ARGUMENTS.all { it.required })
    }

    enum class ArgumentType {
        STRING,
        STRING_LIST,
    }

    data class Argument(
        val name: String,
        val description: String,
        val type: ArgumentType,
        val required: Boolean,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val minItems: Int? = null,
        val maxItems: Int? = null,
        val itemMinLength: Int? = null,
        val itemMaxLength: Int? = null,
    )
}
