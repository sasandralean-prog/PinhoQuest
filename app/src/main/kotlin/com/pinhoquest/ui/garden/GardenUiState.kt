package com.pinhoquest.ui.garden

import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity

data class GardenFlowerUi(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val description: String,
    val rarity: FlowerRarity,
    val discoveryState: FlowerDiscoveryState,
    val xpAward: Int? = null,
    val acquiredAtEpochMillis: Long? = null,
    val questTitle: String? = null,
    val investigationCost: Int? = null,
)

data class GardenUiState(
    val ownerName: String,
    val lifetimeXp: Int,
    val spendableXp: Int,
    val level: Int,
    val collectedCount: Int,
    val totalCount: Int,
    val flowers: List<GardenFlowerUi>,
    val selectedFlowerId: String? = null,
) {
    val selectedFlower: GardenFlowerUi?
        get() = flowers.firstOrNull { it.id == selectedFlowerId }
}
