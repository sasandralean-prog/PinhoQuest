package com.pinhoquest.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.R
import com.pinhoquest.ui.PinhoGraphicButton
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoCreamSoft
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.pinhoSelectedGlow
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
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(GardenFilter.ALL) }
    var showCollection by remember(state.collectedCount) {
        mutableStateOf(state.collectedCount == 0)
    }
    val isGardenEmpty = state.collectedCount == 0
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
            resource = when {
                isGardenEmpty && dark -> PinhoQuestBackgrounds.GARDEN_EMPTY_NIGHT
                isGardenEmpty -> PinhoQuestBackgrounds.GARDEN_EMPTY_DAY
                dark -> PinhoQuestBackgrounds.GARDEN_ART_NIGHT
                else -> PinhoQuestBackgrounds.GARDEN_ART_DAY
            },
            overlayAlpha = 0f,
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
                    val headerColor = if (isGardenEmpty) PinhoInk else Color.White
                    Text("🌷 Jardim de ${state.ownerName}", color = headerColor, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.lifetimeXp} XP · Nível ${state.level} · ${state.collectedCount} / ${state.totalCount} flores",
                        color = headerColor.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                    )
                    Text(
                        "Descubra, conquiste e veja seu jardim florescer.",
                        color = headerColor.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                isGardenEmpty -> {
                    EmptyGardenState(
                        dark = dark,
                        onViewQuests = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                !showCollection -> {
                    GardenArtState(
                        state = state,
                        dark = dark,
                        onOpenCollection = { showCollection = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }

                else -> {
                    PinhoParchment(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GardenFilter.entries.forEach { option ->
                                GardenFilterChip(
                                    label = option.label,
                                    resource = option.resource,
                                    aspectRatio = option.aspectRatio,
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Nenhuma flor corresponde a este filtro.",
                                    color = PinhoInk,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
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
                                        dark = dark,
                                        onClick = { onFlowerSelected(flower.id) },
                                    )
                                }
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

private enum class GardenFilter(
    val label: String,
    val resource: Int,
    val aspectRatio: Float,
) {
    ALL("Todas", R.drawable.btn_garden_filter_all, 107f / 46f),
    COLLECTED("Coletadas", R.drawable.btn_garden_filter_collected, 106f / 40f),
    RESEARCHED("Pesquisadas", R.drawable.btn_garden_filter_searched, 109f / 40f),
    UNKNOWN("Desconhecidas", R.drawable.btn_garden_filter_unkw, 119f / 40f),
}

@Composable
private fun GardenFilterChip(
    label: String,
    resource: Int,
    aspectRatio: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PinhoGraphicButton(
        resource = resource,
        contentDescription = label,
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        showSelectionState = true,
        aspectRatio = aspectRatio,
        label = label,
        labelColor = Color(0xFF4A2114),
        labelFontSize = 10.sp,
    )
}

@Composable
private fun EmptyGardenState(
    dark: Boolean,
    onViewQuests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(if (dark) 388f / 316f else 392f / 282f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.card_garden_empty_night else R.drawable.card_garden_empty_day,
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Ainda há espaço para crescer.",
            color = PinhoInk,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Complete uma aventura e talvez uma nova flor encontre seu jardim.",
            color = PinhoInk,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        PinhoGraphicButton(
            resource = R.drawable.btn_view_quests,
            contentDescription = "Ver quests",
            onClick = onViewQuests,
            modifier = Modifier.fillMaxWidth(0.62f),
            aspectRatio = 258f / 87f,
            label = "Ver quests",
            labelColor = Color(0xFF4A2114),
            labelFontSize = 17.sp,
            labelStartFraction = 0.14f,
        )
    }
}

@Composable
private fun GardenArtState(
    state: GardenUiState,
    dark: Boolean,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Seu jardim está florescendo!",
            color = if (dark) PinhoCream else PinhoForest,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.collectedCount} flores coletadas · ${state.lifetimeXp} XP",
            color = if (dark) PinhoCream else PinhoInk,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Abrir coleção do jardim",
                    onClick = onOpenCollection,
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.btn_garden_collection),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(54.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Abrir coleção",
                color = if (dark) PinhoCream else PinhoForest,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun GardenFlowerCard(
    flower: GardenFlowerUi,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val isUnknown = flower.discoveryState == FlowerDiscoveryState.HIDDEN
    val icon = when (flower.discoveryState) {
        FlowerDiscoveryState.HIDDEN -> "?"
        FlowerDiscoveryState.HINTED -> "🌿"
        FlowerDiscoveryState.REVEALED -> "🌸"
        FlowerDiscoveryState.COLLECTED -> "🌷"
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .height(160.dp)
        .then(
            if (isUnknown) Modifier
            else Modifier
                .background(PinhoCream.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
                .border(2.dp, Color(0xFFB99162), RoundedCornerShape(22.dp)),
        )
        .semantics { role = Role.Button }
        .clickable(onClickLabel = "Flor ${flower.commonName}", onClick = onClick)
        .testTag("flower-card-${flower.id}")

    Box(modifier = cardModifier) {
        if (isUnknown) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.card_flower_unknown_night else R.drawable.card_flower_unknown_day,
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(icon, fontSize = 42.sp)
            Spacer(Modifier.height(2.dp))
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
}

internal val FlowerRarity.label: String
    get() = when (this) {
        FlowerRarity.COMMON -> "Comum"
        FlowerRarity.UNCOMMON -> "Incomum"
        FlowerRarity.RARE -> "Rara"
        FlowerRarity.RAREST -> "Raríssima"
        FlowerRarity.UNKNOWN -> "???"
    }
