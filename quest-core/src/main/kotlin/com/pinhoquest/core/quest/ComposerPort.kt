package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestObjective

fun interface ComposerPort {
    suspend fun compose(plan: QuestGenerationPlan): QuestDraft
}

class ProceduralComposer : ComposerPort {
    override suspend fun compose(plan: QuestGenerationPlan): QuestDraft {
        val copy = copyFor(plan)
        val minMinutes = plan.filters.minMinutes ?: 15
        val maxMinutes = plan.filters.maxMinutes ?: maxOf(minMinutes, 30)
        return QuestDraft(
            title = copy.title,
            description = copy.description,
            objectives = listOf(
                QuestObjective(
                    id = ObjectiveId("main-1"),
                    text = copy.objective,
                ),
            ),
            category = plan.selectedCategory,
            environment = plan.selectedEnvironment,
            estimatedDuration = EstimatedDuration(minMinutes, maxMinutes),
            difficulty = plan.filters.desiredDifficulty ?: QuestDifficulty.EASY,
        )
    }

    private fun copyFor(plan: QuestGenerationPlan): ProceduralCopy = when (plan.selectedCategory) {
        QuestCategory.CODING -> ProceduralCopy(
            "Frankenstein Digital",
            "Escolha um detalhe técnico pequeno e tente entendê-lo de um jeito diferente.",
            "Faça um experimento curto e anote o que descobriu.",
        )
        QuestCategory.GAMING -> {
            val game = requireNotNull(plan.gameCandidate)
            ProceduralCopy(
                "Jogue ${game.title} de outro jeito",
                "Experimente por alguns minutos algo que você normalmente ignoraria nesse jogo.",
                "Teste uma mecânica, arma, classe ou estratégia diferente.",
            )
        }
        QuestCategory.EXPLORATION -> ProceduralCopy(
            "Caça a detalhes",
            "Procure três coisas ao seu redor que normalmente passariam despercebidas.",
            "Encontre e registre três detalhes interessantes.",
        )
        QuestCategory.LEARNING -> ProceduralCopy(
            "Curiosidade de bolso",
            "Escolha uma pergunta pequena que sempre ficou sem resposta.",
            "Descubra uma resposta e explique com suas próprias palavras.",
        )
        QuestCategory.RANDOM -> ProceduralCopy(
            "Pequeno caos controlado",
            "Faça algo simples que quebre um pouco a rotina dos próximos minutos.",
            "Escolha uma ação diferente e leve-a até o fim.",
        )
        QuestCategory.CREATIVE -> ProceduralCopy(
            "Três coisas viram uma",
            "Pegue três ideias ou objetos próximos e invente uma conexão entre eles.",
            "Crie alguma coisa pequena usando essa conexão.",
        )
    }
}

private data class ProceduralCopy(
    val title: String,
    val description: String,
    val objective: String,
)
