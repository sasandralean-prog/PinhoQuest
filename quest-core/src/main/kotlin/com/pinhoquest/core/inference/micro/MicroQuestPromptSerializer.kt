package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceBudget

class MicroQuestPromptSerializer(
    private val budget: InferenceBudget = InferenceBudget.P3,
) {
    fun serialize(request: MicroQuestCompositionRequest): String {
        val envelope = request.prompt.envelope
        val prompt = buildString {
            append("Escreva apenas o texto de uma quest. ")
            append("Nao invente fatos, XP, recompensa ou regras. ")
            append("Nao repita instrucoes ou marcadores. ")
            append("Idioma: ")
            append(request.style.language)
            append(". Tom: ")
            append(request.style.tone)
            append(".\n")

            append("TAREFA=compose_quest_text\n")
            append("CATEGORIA=")
            append(envelope.category.name)
            append("\nAMBIENTE=")
            append(envelope.environment.name)
            append("\nDIFICULDADE=")
            append(envelope.difficulty?.name ?: "EASY")
            append("\nTEMPO=")
            append(formatTime(envelope.minMinutes, envelope.maxMinutes))
            append("\nTAGS=")
            append(envelope.selectedTags.joinToString(", ") { it.label }.ifEmpty { "-" })
            append("\n\nEXEMPLOS_APROVADOS:\n")

            if (request.prompt.examples.isEmpty()) {
                append("- nenhum")
            } else {
                request.prompt.examples.forEachIndexed { index, example ->
                    append(index + 1)
                    append(". ")
                    append(example.title)
                    append(" | ")
                    append(example.description)
                    append(" | ")
                    append(example.objectives.joinToString("; "))
                    append("\n")
                }
            }

            append("\nRETORNO=chame a funcao ")
            append(MicroQuestToolContract.NAME)
            append(" com ")
            append(MicroQuestToolContract.REQUIRED_ARGUMENTS.joinToString(", "))
            append(".\n")
            append(MicroQuestToolContract.TITLE)
            append(": ")
            append(MicroQuestToolContract.MIN_TITLE_LENGTH)
            append("-")
            append(MicroQuestToolContract.MAX_TITLE_LENGTH)
            append(" chars; ")
            append(MicroQuestToolContract.DESCRIPTION_FIELD)
            append(": ")
            append(MicroQuestToolContract.MIN_DESCRIPTION_LENGTH)
            append("-")
            append(MicroQuestToolContract.MAX_DESCRIPTION_LENGTH)
            append(" chars; ")
            append(MicroQuestToolContract.OBJECTIVES)
            append(": ")
            append(MicroQuestToolContract.MIN_OBJECTIVES)
            append("-")
            append(MicroQuestToolContract.MAX_OBJECTIVES)
            append(" textos de ")
            append(MicroQuestToolContract.MIN_OBJECTIVE_LENGTH)
            append("-")
            append(MicroQuestToolContract.MAX_OBJECTIVE_LENGTH)
            append(" chars.")
        }.trim()

        require(prompt.length <= budget.maxPromptCharacters) {
            "micro prompt exceeds inference budget"
        }
        return prompt
    }

    private fun formatTime(min: Int?, max: Int?): String =
        when {
            min != null && max != null -> "$min-$max"
            min != null -> "$min+"
            max != null -> "0-$max"
            else -> "ANY"
        }
}
