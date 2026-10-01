package com.pinhoquest.ui.tags

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource

object BuiltinTags {
    val all = listOf(
        system("coding", "Programação"),
        system("games", "Jogos"),
        system("creative", "Criatividade"),
        system("music", "Música"),
        system("learning", "Aprender"),
        system("exploration", "Explorar"),
        system("technology", "Tecnologia"),
        system("photography", "Fotografia"),
        system("outdoor", "Ao ar livre"),
        system("experiments", "Experimentos"),
    )

    fun byId(id: String): Tag? = all.firstOrNull { it.id.value == id }

    fun categoryAffinities(tags: List<Tag>): Map<QuestCategory, Double> =
        tags.asSequence()
            .filter { it.enabled }
            .mapNotNull { tag ->
                categoryFor(tag.id)?.let { category -> category to tag.affinity }
            }
            .toMap()

    private fun categoryFor(id: TagId): QuestCategory? = when (id.value) {
        "coding" -> QuestCategory.CODING
        "games" -> QuestCategory.GAMING
        "creative" -> QuestCategory.CREATIVE
        "learning" -> QuestCategory.LEARNING
        "exploration" -> QuestCategory.EXPLORATION
        else -> null
    }

    private fun system(id: String, label: String) = Tag(
        id = TagId(id),
        label = label,
        source = TagSource.SYSTEM,
        affinity = 0.8,
        enabled = false,
    )
}
