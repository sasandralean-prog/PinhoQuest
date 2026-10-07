package com.pinhoquest.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoCreamSoft
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoBackButton

@Composable
fun GardenScreen(
    state: GardenUiState,
    onFlowerSelected: (String) -> Unit,
    onInvestigate: (String) -> Unit,
    onDismissFlower: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(GardenFilter.ALL) }
    val filtered = state.flowers.filter { flower ->
        when (filter) {
            GardenFilter.ALL -> true
            GardenFilter.COLLECTED -> flower.discoveryState == FlowerDiscoveryState.COLLECTED
            GardenFilter.RESEARCHED -> flower.discoveryState == FlowerDiscoveryState.REVEALED ||
                flower.discoveryState == FlowerDiscoveryState.COLLECTED
            GardenFilter.UNKNOWN -> flower.discoveryState == FlowerDiscoveryState.HIDDEN
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            PinhoQuestBackgrounds.HOME_NIGHT,
            overlay = Color(0xFF061B36),
            overlayAlpha = 0.08f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinhoBackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("🌷 Jardim", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Descubra, conquiste e veja seu jardim florescer.",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            PinhoParchment(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GardenFilter.entries.forEach { option ->
                        GardenFilterChip(
                            label = option.label,
                            selected = filter == option,
                            onClick = { filter = option },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (state.flowers.isEmpty()) {
                    EmptyGardenState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else if (filtered.isEmpty()) {
                    EmptyGardenState(
                        message = "Nada por aqui ainda.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 112.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { flower ->
                            GardenFlowerCard(
                                flower = flower,
                                onClick = { onFlowerSelected(flower.id) },
                            )
                        }
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
}

private enum class GardenFilter(val label: String) {
    ALL("Todas"),
    COLLECTED("Coletadas"),
    RESEARCHED("Pesquisadas"),
    UNKNOWN("Desconhecidas"),
}

@Composable
private fun GardenFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) PinhoForest else PinhoCreamSoft,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else PinhoInk,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun EmptyGardenState(
    message: String = "Ainda há espaço para crescer.",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(170.dp)
                .background(
                    Color(0xFFEED9B9),
                    RoundedCornerShape(22.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("🌱", fontSize = 72.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            message,
            color = PinhoInk,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Complete uma aventura e talvez uma nova flor encontre seu jardim.",
            color = PinhoInk,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun GardenFlowerCard(
    flower: GardenFlowerUi,
    onClick: () -> Unit,
) {
    val icon = when (flower.discoveryState) {
        FlowerDiscoveryState.HIDDEN -> "?"
        FlowerDiscoveryState.HINTED -> "🌿"
        FlowerDiscoveryState.REVEALED -> "🌸"
        FlowerDiscoveryState.COLLECTED -> "🌷"
    }
    PinhoParchment(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { role = Role.Button }
            .clickable(onClickLabel = "Flor \${flower.commonName}", onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 46.sp)
        }
        Spacer(Modifier.height(4.dp))
        when (flower.discoveryState) {
            FlowerDiscoveryState.HIDDEN,
            FlowerDiscoveryState.HINTED,
            -> {
                Text(
                    if (flower.discoveryState == FlowerDiscoveryState.HIDDEN) "???" else "Uma pista",
                    color = PinhoInk,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (flower.discoveryState == FlowerDiscoveryState.HIDDEN) "Desconhecida" else "Pesquisada",
                    color = PinhoInk,
                    fontSize = 12.sp,
                )
            }
            FlowerDiscoveryState.REVEALED,
            FlowerDiscoveryState.COLLECTED,
            -> {
                Text(
                    flower.commonName,
                    color = PinhoInk,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    flower.rarity.label,
                    color = PinhoInk,
                    fontSize = 12.sp,
                )
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
