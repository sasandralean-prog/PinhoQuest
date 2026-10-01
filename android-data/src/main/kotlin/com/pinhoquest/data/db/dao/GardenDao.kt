package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pinhoquest.data.db.entity.CatalogEntryEntity
import com.pinhoquest.data.db.entity.CatalogPackEntity
import com.pinhoquest.data.db.entity.FlowerAcquisitionEntity
import com.pinhoquest.data.db.entity.FlowerDefinitionEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.db.entity.RarityScaleEntity
import com.pinhoquest.data.db.entity.RewardOpportunityEntity

@Dao
interface GardenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRarityScale(entity: RarityScaleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCatalogPack(entity: CatalogPackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlowerDefinitions(entities: List<FlowerDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogEntries(entities: List<CatalogEntryEntity>)

    @Query("SELECT * FROM catalog_packs WHERE profileId = :profileId ORDER BY collectionIndex")
    suspend fun catalogPacks(profileId: String): List<CatalogPackEntity>

    @Query("SELECT * FROM catalog_packs WHERE profileId = :profileId ORDER BY collectionIndex DESC LIMIT 1")
    suspend fun latestCatalogPack(profileId: String): CatalogPackEntity?

    @Query("SELECT * FROM catalog_entries WHERE packId = :packId ORDER BY slotIndex")
    suspend fun catalogEntries(packId: String): List<CatalogEntryEntity>

    @Query("SELECT * FROM flower_definitions WHERE flowerId IN (:flowerIds)")
    suspend fun flowerDefinitions(flowerIds: List<String>): List<FlowerDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiscovery(entity: FlowerDiscoveryEntity)

    @Query("SELECT * FROM flower_discovery WHERE profileId = :profileId AND flowerId = :flowerId LIMIT 1")
    suspend fun discovery(profileId: String, flowerId: String): FlowerDiscoveryEntity?

    @Query("SELECT * FROM flower_discovery WHERE profileId = :profileId ORDER BY flowerId")
    suspend fun discoveries(profileId: String): List<FlowerDiscoveryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAcquisition(entity: FlowerAcquisitionEntity)

    @Query("SELECT * FROM flower_acquisitions WHERE profileId = :profileId ORDER BY acquiredAtEpochMillis, flowerId")
    suspend fun acquisitions(profileId: String): List<FlowerAcquisitionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRewardOpportunity(entity: RewardOpportunityEntity)

    @Update
    suspend fun updateRewardOpportunity(entity: RewardOpportunityEntity)

    @Query("SELECT * FROM reward_opportunities WHERE completionId = :completionId LIMIT 1")
    suspend fun rewardOpportunityByCompletion(completionId: String): RewardOpportunityEntity?

    @Query("SELECT * FROM reward_opportunities WHERE opportunityId = :opportunityId LIMIT 1")
    suspend fun rewardOpportunity(opportunityId: String): RewardOpportunityEntity?

    @Query("SELECT * FROM reward_opportunities WHERE profileId = :profileId AND state = :state ORDER BY createdAtEpochMillis")
    suspend fun rewardOpportunitiesByState(profileId: String, state: String): List<RewardOpportunityEntity>
}
