package com.pinhoquest.ui.garden

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity

@Composable
fun GardenScreen(
    state: GardenUiState,
    onFlowerSelected: (String) -> Unit,
    onInvestigate: (String) -> Unit,
    onDismissFlower: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🌷 Jardim de " + state.ownerName, style = MaterialTheme.typography.headlineSmall)
        Text(state.lifetimeXp.toString() + " XP")
        Text("Nível " + state.level)
        Text(state.collectedCount.toString() + " / " + state.totalCount + " flores")

        if (state.flowers.isEmpty()) {
            Text("Seu jardim ainda está quietinho.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.flowers, key = { it.id }) { flower ->
                    GardenFlowerCard(
                        flower = flower,
                        onClick = { onFlowerSelected(flower.id) },
                    )
                }
            }
        }
    }

    state.selectedFlower?.let { flower ->
        FlowerDetailScreen(
            flower = flower,
            spendableXp = state.spendableXp,
            onInvestigate = { onInvestigate(flower.id) },
            onDismiss = onDismissFlower,
        )
    }
}

@Composable
private fun GardenFlowerCard(
    flower: GardenFlowerUi,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flower-card-" + flower.id)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (flower.discoveryState) {
                FlowerDiscoveryState.HIDDEN -> {
                    Text("❔", style = MaterialTheme.typography.headlineSmall)
                    Text("Ainda desconhecida")
                    Text("Raridade: ???")
                }
                FlowerDiscoveryState.HINTED -> {
                    Text("❔", style = MaterialTheme.typography.headlineSmall)
                    Text("Uma pista apareceu")
                    Text("Raridade: " + flower.rarity.label)
                }
                FlowerDiscoveryState.REVEALED -> {
                    Text("🌱", style = MaterialTheme.typography.headlineSmall)
                    Text(flower.commonName, style = MaterialTheme.typography.titleMedium)
                    Text("Raridade: " + flower.rarity.label)
                }
                FlowerDiscoveryState.COLLECTED -> {
                    Text("🌷", style = MaterialTheme.typography.headlineSmall)
                    Text(flower.commonName, style = MaterialTheme.typography.titleMedium)
                    Text(flower.rarity.label)
                }
            }
        }
    }
}

internal val FlowerRarity.label: String
    get() = when (this) {
        FlowerRarity.COMMON -> "Comum"
        FlowerRarity.UNCOMMON -> "Incomum"
        FlowerRarity.RARE -> "Rara"
        FlowerRarity.RAREST -> "Raríssima"
        FlowerRarity.UNKNOWN -> "???"
    }
