package com.pinhoquest.core.inference.micro

class MicroQuestPromptSerializer(
    private val maxCharacters: Int = 1200,
) {
    fun serialize(request: MicroQuestCompositionRequest): String {
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
            append(request.plan.selectedCategory.name)
            append("\nAMBIENTE=")
            append(request.plan.selectedEnvironment.name)
            append("\nDIFICULDADE=")
            append(request.plan.filters.desiredDifficulty?.name ?: "EASY")
            append("\nTEMPO=")
            append(formatTime(request))
            append("\nTAGS=")
            append(request.tags.joinToString(", ").ifEmpty { "-" })
            append("\n\nEXEMPLOS_APROVADOS:\n")
            if (request.examples.isEmpty()) {
                append("- nenhum")
            } else {
                request.examples.forEachIndexed { index, example ->
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
            append("\nRETORNO=chame a funcao compose_quest_text com title, description e objectives.")
            append("\ntitle: 1-80 chars; description: 12-220 chars; objectives: 1-4 textos de 1-120 chars.")
        }.trim()

        require(prompt.length <= maxCharacters) {
            "micro prompt exceeds deterministic character budget"
        }
        return prompt
    }

    private fun formatTime(request: MicroQuestCompositionRequest): String {
        val min = request.plan.filters.minMinutes
        val max = request.plan.filters.maxMinutes
        return when {
            min != null && max != null -> "$min-$max"
            min != null -> "$min+"
            max != null -> "0-$max"
            else -> "ANY"
        }
    }
}
