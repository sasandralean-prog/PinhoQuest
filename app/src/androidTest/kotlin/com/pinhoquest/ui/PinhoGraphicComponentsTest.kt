package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.pinhoquest.ui.navigation.MainTab
import com.pinhoquest.domain.tag.TagId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertTrue(composeRule.onAllNodesWithText("⌂", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("🌷", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("▣", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
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
    @Test
    fun everyVisiblePreferenceHasItsApprovedCanonicalArtwork() {
        assertEquals(com.pinhoquest.R.drawable.category_music, tagGraphicAsset(TagId("music")))
        assertEquals(com.pinhoquest.R.drawable.category_photography, tagGraphicAsset(TagId("photography")))
        assertEquals(com.pinhoquest.R.drawable.category_nature, tagGraphicAsset(TagId("nature")))
        assertEquals(com.pinhoquest.R.drawable.category_animal, tagGraphicAsset(TagId("animals")))
        assertEquals(com.pinhoquest.R.drawable.category_learn, tagGraphicAsset(TagId("learning")))
        assertEquals(com.pinhoquest.R.drawable.category_science, tagGraphicAsset(TagId("technology")))
        assertEquals(com.pinhoquest.R.drawable.category_appreciation, tagGraphicAsset(TagId("relax")))
        assertEquals(com.pinhoquest.R.drawable.category_creativity, tagGraphicAsset(TagId("create")))
        assertEquals(com.pinhoquest.R.drawable.category_games, tagGraphicAsset(TagId("fantasy")))
    }

    @Test
    fun categoryButtonTogglesSelectionAndRemainsAccessible() {
        val selected = mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                PinhoTagGraphicButton(
                    tagId = TagId("fantasy"),
                    contentDescription = "Fantasia",
                    selected = selected.value,
                    onClick = { selected.value = !selected.value },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                )
            }
        }
        composeRule.onNodeWithContentDescription("Fantasia").assertIsDisplayed().performClick()
        assertEquals(true, selected.value)
        composeRule.onNodeWithContentDescription("Fantasia").assertIsDisplayed()
    }

}
