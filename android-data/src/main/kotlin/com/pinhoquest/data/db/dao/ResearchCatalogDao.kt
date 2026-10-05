package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinhoquest.data.db.entity.GameCatalogSnapshotEntity
import com.pinhoquest.data.db.entity.GameDiscoveryEntity
import com.pinhoquest.data.db.entity.GameQuestUsageEntity

@Dao
interface ResearchCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(entity: GameCatalogSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiscoveries(entities: List<GameDiscoveryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(entity: GameQuestUsageEntity)

    @Query("SELECT * FROM game_catalog_snapshots WHERE snapshotId = :snapshotId LIMIT 1")
    suspend fun snapshot(snapshotId: String): GameCatalogSnapshotEntity?

    @Query("SELECT * FROM game_discoveries ORDER BY identityKey")
    suspend fun discoveries(): List<GameDiscoveryEntity>

    @Query("SELECT gameIdentityKey FROM game_quest_usages WHERE cycleId = :cycleId")
    suspend fun usedGameIdentityKeys(cycleId: String): List<String>

    @Query("DELETE FROM game_quest_usages WHERE cycleId = :cycleId")
    suspend fun clearUsageCycle(cycleId: String)

    @Query("DELETE FROM game_discoveries")
    suspend fun clearDiscoveries()

    @Query("DELETE FROM game_catalog_snapshots")
    suspend fun clearSnapshots()
}
