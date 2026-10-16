package com.pinhoquest.core.tag

import com.pinhoquest.domain.quest.QuestCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTagCatalogTest {
    @Test
    fun canonicalThemes_match_visualOrder() {
        assertEquals(
            listOf(
                "Jogos",
                "Criatividade",
                "Aprender",
                "Música",
                "Fotografia",
                "Natureza",
                "Tecnologia",
                "Animais",
                "Aventuras",
                "Relaxar",
                "Criar",
                "Fantasia",
            ),
            SystemTagCatalog.all.map { it.label },
        )
    }

    @Test
    fun enabledThemes_accumulate_governed_category_affinities() {
        val enabled = SystemTagCatalog.all
            .filter { it.id.value in setOf("creative", "music", "nature") }
            .map { it.copy(enabled = true) }

        val affinities = SystemTagCatalog.categoryAffinities(enabled)

        assertEquals(1.6, affinities[QuestCategory.CREATIVE])
        assertEquals(0.8, affinities[QuestCategory.EXPLORATION])
        assertTrue(affinities.keys.size == 2)
    }
}
