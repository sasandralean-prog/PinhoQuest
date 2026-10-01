package com.pinhoquest.ui.navigation

import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.tag.Tag

enum class MainTab {
    QUESTS,
    TAGS,
    GARDEN,
    SETTINGS,
}

data class PinhoQuestUiState(
    val onboardingRequired: Boolean = true,
    val ownerName: String = "",
    val selectedTab: MainTab = MainTab.QUESTS,
    val currentQuest: Quest? = null,
    val activeSession: QuestSession? = null,
    val tags: List<Tag> = emptyList(),
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val fontScale: Float = 1.0f,
    val message: String? = null,
    val loading: Boolean = false,
) {
    companion object {
        fun ready(
            ownerName: String,
            selectedTab: MainTab = MainTab.QUESTS,
        ) = PinhoQuestUiState(
            onboardingRequired = false,
            ownerName = ownerName,
            selectedTab = selectedTab,
        )
    }
}
