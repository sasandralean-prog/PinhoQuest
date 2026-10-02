package com.pinhoquest.core.inference.prompt

class CompactPromptSerializer(
    private val limits: PromptContractLimits = PromptContractLimits(),
) {
    fun serialize(envelope: BoundedPromptEnvelope): String {
        val json = buildJson(envelope)
        val prompt = PREAMBLE + "\n" + json
        require(prompt.length <= limits.maxPromptCharacters) {
            "prompt exceeds deterministic character budget"
        }
        return prompt
    }

    private fun buildJson(envelope: BoundedPromptEnvelope): String = buildString {
        append("categoria=")
        append(envelope.category.name)
        append("\nambiente=")
        append(envelope.environment.name)
        append("\ntempo=")
        append(formatTime(envelope.minMinutes, envelope.maxMinutes))
        if (envelope.difficulty != null) {
            append("\ndificuldade=")
            append(envelope.difficulty.name)
        }
        append("\ntags=")
        append(envelope.selectedTags.joinToString(", ") { escape(it.label) }.ifEmpty { "-" })
        append("\nfatos=")
        append(envelope.researchHints.joinToString("; ") { serializeResearch(it) }.ifEmpty { "-" })
        append("\nsaida=JSON: title, description, objectives(string[]), bonusObjectives(string[]), estimatedMinutes(int), estimatedDifficulty. ")
        append("Objectives: 1-4 acoes concretas. Bonus: 0-2 acoes. Nao use nomes de campos como conteudo.")
    }

    private fun serializeResearch(hint: PromptResearchHint): String = when (hint) {
        is PromptResearchHint.Game -> buildString {
            append("GAME(name=")
            append(escape(hint.canonicalName))
            hint.platform?.let {
                append(", platform=")
                append(escape(it))
            }
            hint.genre?.let {
                append(", genre=")
                append(escape(it))
            }
            append(", availability=")
            append(hint.availability.name)
            append(")")
        }
        is PromptResearchHint.Flower -> buildString {
            append("FLOWER(name=")
            append(escape(hint.canonicalName))
            hint.commonName?.let {
                append(", commonName=")
                append(escape(it))
            }
            hint.region?.let {
                append(", region=")
                append(escape(it))
            }
            append(")")
        }
    }
    private fun formatTime(min: Int?, max: Int?): String =
        when {
            min != null && max != null -> "$min-$max"
            min != null -> "$min+"
            max != null -> "0-$max"
            else -> "ANY"
        }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private companion object {
        const val PREAMBLE =
            "Crie uma quest concreta, curta e executavel. Nao invente fatos externos, XP ou recompensas. " +
                "Use exatamente categoria, ambiente, dificuldade e tempo. Escreva conteudo natural; " +
                "nunca use nomes de campos, metacomentarios ou instrucoes como conteudo da quest. " +
                "A resposta deve ser somente JSON valido."
    }
}