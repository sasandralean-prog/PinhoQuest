package com.pinhoquest.ui.navigation

import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.ui.garden.GardenUiState
import com.pinhoquest.ui.quests.QuestCompletionUi

enum class MainTab {
    QUESTS,
    TAGS,
    GARDEN,
    SETTINGS,
}

data class PinhoQuestUiState(
    val initializing: Boolean = true,
    val onboardingRequired: Boolean = false,
    val ownerName: String = "",
    val selectedTab: MainTab = MainTab.QUESTS,
    val currentQuest: Quest? = null,
    val activeSession: QuestSession? = null,
    val tags: List<Tag> = emptyList(),
    val garden: GardenUiState? = null,
    val completionDialog: QuestCompletionUi? = null,
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
            initializing = false,
            onboardingRequired = false,
            ownerName = ownerName,
            selectedTab = selectedTab,
        )
    }
}
