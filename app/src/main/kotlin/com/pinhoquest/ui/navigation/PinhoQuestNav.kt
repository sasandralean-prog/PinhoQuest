package com.pinhoquest.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.PinhoBottomNavigation
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.profile.ProfileScreen
import com.pinhoquest.ui.quests.QuestCompletionDialog
import com.pinhoquest.ui.quests.QuestScreen
import com.pinhoquest.ui.settings.SettingsScreen

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
    onBackup: (() -> Unit)? = null,
    onDonate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state.selectedTab) {
            MainTab.QUESTS -> {
                QuestScreen(
                    ownerName = state.ownerName,
                    currentQuest = state.currentQuest,
                    activeSession = state.activeSession,
                    loading = state.loading,
                    onGenerateQuest = onGenerateQuest,
                    onStartQuest = onStartQuest,
                    onCompleteQuest = onCompleteQuest,
                    onAbandonQuest = onAbandonQuest,
                    flowerCount = state.garden?.collectedCount ?: 0,
                    lifetimeXp = state.garden?.lifetimeXp ?: 0,
                    dark = state.theme == ThemePreference.DARK,
                )
            }

            MainTab.GARDEN -> {
                state.garden?.let { garden ->
                    GardenScreen(
                        state = garden,
                        onFlowerSelected = onFlowerSelected,
                        onInvestigate = onInvestigateFlower,
                        onDismissFlower = onDismissFlower,
                        onBack = { onTabSelected(MainTab.QUESTS) },
                    )
                }
            }

            MainTab.PROFILE -> {
                ProfileScreen(
                    ownerName = state.ownerName,
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                    onOpenSettings = { onTabSelected(MainTab.SETTINGS) },
                )
            }

            MainTab.SETTINGS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    PinhoQuestBackground(PinhoQuestBackgrounds.ROOM, overlay = androidx.compose.ui.graphics.Color.Black, overlayAlpha = 0.08f)
                    SettingsScreen(
                        ownerName = state.ownerName,
                        theme = state.theme,
                        fontScale = state.fontScale,
                        onThemeSelected = onThemeSelected,
                        onFontScaleSelected = onFontScaleSelected,
                        onBackup = onBackup,
                        onDonate = onDonate,
                        onBack = { onTabSelected(MainTab.PROFILE) },
                    )
                }
            }

            MainTab.TAGS -> {
                ProfileScreen(
                    ownerName = state.ownerName,
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                    onOpenSettings = { onTabSelected(MainTab.SETTINGS) },
                )
            }
        }

        PinhoBottomNavigation(
            selectedTab = when (state.selectedTab) {
                MainTab.GARDEN -> MainTab.GARDEN
                MainTab.PROFILE, MainTab.SETTINGS, MainTab.TAGS -> MainTab.PROFILE
                MainTab.QUESTS -> MainTab.QUESTS
            },
            onTabSelected = onTabSelected,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
        )

        state.completionDialog?.let { completion ->
            QuestCompletionDialog(
                completion = completion,
                onDismiss = onDismissCompletion,
                onOpenGarden = onOpenGardenFromCompletion,
            )
        }
    }
}
