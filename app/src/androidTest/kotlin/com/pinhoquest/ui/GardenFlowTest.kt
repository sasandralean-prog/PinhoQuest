package com.pinhoquest.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.ui.garden.GardenFlowerUi
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.garden.GardenUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GardenFlowTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun gardenShowsAllDiscoveryStatesAndCollectedDetail() {
        var selected: String? = null
        val state = GardenUiState(
            ownerName = "Rafa",
            lifetimeXp = 425,
            spendableXp = 200,
            level = 4,
            collectedCount = 1,
            totalCount = 4,
            flowers = listOf(
                flower("hidden", "Flor Oculta", FlowerDiscoveryState.HIDDEN, FlowerRarity.UNKNOWN),
                flower("hinted", "Flor Pista", FlowerDiscoveryState.HINTED, FlowerRarity.RARE),
                flower("revealed", "Flor Revelada", FlowerDiscoveryState.REVEALED, FlowerRarity.UNCOMMON),
                flower("collected", "Rosa de Teste", FlowerDiscoveryState.COLLECTED, FlowerRarity.COMMON, 50),
            ),
            selectedFlowerId = "collected",
        )

        composeRule.setContent {
            MaterialTheme {
                GardenScreen(
                    state = state,
                    onFlowerSelected = { selected = it },
                    onInvestigate = {},
                )
            }
        }

        composeRule.onNodeWithText("🌷 Jardim de Rafa").assertIsDisplayed()
        composeRule.onNodeWithText("425 XP").assertIsDisplayed()
        composeRule.onNodeWithText("Nível 4").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 4 flores").assertIsDisplayed()
        composeRule.onNodeWithText("Ainda desconhecida").assertIsDisplayed()
        composeRule.onNodeWithText("Raridade: Rara").assertIsDisplayed()
        composeRule.onNodeWithText("Flor Revelada").assertIsDisplayed()
        composeRule.onNodeWithText("XP da quest: +50").assertIsDisplayed()
        composeRule.onNodeWithText("Conquistada com: Quest de Teste").assertIsDisplayed()
        composeRule.onNodeWithText("Raridade: Comum").assertIsDisplayed()
        composeRule.onNodeWithTag("flower-card-collected").performClick()
        assertEquals("collected", selected)
    }

    @Test
    fun investigationShowsCostAndInsufficientXpHumanely() {
        val flower = flower(
            "hidden",
            "Flor Oculta",
            FlowerDiscoveryState.HIDDEN,
            FlowerRarity.UNKNOWN,
            investigationCost = 90,
        )
        composeRule.setContent {
            MaterialTheme {
                GardenScreen(
                    state = GardenUiState(
                        ownerName = "Rafa",
                        lifetimeXp = 50,
                        spendableXp = 25,
                        level = 1,
                        collectedCount = 0,
                        totalCount = 1,
                        flowers = listOf(flower),
                        selectedFlowerId = "hidden",
                    ),
                    onFlowerSelected = {},
                    onInvestigate = {},
                )
            }
        }

        composeRule.onNodeWithText("Investigar — 90 XP").assertIsDisplayed()
        composeRule.onNodeWithText("Faltam 65 XP para investigar esta flor.").assertIsDisplayed()
    }

    private fun flower(
        id: String,
        name: String,
        state: FlowerDiscoveryState,
        rarity: FlowerRarity,
        xpAward: Int? = null,
        investigationCost: Int? = if (
            state == FlowerDiscoveryState.HIDDEN || state == FlowerDiscoveryState.HINTED
        ) 75 else null,
    ) = GardenFlowerUi(
        id = id,
        commonName = name,
        scientificName = "Specia testii",
        description = "Descrição de teste",
        rarity = rarity,
        discoveryState = state,
        xpAward = xpAward,
        acquiredAtEpochMillis = if (state == FlowerDiscoveryState.COLLECTED) 1L else null,
        questTitle = if (state == FlowerDiscoveryState.COLLECTED) "Quest de Teste" else null,
        investigationCost = investigationCost,
    )
    @Test
    fun collectionFiltersRemainAccessibleAtCompactWidth() {
        val state = GardenUiState(
            ownerName = "Rafa",
            lifetimeXp = 120,
            spendableXp = 20,
            level = 2,
            collectedCount = 1,
            totalCount = 4,
            flowers = listOf(
                flower("hidden-filter", "Flor Oculta", FlowerDiscoveryState.HIDDEN, FlowerRarity.UNKNOWN),
                flower("hinted-filter", "Flor Pesquisada", FlowerDiscoveryState.HINTED, FlowerRarity.RARE),
                flower("revealed-filter", "Flor Revelada", FlowerDiscoveryState.REVEALED, FlowerRarity.UNCOMMON),
                flower("collected-filter", "Flor Coletada", FlowerDiscoveryState.COLLECTED, FlowerRarity.COMMON, 40),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp).height(720.dp)) {
                    GardenScreen(
                        state = state,
                        onFlowerSelected = {},
                        onInvestigate = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Abrir coleção").performClick()
        listOf("Todas", "Coletadas", "Pesquisadas", "Desconhecidas").forEach { label ->
            composeRule.onNodeWithContentDescription(label).assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("Desconhecidas").performClick()
        composeRule.onNodeWithTag("flower-card-hidden-filter").assertIsDisplayed()
    }

}
