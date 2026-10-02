package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.garden.GardenUiState
import org.junit.Rule
import org.junit.Test

class GardenLargeFontTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun gardenHeaderRemainsVisibleAtLargeFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.8f),
            ) {
                MaterialTheme {
                    GardenScreen(
                        state = GardenUiState(
                            ownerName = "Rafa",
                            lifetimeXp = 0,
                            spendableXp = 0,
                            level = 1,
                            collectedCount = 0,
                            totalCount = 0,
                            flowers = emptyList(),
                        ),
                        onFlowerSelected = {},
                        onInvestigate = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("🌷 Jardim de Rafa").assertIsDisplayed()
        composeRule.onNodeWithText("Seu jardim ainda está quietinho.").assertIsDisplayed()
    }
}
