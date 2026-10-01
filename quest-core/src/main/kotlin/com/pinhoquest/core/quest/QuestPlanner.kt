package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSessionFilters

data class GameQuestSeed(
    val title: String,
    val environment: QuestEnvironment,
)

data class QuestContext(
    val categoryAffinities: Map<QuestCategory, Double> = emptyMap(),
    val recentCategories: List<QuestCategory> = emptyList(),
    val gameCandidate: GameQuestSeed? = null,
)

data class QuestGenerationPlan(
    val mode: QuestMode,
    val filters: QuestSessionFilters,
    val selectedCategory: QuestCategory,
    val selectedEnvironment: QuestEnvironment,
    val affinityWeight: Double,
    val gameCandidate: GameQuestSeed?,
)

class QuestPlanner {
    fun plan(request: QuestRequest, context: QuestContext): QuestGenerationPlan {
        val affinityWeight = if (request.mode == QuestMode.RANDOM) 0.25 else 1.0
        val selectedCategory = when {
            request.mode == QuestMode.GAME -> QuestCategory.GAMING
            request.filters.categories.isNotEmpty() ->
                request.filters.categories.minBy { it.ordinal }
            request.mode == QuestMode.RANDOM -> selectNovelCategory(context)
            else -> selectPreferredCategory(context)
        }
        val selectedEnvironment = when {
            request.filters.environments.isNotEmpty() ->
                request.filters.environments.minBy { it.ordinal }
            request.mode == QuestMode.GAME && context.gameCandidate != null ->
                context.gameCandidate.environment
            else -> QuestEnvironment.ANYWHERE
        }
        return QuestGenerationPlan(
            mode = request.mode,
            filters = request.filters,
            selectedCategory = selectedCategory,
            selectedEnvironment = selectedEnvironment,
            affinityWeight = affinityWeight,
            gameCandidate = context.gameCandidate,
        )
    }

    private fun selectPreferredCategory(context: QuestContext): QuestCategory =
        context.categoryAffinities
            .filterKeys { it != QuestCategory.GAMING }
            .maxByOrNull { it.value }
            ?.key
            ?: QuestCategory.CREATIVE

    private fun selectNovelCategory(context: QuestContext): QuestCategory {
        val candidates = listOf(
            QuestCategory.CODING,
            QuestCategory.CREATIVE,
            QuestCategory.EXPLORATION,
            QuestCategory.LEARNING,
            QuestCategory.RANDOM,
        )
        return candidates.firstOrNull { it !in context.recentCategories } ?: QuestCategory.RANDOM
    }
}
