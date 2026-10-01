package com.pinhoquest.domain.garden

@JvmInline value class FlowerId(val value: String)

enum class FlowerRarity {
    COMMON,
    UNCOMMON,
    RARE,
    RAREST,
    UNKNOWN,
}

data class FlowerDefinition(
    val id: FlowerId,
    val commonName: String,
    val scientificName: String,
    val description: String,
    val rarity: FlowerRarity,
    val rarityScaleVersion: Int?,
    val populationEstimate: GlobalPopulationEstimate?,
)
