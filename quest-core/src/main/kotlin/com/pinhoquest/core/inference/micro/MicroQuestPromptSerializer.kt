package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceBudget
import com.pinhoquest.core.inference.prompt.PromptResearchHint
import com.pinhoquest.domain.quest.QuestEnvironment

class MicroQuestPromptSerializer(
    private val budget: InferenceBudget = InferenceBudget.P3,
) {
    fun serialize(request: MicroQuestCompositionRequest): String {
        val envelope = request.prompt.envelope
        val seed = MicroQuestCategorySeedCatalog.forCategory(envelope.category, request.style.language)
        val prompt = buildString {
            append("Crie uma micro-quest concreta, útil e natural a partir dos dados determinísticos abaixo. ")
            append("Use os fatos e o objetivo aprovados como ponto de partida. ")
            append("Chame exatamente ")
            append(MicroQuestToolContract.NAME)
            append(" uma vez. Não copie instruções, nomes de campos ou limites do contrato. ")
            append("Não invente fatos, XP, recompensas ou metadados.\n")

            append("Categoria=")
            append(envelope.category.name)
            append("; Ambiente=")
            append(formatEnvironment(envelope.environment))
            append("; Dificuldade=")
            append(envelope.difficulty?.name ?: "EASY")
            append("; Tempo=")
            append(formatTime(envelope.minMinutes, envelope.maxMinutes))
            append("; Idioma=")
            append(request.style.language)
            append(".\n")

            append("Fatos aprovados: ")
            append(seed.facts)
            append(" Objetivo aprovado: ")
            append(seed.objective)

            if (envelope.selectedTags.isNotEmpty()) {
                append(" Tags selecionadas: ")
                append(envelope.selectedTags.joinToString(", ") { it.label })
            }

            if (envelope.researchHints.isNotEmpty()) {
                append(" Pistas factuais aprovadas: ")
                append(envelope.researchHints.joinToString("; ") { serializeHint(it) })
            }

            if (request.prompt.examples.isNotEmpty()) {
                append("\nExemplos aprovados (referência de estilo; não copie literalmente):\n")
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
        }.trim()

        require(prompt.length <= budget.maxPromptCharacters) {
            "micro prompt exceeds inference budget"
        }
        return prompt
    }

    private fun serializeHint(hint: PromptResearchHint): String = when (hint) {
        is PromptResearchHint.Game -> buildString {
            append("GAME(name=")
            append(hint.canonicalName)
            hint.platform?.let {
                append(", platform=")
                append(it)
            }
            hint.genre?.let {
                append(", genre=")
                append(it)
            }
            hint.focus?.let {
                append(", foco=")
                append(it)
            }
            append(")")
        }

        is PromptResearchHint.Flower -> buildString {
            append("FLOWER(name=")
            append(hint.canonicalName)
            hint.commonName?.let {
                append(", commonName=")
                append(it)
            }
            hint.region?.let {
                append(", region=")
                append(it)
            }
            append(")")
        }
    }

    private fun formatEnvironment(environment: QuestEnvironment): String =
        when (environment) {
            QuestEnvironment.ANYWHERE -> "ANY"
            else -> environment.name
        }

    private fun formatTime(min: Int?, max: Int?): String {
        val effectiveMin = min ?: 15
        val effectiveMax = max ?: maxOf(effectiveMin, 30)
        return "$effectiveMin-$effectiveMax"
    }

}
