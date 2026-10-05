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
import androidx.compose.ui.clearAndSetSemantics
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.garden.GardenUiState
import com.pinhoquest.ui.quests.QuestCompletionDialog
import com.pinhoquest.ui.quests.QuestScreen
import com.pinhoquest.ui.settings.SettingsScreen
import com.pinhoquest.ui.tags.TagsScreen

@Composable
fun PinhoQuestNav(
    state: PinhoQuestUiState,
    onTabSelected: (MainTab) -> Unit,
    onGenerateQuest: (QuestMode) -> Unit,
    onStartQuest: () -> Unit,
    onCompleteQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    onFlowerSelected: (String) -> Unit,
    onInvestigateFlower: (String) -> Unit,
    onDismissFlower: () -> Unit,
    onDismissCompletion: () -> Unit,
    onOpenGardenFromCompletion: () -> Unit,
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
                        icon = {
                            Text(
                                text = tab.emoji,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        },
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
                    onCompleteQuest = onCompleteQuest,
                    onAbandonQuest = onAbandonQuest,
                )
                MainTab.TAGS -> TagsScreen(
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                )
                MainTab.GARDEN -> GardenScreen(
                    state = state.garden ?: GardenUiState(
                        ownerName = state.ownerName,
                        lifetimeXp = 0,
                        spendableXp = 0,
                        level = 1,
                        collectedCount = 0,
                        totalCount = 0,
                        flowers = emptyList(),
                    ),
                    onFlowerSelected = onFlowerSelected,
                    onInvestigate = onInvestigateFlower,
                    onDismissFlower = onDismissFlower,
                )
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

    state.completionDialog?.let { completion ->
        QuestCompletionDialog(
            completion = completion,
            onDismiss = onDismissCompletion,
            onOpenGarden = onOpenGardenFromCompletion,
        )
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
