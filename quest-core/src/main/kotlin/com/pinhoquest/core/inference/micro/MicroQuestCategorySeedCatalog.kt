package com.pinhoquest.core.inference.micro

import com.pinhoquest.domain.quest.QuestCategory

data class MicroQuestCategorySeed(
    val facts: String,
    val objective: String,
)

/**
 * Shared category seeds for the on-device prompt and the FunctionGemma SFT dataset.
 * Keep these deterministic, short, and actionable; the model may elaborate but must
 * not present contract instructions as quest content.
 */
object MicroQuestCategorySeedCatalog {
    const val CODING_FACTS_PT_BR = "Escolha um detalhe tecnico pequeno e tente entende-lo de um jeito diferente."
    const val CODING_OBJECTIVE_PT_BR = "Faca um experimento curto e anote o que descobriu."
    const val CODING_FACTS_EN_US = "Pick one small technical detail and try to understand it differently."
    const val CODING_OBJECTIVE_EN_US = "Do a short experiment and note what you discovered."

    const val GAMING_FACTS_PT_BR = "Experimente por alguns minutos algo que voce normalmente ignoraria nesse jogo."
    const val GAMING_OBJECTIVE_PT_BR = "Teste uma mecanica, arma, classe ou estrategia diferente."
    const val GAMING_FACTS_EN_US = "Try for a few minutes something you would normally ignore in this game."
    const val GAMING_OBJECTIVE_EN_US = "Test a different mechanic, weapon, class, or strategy."

    const val EXPLORATION_FACTS_PT_BR = "Procure tres coisas ao seu redor que normalmente passariam despercebidas."
    const val EXPLORATION_OBJECTIVE_PT_BR = "Encontre e registre tres detalhes interessantes."
    const val EXPLORATION_FACTS_EN_US = "Look for three things around you that normally go unnoticed."
    const val EXPLORATION_OBJECTIVE_EN_US = "Find and record three interesting details."

    const val LEARNING_FACTS_PT_BR = "Escolha uma pergunta pequena que sempre ficou sem resposta."
    const val LEARNING_OBJECTIVE_PT_BR = "Descubra uma resposta e explique com suas proprias palavras."
    const val LEARNING_FACTS_EN_US = "Choose one small question that has always gone unanswered."
    const val LEARNING_OBJECTIVE_EN_US = "Find an answer and explain it in your own words."

    const val RANDOM_FACTS_PT_BR = "Faca algo simples que quebre um pouco a rotina dos proximos minutos."
    const val RANDOM_OBJECTIVE_PT_BR = "Escolha uma acao diferente e leve-a ate o fim."
    const val RANDOM_FACTS_EN_US = "Do something simple that breaks your routine for the next few minutes."
    const val RANDOM_OBJECTIVE_EN_US = "Choose a different action and carry it through."

    const val CREATIVE_FACTS_PT_BR = "Pegue tres ideias ou objetos proximos e invente uma conexao entre eles."
    const val CREATIVE_OBJECTIVE_PT_BR = "Crie alguma coisa pequena usando essa conexao."
    const val CREATIVE_FACTS_EN_US = "Take three nearby ideas or objects and invent a connection between them."
    const val CREATIVE_OBJECTIVE_EN_US = "Create something small using that connection."

    fun forCategory(category: QuestCategory, language: String): MicroQuestCategorySeed {
        val english = language.equals("en-US", ignoreCase = true)
        return when (category) {
            QuestCategory.CODING -> seed(
                CODING_FACTS_PT_BR, CODING_OBJECTIVE_PT_BR,
                CODING_FACTS_EN_US, CODING_OBJECTIVE_EN_US, english,
            )
            QuestCategory.GAMING -> seed(
                GAMING_FACTS_PT_BR, GAMING_OBJECTIVE_PT_BR,
                GAMING_FACTS_EN_US, GAMING_OBJECTIVE_EN_US, english,
            )
            QuestCategory.EXPLORATION -> seed(
                EXPLORATION_FACTS_PT_BR, EXPLORATION_OBJECTIVE_PT_BR,
                EXPLORATION_FACTS_EN_US, EXPLORATION_OBJECTIVE_EN_US, english,
            )
            QuestCategory.LEARNING -> seed(
                LEARNING_FACTS_PT_BR, LEARNING_OBJECTIVE_PT_BR,
                LEARNING_FACTS_EN_US, LEARNING_OBJECTIVE_EN_US, english,
            )
            QuestCategory.RANDOM -> seed(
                RANDOM_FACTS_PT_BR, RANDOM_OBJECTIVE_PT_BR,
                RANDOM_FACTS_EN_US, RANDOM_OBJECTIVE_EN_US, english,
            )
            QuestCategory.CREATIVE -> seed(
                CREATIVE_FACTS_PT_BR, CREATIVE_OBJECTIVE_PT_BR,
                CREATIVE_FACTS_EN_US, CREATIVE_OBJECTIVE_EN_US, english,
            )
        }
    }

    private fun seed(
        factsPtBr: String,
        objectivePtBr: String,
        factsEnUs: String,
        objectiveEnUs: String,
        english: Boolean,
    ) = if (english) {
        MicroQuestCategorySeed(factsEnUs, objectiveEnUs)
    } else {
        MicroQuestCategorySeed(factsPtBr, objectivePtBr)
    }
}
