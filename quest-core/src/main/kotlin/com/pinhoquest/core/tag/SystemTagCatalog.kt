package com.pinhoquest.core.tag

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource

/**
 * Canonical product themes shown by the PinhoQuest visual language.
 *
 * The UI renders these as themes; the quest engine still consumes the governed
 * tag/category affinities below. Additional user-created tags are stored
 * separately and are not silently invented here.
 */
object SystemTagCatalog {
    val all = listOf(
        system("games", "Jogos", QuestCategory.GAMING),
        system("creative", "Criatividade", QuestCategory.CREATIVE),
        system("learning", "Aprender", QuestCategory.LEARNING),
        system("music", "Música", QuestCategory.CREATIVE),
        system("photography", "Fotografia", QuestCategory.EXPLORATION),
        system("nature", "Natureza", QuestCategory.EXPLORATION),
        system("technology", "Tecnologia", QuestCategory.CODING),
        system("animals", "Animais", QuestCategory.EXPLORATION),
        system("adventures", "Aventuras", QuestCategory.EXPLORATION),
        system("relax", "Relaxar", QuestCategory.LEARNING),
        system("create", "Criar", QuestCategory.CREATIVE),
        system("fantasy", "Fantasia", QuestCategory.CREATIVE),
    )

    fun byId(id: String): Tag? = all.firstOrNull { it.id.value == id }

    fun categoryAffinities(tags: List<Tag>): Map<QuestCategory, Double> =
        tags.asSequence()
            .filter { it.enabled }
            .mapNotNull { tag ->
                categoryFor(tag.id)?.let { category -> category to tag.affinity }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, affinities) -> affinities.sum() }

    private fun categoryFor(id: TagId): QuestCategory? = when (id.value) {
        "games" -> QuestCategory.GAMING
        "creative", "music", "create", "fantasy" -> QuestCategory.CREATIVE
        "learning", "relax" -> QuestCategory.LEARNING
        "photography", "nature", "animals", "adventures" -> QuestCategory.EXPLORATION
        "technology" -> QuestCategory.CODING
        else -> null
    }

    private fun system(
        id: String,
        label: String,
        category: QuestCategory,
    ) = Tag(
        id = TagId(id),
        label = label,
        source = TagSource.SYSTEM,
        affinity = 0.8,
        enabled = false,
    )
}
