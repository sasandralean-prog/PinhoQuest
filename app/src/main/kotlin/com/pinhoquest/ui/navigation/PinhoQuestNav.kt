package com.pinhoquest.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.LocalPinhoBottomNavigationInset
import com.pinhoquest.ui.PinhoBottomNavigation
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.profile.ProfileScreen
import com.pinhoquest.ui.quests.QuestCompletionDialog
import com.pinhoquest.ui.quests.QuestScreen
import com.pinhoquest.ui.settings.SettingsScreen
import com.pinhoquest.ui.tags.QuestThemeSelectionScreen

@Composable
fun PinhoQuestNav(
    state: PinhoQuestUiState,
    onTabSelected: (MainTab) -> Unit,
    onGenerateQuest: (QuestMode) -> Unit,
    onOpenQuestThemeSelection: () -> Unit,
    onCloseQuestThemeSelection: () -> Unit,
    onGenerateQuestFromThemeSelection: () -> Unit,
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
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val navigationAspectRatio = if (dark) 798f / 167f else 768f / 181f
        val systemNavigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val reservedNavigationSpace = if (state.questThemeSelectionOpen) {
            0.dp
        } else {
            maxWidth / navigationAspectRatio + systemNavigationInset
        }

        CompositionLocalProvider(
            LocalPinhoBottomNavigationInset provides reservedNavigationSpace,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
        if (state.questThemeSelectionOpen) {
            QuestThemeSelectionScreen(
                tags = state.tags,
                onTagToggled = onTagToggled,
                onConfirm = onGenerateQuestFromThemeSelection,
                onBack = onCloseQuestThemeSelection,
                dark = dark,
            )
        } else when (state.selectedTab) {
            MainTab.QUESTS -> {
                QuestScreen(
                    ownerName = state.ownerName,
                    currentQuest = state.currentQuest,
                    activeSession = state.activeSession,
                    loading = state.loading,
                    onGenerateQuest = onGenerateQuest,
                    onOpenThemeSelection = onOpenQuestThemeSelection,
                    onStartQuest = onStartQuest,
                    onCompleteQuest = onCompleteQuest,
                    onAbandonQuest = onAbandonQuest,
                    flowerCount = state.garden?.collectedCount ?: 0,
                    lifetimeXp = state.garden?.lifetimeXp ?: 0,
                    dark = dark,
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
                        dark = dark,
                    )
                }
            }

            MainTab.PROFILE -> {
                ProfileScreen(
                    ownerName = state.ownerName,
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                    onOpenSettings = { onTabSelected(MainTab.SETTINGS) },
                    dark = dark,
                )
            }

            MainTab.SETTINGS -> {
                SettingsScreen(
                    ownerName = state.ownerName,
                    theme = state.theme,
                    fontScale = state.fontScale,
                    effectiveDark = dark,
                    onThemeSelected = onThemeSelected,
                    onFontScaleSelected = onFontScaleSelected,
                    onBackup = onBackup,
                    onDonate = onDonate,
                    onBack = { onTabSelected(MainTab.PROFILE) },
                )
            }

            MainTab.TAGS -> {
                ProfileScreen(
                    ownerName = state.ownerName,
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                    onOpenSettings = { onTabSelected(MainTab.SETTINGS) },
                    dark = dark,
                )
            }
        }

        if (!state.questThemeSelectionOpen) PinhoBottomNavigation(
            selectedTab = when (state.selectedTab) {
                MainTab.GARDEN -> MainTab.GARDEN
                MainTab.PROFILE, MainTab.SETTINGS, MainTab.TAGS -> MainTab.PROFILE
                MainTab.QUESTS -> MainTab.QUESTS
            },
            onTabSelected = onTabSelected,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            dark = dark,
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
    }
}
