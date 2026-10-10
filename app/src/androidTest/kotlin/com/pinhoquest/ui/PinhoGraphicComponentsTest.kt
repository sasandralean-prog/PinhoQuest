package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pinhoquest.ui.navigation.MainTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PinhoGraphicComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomNavigationUsesArtworkIconsAndKeepsThreeAccessibleDestinations() {
        var selectedTab: MainTab? = null

        composeRule.setContent {
            MaterialTheme {
                PinhoBottomNavigation(
                    selectedTab = MainTab.QUESTS,
                    onTabSelected = { selectedTab = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Início").assertIsDisplayed().performClick()
        assertEquals(MainTab.QUESTS, selectedTab)

        composeRule.onNodeWithContentDescription("Jardim").assertIsDisplayed().performClick()
        assertEquals(MainTab.GARDEN, selectedTab)

        composeRule.onNodeWithContentDescription("Perfil").assertIsDisplayed().performClick()
        assertEquals(MainTab.PROFILE, selectedTab)

        // The PNG supplies the glyphs. Compose should expose labels, not draw the
        // old text glyphs on top of the same artwork.
        composeRule.onNodeWithText("⌂", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("🌷", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("▣", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun graphicButtonWithReservedArtworkSpaceRemainsAccessibleAndClickable() {
        var clicked = false

        composeRule.setContent {
            MaterialTheme {
                PinhoGraphicButton(
                    resource = com.pinhoquest.R.drawable.btn_confirm,
                    contentDescription = "Confirmar teste",
                    onClick = { clicked = true },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    aspectRatio = 187f / 86f,
                    label = "Confirmar",
                    labelStartFraction = 0.28f,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Confirmar teste").assertIsDisplayed().performClick()
        assertEquals(true, clicked)
    }
}
