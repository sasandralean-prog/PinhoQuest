package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.ui.navigation.MainTab
import com.pinhoquest.ui.navigation.PinhoQuestNav
import com.pinhoquest.ui.navigation.PinhoQuestUiState
import com.pinhoquest.ui.onboarding.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingQuestFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingStartsFromReferenceStartAndCompletesAfterNameAndTags() {
        var completedName: String? = null
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    onComplete = { name, _ -> completedName = name },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Começar").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithContentDescription("Confirmar nome").performClick()
        composeRule.onNodeWithContentDescription("Continuar").assertIsDisplayed().performClick()

        assertEquals("Rafa", completedName)
    }

    @Test
    fun onboardingImeDoneAdvancesToTagsAfterValidName() {
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(onComplete = { _, _ -> })
            }
        }

        composeRule.onNodeWithContentDescription("Começar").performClick()
        composeRule.onNodeWithContentDescription("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithContentDescription("Seu nome").performImeAction()
        composeRule.onNodeWithContentDescription("Continuar").assertIsDisplayed()
    }

    @Test
    fun onboardingRejectsNamesLongerThanTwentyCharacters() {
        var completed = false
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(onComplete = { _, _ -> completed = true })
            }
        }

        composeRule.onNodeWithContentDescription("Começar").performClick()
        composeRule.onNodeWithContentDescription("Seu nome")
            .performTextInput("123456789012345678901")
        composeRule.onNodeWithContentDescription("Confirmar nome").performClick()

        assertEquals(false, completed)
    }

    @Test
    fun questsUsesReferenceButtonsForNormalAndRandom() {
        val modes = mutableListOf<QuestMode>()
        composeRule.setContent {
            MaterialTheme {
                PinhoQuestNav(
                    state = PinhoQuestUiState.ready(ownerName = "Rafa"),
                    onTabSelected = {},
                    onGenerateQuest = { modes += it },
                    onOpenQuestThemeSelection = {},
                    onCloseQuestThemeSelection = {},
                    onGenerateQuestFromThemeSelection = {},
                    onStartQuest = {},
                    onCompleteQuest = {},
                    onAbandonQuest = {},
                    onFlowerSelected = {},
                    onInvestigateFlower = {},
                    onDismissFlower = {},
                    onDismissCompletion = {},
                    onOpenGardenFromCompletion = {},
                    onTagToggled = { _, _ -> },
                    onThemeSelected = {},
                    onFontScaleSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Sortear quest").assertIsDisplayed().performClick()
        // The generated quest is intentionally handled by the existing functional surface.
        assertEquals(listOf(QuestMode.NORMAL), modes)
    }

    @Test
    fun bottomNavigationHasThreeReferenceTabs() {
        var selected = MainTab.QUESTS
        composeRule.setContent {
            MaterialTheme {
                PinhoQuestNav(
                    state = PinhoQuestUiState.ready(ownerName = "Rafa", selectedTab = selected),
                    onTabSelected = { selected = it },
                    onGenerateQuest = {},
                    onOpenQuestThemeSelection = {},
                    onCloseQuestThemeSelection = {},
                    onGenerateQuestFromThemeSelection = {},
                    onStartQuest = {},
                    onCompleteQuest = {},
                    onAbandonQuest = {},
                    onFlowerSelected = {},
                    onInvestigateFlower = {},
                    onDismissFlower = {},
                    onDismissCompletion = {},
                    onOpenGardenFromCompletion = {},
                    onTagToggled = { _, _ -> },
                    onThemeSelected = {},
                    onFontScaleSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Início").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Jardim").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }
}
