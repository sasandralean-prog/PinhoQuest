package com.pinhoquest.core.tag

import com.pinhoquest.domain.quest.QuestCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemTagCatalogTest {
    @Test
    fun enabledSystemTagsProduceQuestCategoryAffinities() {
        val coding = SystemTagCatalog.all.first { it.id.value == "coding" }.copy(enabled = true)
        val learning = SystemTagCatalog.all.first { it.id.value == "learning" }.copy(enabled = true)

        val affinities = SystemTagCatalog.categoryAffinities(listOf(coding, learning))

        assertEquals(
            mapOf(
                QuestCategory.CODING to 0.8,
                QuestCategory.LEARNING to 0.8,
            ),
            affinities,
        )
    }

    @Test
    fun disabledAndNonQuestTagsDoNotInfluenceQuestCategories() {
        val coding = SystemTagCatalog.all.first { it.id.value == "coding" }.copy(enabled = false)
        val music = SystemTagCatalog.all.first { it.id.value == "music" }.copy(enabled = true)

        assertEquals(emptyMap<QuestCategory, Double>(), SystemTagCatalog.categoryAffinities(listOf(coding, music)))
    }
}
