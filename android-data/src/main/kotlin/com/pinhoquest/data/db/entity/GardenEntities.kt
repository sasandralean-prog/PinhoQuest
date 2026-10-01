package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rarity_scales")
data class RarityScaleEntity(
    @PrimaryKey val scaleVersion: Int,
    val referenceMinPopulation: Long,
    val referenceMaxPopulation: Long,
    val countingBasis: String,
    val methodology: String,
)

@Entity(
    tableName = "catalog_packs",
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "collectionIndex"], unique = true),
    ],
)
data class CatalogPackEntity(
    @PrimaryKey val packId: String,
    val profileId: String,
    val collectionIndex: Int,
    val version: Int,
    val generatedAtEpochMillis: Long,
    val rarityScaleVersion: Int,
    val sourceUrisJson: String,
    val integrityHash: String,
)

@Entity(tableName = "flower_definitions")
data class FlowerDefinitionEntity(
    @PrimaryKey val flowerId: String,
    val commonName: String,
    val scientificName: String,
    val description: String,
    val rarity: String,
    val rarityScaleVersion: Int?,
    val estimatedIndividuals: Long?,
    val lowerBound: Long?,
    val upperBound: Long?,
    val estimateDateEpochMillis: Long?,
    val evidenceSourceUrisJson: String,
    val confidence: String?,
    val countingBasis: String?,
)

@Entity(
    tableName = "catalog_entries",
    primaryKeys = ["packId", "flowerId"],
    indices = [Index("packId"), Index("flowerId")],
)
data class CatalogEntryEntity(
    val packId: String,
    val flowerId: String,
    val slotIndex: Int,
    val eligible: Boolean,
)

@Entity(
    tableName = "flower_discovery",
    primaryKeys = ["profileId", "flowerId"],
    indices = [Index("profileId")],
)
data class FlowerDiscoveryEntity(
    val profileId: String,
    val flowerId: String,
    val state: String,
)

@Entity(
    tableName = "flower_acquisitions",
    primaryKeys = ["profileId", "flowerId"],
    indices = [
        Index("profileId"),
        Index(value = ["completionId"], unique = true),
    ],
)
data class FlowerAcquisitionEntity(
    val profileId: String,
    val flowerId: String,
    val completionId: String,
    val acquiredAtEpochMillis: Long,
    val xpAward: Int,
)

@Entity(
    tableName = "reward_opportunities",
    indices = [
        Index("profileId"),
        Index(value = ["completionId"], unique = true),
        Index("state"),
    ],
)
data class RewardOpportunityEntity(
    @PrimaryKey val opportunityId: String,
    val profileId: String,
    val completionId: String,
    val state: String,
    val resolvedFlowerId: String?,
    val createdAtEpochMillis: Long,
    val resolvedAtEpochMillis: Long?,
)
