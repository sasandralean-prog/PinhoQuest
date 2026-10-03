package com.pinhoquest.core.inference.prompt

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.tag.Tag

class PromptFactsAssembler(
    private val limits: PromptContractLimits = PromptContractLimits(),
) {
    fun assemble(
        plan: QuestGenerationPlan,
        tags: List<Tag> = emptyList(),
        researchHints: List<PromptResearchHint> = emptyList(),
    ): BoundedPromptEnvelope {
        return BoundedPromptEnvelope(
            schemaVersion = QuestPromptContract.SCHEMA_VERSION,
            task = QuestPromptContract.TASK,
            category = plan.selectedCategory,
            environment = plan.selectedEnvironment,
            minMinutes = plan.filters.minMinutes,
            maxMinutes = plan.filters.maxMinutes,
            difficulty = plan.filters.desiredDifficulty,
            selectedTags = selectTags(tags),
            researchHints = selectResearchHints(researchHints),
        )
    }

    fun assembleTagLabels(
        plan: QuestGenerationPlan,
        tagLabels: List<String>,
        researchHints: List<PromptResearchHint> = emptyList(),
    ): BoundedPromptEnvelope {
        return BoundedPromptEnvelope(
            schemaVersion = QuestPromptContract.SCHEMA_VERSION,
            task = QuestPromptContract.TASK,
            category = plan.selectedCategory,
            environment = plan.selectedEnvironment,
            minMinutes = plan.filters.minMinutes,
            maxMinutes = plan.filters.maxMinutes,
            difficulty = plan.filters.desiredDifficulty,
            selectedTags = selectTagLabels(tagLabels),
            researchHints = selectResearchHints(researchHints),
        )
    }

    private fun selectTags(tags: List<Tag>): List<PromptTag> =
        selectTagLabels(tags.asSequence().filter { it.enabled }.map { it.label }.toList())

    private fun selectTagLabels(labels: List<String>): List<PromptTag> =
        labels.asSequence()
            .map(::sanitize)
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it }))
            .take(limits.maxTags)
            .map(::PromptTag)
            .toList()

    private fun selectResearchHints(
        hints: List<PromptResearchHint>,
    ): List<PromptResearchHint> =
        hints.asSequence()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { stableResearchKey(it) })
            .take(limits.maxResearchHints)
            .map(::sanitizeHint)
            .toList()

    private fun sanitize(value: String): String =
        value
            .replace(Regex("\\p{Cntrl}+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(limits.maxTagCharacters)

    private fun stableResearchKey(hint: PromptResearchHint): String =
        "${hint.canonicalName}:${hint.kind}".lowercase()
    private fun sanitizeHint(
        hint: PromptResearchHint,
    ): PromptResearchHint = when (hint) {
        is PromptResearchHint.Game -> hint.copy(
            canonicalName = sanitizeField(hint.canonicalName),
            platform = hint.platform?.let(::sanitizeField),
            genre = hint.genre?.let(::sanitizeField),
        )
        is PromptResearchHint.Flower -> hint.copy(
            canonicalName = sanitizeField(hint.canonicalName),
            commonName = hint.commonName?.let(::sanitizeField),
            region = hint.region?.let(::sanitizeField),
        )
    }

    private fun sanitizeField(value: String): String =
        value
            .replace(Regex("\\p{Cntrl}+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(limits.maxResearchFieldCharacters)

    companion object {
        fun limitsForPrompt(promptCharacters: Int): PromptContractLimits =
            PromptContractLimits(maxPromptCharacters = promptCharacters)
    }
}
