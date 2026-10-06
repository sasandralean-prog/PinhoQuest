package com.pinhoquest.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pinhoquest.R
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.garden.GardenUiState
import com.pinhoquest.ui.profile.ProfileScreen
import com.pinhoquest.ui.quests.QuestCompletionDialog
import com.pinhoquest.ui.quests.QuestScreen
import com.pinhoquest.ui.settings.SettingsScreen
import com.pinhoquest.ui.reference.ReferenceHotspot
import com.pinhoquest.ui.reference.ReferenceRect

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

    Box(modifier = modifier.fillMaxSize()) {
        when (state.selectedTab) {
            MainTab.QUESTS -> {
                if (state.currentQuest != null || state.activeSession != null) {
                    QuestScreen(
                        ownerName = state.ownerName,
                        currentQuest = state.currentQuest,
                        activeSession = state.activeSession,
                        loading = state.loading,
                        onGenerateQuest = onGenerateQuest,
                        onStartQuest = onStartQuest,
                        onCompleteQuest = onCompleteQuest,
                        onAbandonQuest = onAbandonQuest,
                    )
                } else {
                    ReferenceBackground(R.drawable.bg_home)
                    ReferenceHotspot(
                        rect = ReferenceRect(0.13f, 0.57f, 0.74f, 0.065f),
                        contentDescription = "Sortear quest",
                        onClick = { onGenerateQuest(QuestMode.NORMAL) },
                    )
                    ReferenceHotspot(
                        rect = ReferenceRect(0.13f, 0.655f, 0.35f, 0.08f),
                        contentDescription = "Quest de jogo",
                        onClick = { onGenerateQuest(QuestMode.GAME) },
                    )
                    ReferenceHotspot(
                        rect = ReferenceRect(0.52f, 0.655f, 0.35f, 0.08f),
                        contentDescription = "Quest aleatória",
                        onClick = { onGenerateQuest(QuestMode.RANDOM) },
                    )
                }
            }

            MainTab.GARDEN -> {
                ReferenceBackground(R.drawable.bg_garden)
                state.garden?.flowers?.take(24)?.forEachIndexed { index, flower ->
                    val column = index % 4
                    val row = index / 4
                    ReferenceHotspot(
                        rect = ReferenceRect(
                            left = 0.05f + column * 0.24f,
                            top = 0.20f + row * 0.12f,
                            width = 0.20f,
                            height = 0.105f,
                        ),
                        contentDescription = "Flor " + flower.commonName,
                        onClick = { onFlowerSelected(flower.id) },
                    )
                }
            }

            MainTab.PROFILE -> {
                ProfileScreen(
                    tags = state.tags,
                    onTagToggled = onTagToggled,
                    onOpenSettings = { onTabSelected(MainTab.SETTINGS) },
                )
            }

            MainTab.SETTINGS -> {
                ReferenceBackground(R.drawable.bg_settings)
                ReferenceHotspot(
                    rect = ReferenceRect(0.13f, 0.27f, 0.30f, 0.12f),
                    contentDescription = "Tema claro",
                    onClick = { onThemeSelected(ThemePreference.LIGHT) },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.55f, 0.27f, 0.30f, 0.12f),
                    contentDescription = "Tema escuro",
                    onClick = { onThemeSelected(ThemePreference.DARK) },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.16f, 0.48f, 0.24f, 0.08f),
                    contentDescription = "Texto menor",
                    onClick = { onFontScaleSelected(0.9f) },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.39f, 0.48f, 0.24f, 0.08f),
                    contentDescription = "Texto médio",
                    onClick = { onFontScaleSelected(1.0f) },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.62f, 0.48f, 0.24f, 0.08f),
                    contentDescription = "Texto maior",
                    onClick = { onFontScaleSelected(1.15f) },
                )
            }

            // Kept for compatibility with existing tests and callers; not exposed by the new bottom navigation.
            MainTab.TAGS -> {
                ReferenceBackground(R.drawable.bg_onboarding_tags)
                state.tags.take(12).forEachIndexed { index, tag ->
                    val column = index % 3
                    val row = index / 3
                    ReferenceHotspot(
                        rect = ReferenceRect(
                            left = 0.18f + column * 0.25f,
                            top = 0.40f + row * 0.10f,
                            width = 0.22f,
                            height = 0.085f,
                        ),
                        contentDescription = tag.label,
                        onClick = { onTagToggled(tag.id, !tag.enabled) },
                    )
                }
            }
        }

        ReferenceBottomNavigation(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected,
        )

        SnackbarHost(hostState = snackbarHostState)

        state.completionDialog?.let { completion ->
            QuestCompletionDialog(
                completion = completion,
                onDismiss = onDismissCompletion,
                onOpenGarden = onOpenGardenFromCompletion,
            )
        }
    }
}

@Composable
private fun ReferenceBackground(resource: Int) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ReferenceBottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    ReferenceHotspot(
        rect = ReferenceRect(0.00f, 0.885f, 0.333f, 0.115f),
        contentDescription = "Início",
        onClick = { onTabSelected(MainTab.QUESTS) },
    )
    ReferenceHotspot(
        rect = ReferenceRect(0.333f, 0.885f, 0.334f, 0.115f),
        contentDescription = "Jardim",
        onClick = { onTabSelected(MainTab.GARDEN) },
    )
    ReferenceHotspot(
        rect = ReferenceRect(0.667f, 0.885f, 0.333f, 0.115f),
        contentDescription = "Perfil",
        onClick = { onTabSelected(MainTab.PROFILE) },
    )
}
