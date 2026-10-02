package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pinhoquest.ui.navigation.PinhoQuestNav
import com.pinhoquest.ui.navigation.PinhoQuestUiState
import org.junit.Rule
import org.junit.Test

class LargeFontLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun questHomePrimaryContentRemainsVisibleAtLargeFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
            ) {
                MaterialTheme {
                    PinhoQuestNav(
                        state = PinhoQuestUiState.ready(ownerName = "Rafa"),
                        onTabSelected = {},
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
        }

        composeRule.onNodeWithText("O que vamos inventar hoje?").assertIsDisplayed()
        composeRule.onNodeWithText("SORTEAR QUEST").assertIsDisplayed()
        composeRule.onNodeWithText("Quest Aleatória").assertIsDisplayed()
    }
}
