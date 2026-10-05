package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_catalog_snapshots")
data class GameCatalogSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val cycleId: String,
    val researchedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
)

@Entity(tableName = "game_discoveries")
data class GameDiscoveryEntity(
    @PrimaryKey val identityKey: String,
    val canonicalName: String,
    val platformsJson: String,
    val genresJson: String,
    val availability: String,
    val provenanceJson: String,
)

@Entity(tableName = "game_quest_usages")
data class GameQuestUsageEntity(
    @PrimaryKey val usageId: String,
    val cycleId: String,
    val gameIdentityKey: String,
    val variantFingerprint: String,
    val usedAtEpochMillis: Long,
)
