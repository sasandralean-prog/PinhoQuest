package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
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
    fun onboardingValidatesGardenNameBeforeCompletion() {
        var completedName: String? = null
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(
                    onComplete = { name, _ -> completedName = name },
                )
            }
        }

        composeRule.onNodeWithText("Criar meu jardim").assertIsNotEnabled()
        composeRule.onNodeWithText("Seu nome").performTextInput("123456789012345678901")
        composeRule.onNodeWithText("Use até 20 caracteres.").assertIsDisplayed()
        composeRule.onNodeWithText("Criar meu jardim").assertIsNotEnabled()

        composeRule.onNodeWithText("Seu nome").performTextClearance()
        composeRule.onNodeWithText("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithText("Criar meu jardim").assertIsEnabled().performClick()

        assertEquals("Rafa", completedName)
    }

    @Test
    fun onboardingCountsUnicodeCodePointsConsistently() {
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(onComplete = { _, _ -> })
            }
        }

        composeRule.onNodeWithText("Seu nome").performTextInput("🌷".repeat(20))

        composeRule.onAllNodesWithText("Use até 20 caracteres.").assertCountEquals(0)
        composeRule.onNodeWithText("Criar meu jardim").assertIsEnabled()
    }

    @Test
    fun questsIsDefaultAndNormalAndRandomUseOneSurface() {
        val modes = mutableListOf<QuestMode>()
        composeRule.setContent {
            MaterialTheme {
                PinhoQuestNav(
                    state = PinhoQuestUiState.ready(ownerName = "Rafa"),
                    onTabSelected = {},
                    onGenerateQuest = { modes += it },
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

        composeRule.onNodeWithText("O que vamos inventar hoje?").assertIsDisplayed()
        composeRule.onNodeWithText("SORTEAR QUEST").performClick()
        composeRule.onNodeWithText("Quest Aleatória").performClick()

        assertEquals(listOf(QuestMode.NORMAL, QuestMode.RANDOM), modes)
    }

    @Test
    fun bottomNavigationHasFourTabsAndGardenUsesOwnerName() {
        var selected = MainTab.QUESTS
        composeRule.setContent {
            MaterialTheme {
                PinhoQuestNav(
                    state = PinhoQuestUiState.ready(ownerName = "Rafa", selectedTab = selected),
                    onTabSelected = { selected = it },
                    onGenerateQuest = {},
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

        composeRule.onNodeWithText("Quests").assertIsDisplayed()
        composeRule.onNodeWithText("Tags").assertIsDisplayed()
        composeRule.onNodeWithText("Jardim").assertIsDisplayed()
        composeRule.onNodeWithText("Configurações").assertIsDisplayed()
    }
}
