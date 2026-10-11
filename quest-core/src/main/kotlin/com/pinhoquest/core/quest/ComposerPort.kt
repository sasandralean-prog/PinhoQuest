package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestObjective

enum class QuestCompositionOrigin {
    LOCAL_MODEL,
    PROCEDURAL_FALLBACK,
    UNKNOWN,
}

enum class QuestFallbackReason {
    PROCEDURAL_ONLY,
    MODEL_NOT_INSTALLED,
    INFERENCE_BUSY,
    ADMISSION_DENIED,
    INFERENCE_FAILED,
    INVALID_MODEL_OUTPUT,
}

data class QuestCompositionOutcome(
    val draft: QuestDraft,
    val origin: QuestCompositionOrigin,
    val fallbackReason: QuestFallbackReason? = null,
)

/**
 * Implementations may expose a non-sensitive outcome in addition to the draft.
 * The default keeps older/test composers compatible without pretending to know their origin.
 */
fun interface ComposerPort {
    suspend fun compose(plan: QuestGenerationPlan): QuestDraft

    suspend fun composeWithOutcome(plan: QuestGenerationPlan): QuestCompositionOutcome =
        QuestCompositionOutcome(
            draft = compose(plan),
            origin = QuestCompositionOrigin.UNKNOWN,
        )
}

class ProceduralComposer : ComposerPort {
    override suspend fun compose(plan: QuestGenerationPlan): QuestDraft =
        composeWithOutcome(plan).draft

    override suspend fun composeWithOutcome(plan: QuestGenerationPlan): QuestCompositionOutcome {
        val copy = copyFor(plan)
        val minMinutes = plan.filters.minMinutes ?: 15
        val maxMinutes = plan.filters.maxMinutes ?: maxOf(minMinutes, 30)
        return QuestCompositionOutcome(
            draft = QuestDraft(
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
            ),
            origin = QuestCompositionOrigin.PROCEDURAL_FALLBACK,
            fallbackReason = QuestFallbackReason.PROCEDURAL_ONLY,
        )
    }

    private fun copyFor(plan: QuestGenerationPlan): ProceduralCopy {
        val variants = when (plan.selectedCategory) {
            QuestCategory.CODING -> listOf(
                ProceduralCopy(
                    "Pequeno laboratório digital",
                    "Escolha um detalhe técnico pequeno e explore como ele funciona.",
                    "Faça um experimento curto e anote o que descobriu.",
                ),
                ProceduralCopy(
                    "Detetive de bugs",
                    "Observe um programa ou ferramenta e encontre algo que poderia ser mais claro.",
                    "Registre uma hipótese e teste uma mudança pequena, se for seguro.",
                ),
                ProceduralCopy(
                    "Atalho inteligente",
                    "Procure uma tarefa digital repetitiva que possa ser simplificada.",
                    "Descubra e experimente um atalho ou uma melhoria pequena.",
                ),
            )
            QuestCategory.GAMING -> {
                val game = requireNotNull(plan.gameCandidate) {
                    "A game quest requires a validated game candidate"
                }
                listOf(
                    ProceduralCopy(
                        "Jogue ${game.title} de outro jeito",
                        "Experimente uma mecânica ou estratégia que você normalmente deixaria de lado.",
                        "Teste uma abordagem diferente por alguns minutos.",
                    ),
                    ProceduralCopy(
                        "Explorador de ${game.title}",
                        "Observe um lugar, sistema ou detalhe do jogo que costuma passar despercebido.",
                        "Encontre e descreva três detalhes interessantes do jogo.",
                    ),
                    ProceduralCopy(
                        "Desafio gentil em ${game.title}",
                        "Escolha uma meta pequena que combine com seu tempo e com as regras do jogo.",
                        "Complete a meta escolhida sem transformar a sessão numa obrigação.",
                    ),
                )
            }
            QuestCategory.EXPLORATION -> listOf(
                ProceduralCopy(
                    "Caça a detalhes",
                    "Procure três coisas ao seu redor que normalmente passariam despercebidas.",
                    "Encontre e registre três detalhes interessantes.",
                ),
                ProceduralCopy(
                    "Rota de curiosidade",
                    "Observe um espaço familiar como se estivesse visitando pela primeira vez.",
                    "Escolha um detalhe novo e descubra algo sobre ele.",
                ),
                ProceduralCopy(
                    "Pequena expedição",
                    "Explore com segurança um canto próximo, físico ou digital, que você raramente visita.",
                    "Anote uma descoberta e por que ela chamou sua atenção.",
                ),
            )
            QuestCategory.LEARNING -> listOf(
                ProceduralCopy(
                    "Curiosidade de bolso",
                    "Escolha uma pergunta pequena que sempre ficou sem resposta.",
                    "Descubra uma resposta e explique com suas próprias palavras.",
                ),
                ProceduralCopy(
                    "Ensine para si",
                    "Escolha um conceito que você conhece só por alto.",
                    "Explique a ideia em três frases simples, sem copiar a fonte.",
                ),
                ProceduralCopy(
                    "Uma coisa nova",
                    "Encontre uma curiosidade sobre um tema que já desperta seu interesse.",
                    "Registre o fato e uma pergunta que ele deixou.",
                ),
            )
            QuestCategory.RANDOM -> listOf(
                ProceduralCopy(
                    "Pequeno caos controlado",
                    "Faça algo simples que quebre um pouco a rotina dos próximos minutos.",
                    "Escolha uma ação diferente e leve-a até o fim.",
                ),
                ProceduralCopy(
                    "Troca de perspectiva",
                    "Escolha uma tarefa comum e tente abordá-la por um caminho diferente.",
                    "Experimente a nova abordagem e perceba o que mudou.",
                ),
                ProceduralCopy(
                    "Pausa curiosa",
                    "Escolha uma atividade leve que caiba no tempo disponível e desperte curiosidade.",
                    "Faça a atividade por alguns minutos e guarde uma pequena descoberta.",
                ),
            )
            QuestCategory.CREATIVE -> listOf(
                ProceduralCopy(
                    "Três coisas viram uma",
                    "Pegue três ideias ou objetos próximos e invente uma conexão entre eles.",
                    "Crie alguma coisa pequena usando essa conexão.",
                ),
                ProceduralCopy(
                    "Rascunho sem pressão",
                    "Escolha uma ideia e faça uma versão simples sem tentar deixá-la perfeita.",
                    "Produza um rascunho, esboço ou protótipo em poucos minutos.",
                ),
                ProceduralCopy(
                    "Mistura improvável",
                    "Combine duas referências diferentes para criar uma ideia nova.",
                    "Dê um nome à ideia e registre como ela funcionaria.",
                ),
            )
        }
        return variants[plan.variantIndex.mod(variants.size)]
    }
}

private data class ProceduralCopy(
    val title: String,
    val description: String,
    val objective: String,
)
