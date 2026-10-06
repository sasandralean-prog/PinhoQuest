package com.pinhoquest.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.garden.GardenPlaceholderScreen
import com.pinhoquest.ui.quests.QuestScreen
import com.pinhoquest.ui.settings.SettingsScreen
import com.pinhoquest.ui.tags.TagsScreen

@Composable
fun PinhoQuestNav(
    state: PinhoQuestUiState,
    onTabSelected: (MainTab) -> Unit,
    onGenerateQuest: (QuestMode) -> Unit,
    onStartQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    onTagToggled: (TagId, Boolean) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontScaleSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.takeIf { it.isNotBlank() }?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Text(tab.emoji) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.selectedTab) {
                MainTab.QUESTS -> QuestScreen(
                    ownerName = state.ownerName,
                    currentQuest = state.currentQuest,
                    activeSession = state.activeSession,
                    loading = state.loading,
                    onGenerateQuest = onGenerateQuest,
                    onStartQuest = onStartQuest,
                    onAbandonQuest = onAbandonQuest,
                )
                MainTab.TAGS -> TagsScreen(
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                )
                MainTab.GARDEN -> GardenPlaceholderScreen(state.ownerName)
                MainTab.SETTINGS -> SettingsScreen(
                    ownerName = state.ownerName,
                    theme = state.theme,
                    fontScale = state.fontScale,
                    onThemeSelected = onThemeSelected,
                    onFontScaleSelected = onFontScaleSelected,
                )
            }
        }
    }
}

private val MainTab.label: String
    get() = when (this) {
        MainTab.QUESTS -> "Quests"
        MainTab.TAGS -> "Tags"
        MainTab.GARDEN -> "Jardim"
        MainTab.SETTINGS -> "Configurações"
    }

private val MainTab.emoji: String
    get() = when (this) {
        MainTab.QUESTS -> "🎲"
        MainTab.TAGS -> "🏷"
        MainTab.GARDEN -> "🌷"
        MainTab.SETTINGS -> "⚙"
    }
