package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.InferenceBudget
import com.pinhoquest.core.inference.prompt.PromptResearchHint
import com.pinhoquest.domain.quest.QuestCategory

class MicroQuestPromptSerializer(
    private val budget: InferenceBudget = InferenceBudget.P3,
) {
    fun serialize(request: MicroQuestCompositionRequest): String {
        val envelope = request.prompt.envelope
        val seed = categorySeed(envelope.category, request.style.language)
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
            append(envelope.environment.name)
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

    private fun categorySeed(category: QuestCategory, language: String): CategorySeed {
        val english = language.equals("en-US", ignoreCase = true)
        return when (category) {
            QuestCategory.CODING -> if (english) {
                CategorySeed(
                    "Pick one small technical detail and try to understand it differently.",
                    "Do a short experiment and note what you discovered.",
                )
            } else {
                CategorySeed(
                    "Escolha um detalhe tecnico pequeno e tente entende-lo de um jeito diferente.",
                    "Faca um experimento curto e anote o que descobriu.",
                )
            }

            QuestCategory.GAMING -> if (english) {
                CategorySeed(
                    "Try for a few minutes something you would normally ignore in this game.",
                    "Test a different mechanic, weapon, class, or strategy.",
                )
            } else {
                CategorySeed(
                    "Experimente por alguns minutos algo que voce normalmente ignoraria nesse jogo.",
                    "Teste uma mecanica, arma, classe ou estrategia diferente.",
                )
            }

            QuestCategory.EXPLORATION -> if (english) {
                CategorySeed(
                    "Look for three things around you that normally go unnoticed.",
                    "Find and record three interesting details.",
                )
            } else {
                CategorySeed(
                    "Procure tres coisas ao seu redor que normalmente passariam despercebidas.",
                    "Encontre e registre tres detalhes interessantes.",
                )
            }

            QuestCategory.LEARNING -> if (english) {
                CategorySeed(
                    "Choose one small question that has always gone unanswered.",
                    "Find an answer and explain it in your own words.",
                )
            } else {
                CategorySeed(
                    "Escolha uma pergunta pequena que sempre ficou sem resposta.",
                    "Descubra uma resposta e explique com suas proprias palavras.",
                )
            }

            QuestCategory.RANDOM -> if (english) {
                CategorySeed(
                    "Do something simple that breaks your routine for the next few minutes.",
                    "Choose a different action and carry it through.",
                )
            } else {
                CategorySeed(
                    "Faca algo simples que quebre um pouco a rotina dos proximos minutos.",
                    "Escolha uma acao diferente e leve-a ate o fim.",
                )
            }

            QuestCategory.CREATIVE -> if (english) {
                CategorySeed(
                    "Take three nearby ideas or objects and invent a connection between them.",
                    "Create something small using that connection.",
                )
            } else {
                CategorySeed(
                    "Pegue tres ideias ou objetos proximos e invente uma conexao entre eles.",
                    "Crie alguma coisa pequena usando essa conexao.",
                )
            }
        }
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
                append(", focus=")
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

    private fun formatTime(min: Int?, max: Int?): String {
        val effectiveMin = min ?: 15
        val effectiveMax = max ?: maxOf(effectiveMin, 30)
        return "$effectiveMin-$effectiveMax"
    }

    private data class CategorySeed(
        val facts: String,
        val objective: String,
    )
}
