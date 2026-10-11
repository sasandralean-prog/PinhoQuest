package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSessionFilters
import kotlin.random.Random

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
    val variantIndex: Int = 0,
)

/** Injectable bounded choice source keeps production selection varied and tests reproducible. */
fun interface QuestChoiceSource {
    fun nextInt(bound: Int): Int
}

private val productionQuestChoiceSource = QuestChoiceSource { bound ->
    require(bound > 0) { "Choice bound must be positive" }
    Random.nextInt(bound)
}

class QuestPlanner(
    private val choiceSource: QuestChoiceSource = productionQuestChoiceSource,
) {
    fun plan(request: QuestRequest, context: QuestContext): QuestGenerationPlan {
        val affinityWeight = if (request.mode == QuestMode.RANDOM) 0.25 else 1.0
        val selectedCategory = when {
            request.mode == QuestMode.GAME -> QuestCategory.GAMING
            request.filters.categories.isNotEmpty() ->
                chooseCategory(request.filters.categories.sortedBy { it.ordinal })
            request.mode == QuestMode.RANDOM -> selectNovelCategory(context)
            else -> selectPreferredCategory(context)
        }
        val selectedEnvironment = when {
            request.filters.environments.isNotEmpty() ->
                request.filters.environments.sortedBy { it.ordinal }.first()
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
            variantIndex = choiceSource.nextInt(PROCEDURAL_VARIANT_COUNT),
        )
    }

    private fun selectPreferredCategory(context: QuestContext): QuestCategory =
        context.categoryAffinities
            .filterKeys { it != QuestCategory.GAMING }
            .maxByOrNull { it.value }
            ?.key
            ?: chooseCategory(nonGameCategories)

    private fun selectNovelCategory(context: QuestContext): QuestCategory {
        val recent = context.recentCategories.toSet()
        val novel = nonGameCategories.filterNot { it in recent }
        // Prefer unseen categories. If history covers all candidates, rotate through the
        // complete stable list using an injected choice source instead of a fixed fallback.
        return chooseCategory(novel.ifEmpty { nonGameCategories })
    }

    private fun chooseCategory(candidates: List<QuestCategory>): QuestCategory {
        require(candidates.isNotEmpty()) { "At least one category candidate is required" }
        val stableCandidates = candidates.distinct().sortedBy { it.ordinal }
        return stableCandidates[choiceSource.nextInt(stableCandidates.size).coerceIn(0, stableCandidates.lastIndex)]
    }

    private companion object {
        val nonGameCategories = listOf(
            QuestCategory.CODING,
            QuestCategory.CREATIVE,
            QuestCategory.EXPLORATION,
            QuestCategory.LEARNING,
            QuestCategory.RANDOM,
        )
        const val PROCEDURAL_VARIANT_COUNT = 3
    }
}
