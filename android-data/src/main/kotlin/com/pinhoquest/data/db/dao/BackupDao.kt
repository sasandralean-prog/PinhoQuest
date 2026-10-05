package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinhoquest.data.db.entity.CatalogEntryEntity
import com.pinhoquest.data.db.entity.CatalogPackEntity
import com.pinhoquest.data.db.entity.CompletionObjectiveEntity
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.FlowerAcquisitionEntity
import com.pinhoquest.data.db.entity.FlowerDefinitionEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.db.entity.GameCatalogSnapshotEntity
import com.pinhoquest.data.db.entity.GameDiscoveryEntity
import com.pinhoquest.data.db.entity.GameQuestUsageEntity
import com.pinhoquest.data.db.entity.GoalDefinitionEntity
import com.pinhoquest.data.db.entity.GoalProgressApplicationEntity
import com.pinhoquest.data.db.entity.GoalProgressEntity
import com.pinhoquest.data.db.entity.ProfileEntity
import com.pinhoquest.data.db.entity.QuestCompletionEntity
import com.pinhoquest.data.db.entity.QuestEntity
import com.pinhoquest.data.db.entity.QuestObjectiveEntity
import com.pinhoquest.data.db.entity.QuestSessionEntity
import com.pinhoquest.data.db.entity.RarityScaleEntity
import com.pinhoquest.data.db.entity.RewardOpportunityEntity
import com.pinhoquest.data.db.entity.TagEntity
import com.pinhoquest.data.db.entity.XpTransactionEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM profiles ORDER BY profileId")
    suspend fun profiles(): List<ProfileEntity>

    @Query("SELECT * FROM tags ORDER BY profileId, tagId")
    suspend fun tags(): List<TagEntity>

    @Query("SELECT * FROM quests ORDER BY questId")
    suspend fun quests(): List<QuestEntity>

    @Query("SELECT * FROM quest_objectives ORDER BY questId, objectiveId")
    suspend fun questObjectives(): List<QuestObjectiveEntity>

    @Query("SELECT * FROM quest_sessions ORDER BY sessionId")
    suspend fun questSessions(): List<QuestSessionEntity>

    @Query("SELECT * FROM xp_transactions ORDER BY profileId, createdAtEpochMillis, transactionId")
    suspend fun xpTransactions(): List<XpTransactionEntity>

    @Query("SELECT * FROM goal_definitions ORDER BY goalId")
    suspend fun goalDefinitions(): List<GoalDefinitionEntity>

    @Query("SELECT * FROM goal_progress ORDER BY profileId, goalId")
    suspend fun goalProgress(): List<GoalProgressEntity>

    @Query("SELECT * FROM goal_progress_applications ORDER BY profileId, goalId, completionId")
    suspend fun goalProgressApplications(): List<GoalProgressApplicationEntity>

    @Query("SELECT * FROM quest_completions ORDER BY profileId, completedAtEpochMillis, completionId")
    suspend fun questCompletions(): List<QuestCompletionEntity>

    @Query("SELECT * FROM completion_objectives ORDER BY completionId, objectiveId")
    suspend fun completionObjectives(): List<CompletionObjectiveEntity>

    @Query("SELECT * FROM dataset_metadata ORDER BY profileId")
    suspend fun datasetMetadata(): List<DatasetMetadataEntity>

    @Query("SELECT * FROM rarity_scales ORDER BY scaleVersion")
    suspend fun rarityScales(): List<RarityScaleEntity>

    @Query("SELECT * FROM catalog_packs ORDER BY profileId, collectionIndex")
    suspend fun catalogPacks(): List<CatalogPackEntity>

    @Query("SELECT * FROM catalog_entries ORDER BY packId, slotIndex, flowerId")
    suspend fun catalogEntries(): List<CatalogEntryEntity>

    @Query("SELECT * FROM flower_definitions ORDER BY flowerId")
    suspend fun flowerDefinitions(): List<FlowerDefinitionEntity>

    @Query("SELECT * FROM flower_discovery ORDER BY profileId, flowerId")
    suspend fun flowerDiscoveries(): List<FlowerDiscoveryEntity>

    @Query("SELECT * FROM flower_acquisitions ORDER BY profileId, acquiredAtEpochMillis, flowerId")
    suspend fun flowerAcquisitions(): List<FlowerAcquisitionEntity>

    @Query("SELECT * FROM reward_opportunities ORDER BY profileId, createdAtEpochMillis, opportunityId")
    suspend fun rewardOpportunities(): List<RewardOpportunityEntity>

    @Query("SELECT * FROM game_catalog_snapshots ORDER BY snapshotId")
    suspend fun gameCatalogSnapshots(): List<GameCatalogSnapshotEntity>

    @Query("SELECT * FROM game_discoveries ORDER BY identityKey")
    suspend fun gameDiscoveries(): List<GameDiscoveryEntity>

    @Query("SELECT * FROM game_quest_usages ORDER BY cycleId, usedAtEpochMillis, usageId")
    suspend fun gameQuestUsages(): List<GameQuestUsageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfiles(rows: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTags(rows: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuests(rows: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestObjectives(rows: List<QuestObjectiveEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestSessions(rows: List<QuestSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertXpTransactions(rows: List<XpTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalDefinitions(rows: List<GoalDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalProgress(rows: List<GoalProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoalProgressApplications(rows: List<GoalProgressApplicationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuestCompletions(rows: List<QuestCompletionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompletionObjectives(rows: List<CompletionObjectiveEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDatasetMetadata(rows: List<DatasetMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRarityScales(rows: List<RarityScaleEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCatalogPacks(rows: List<CatalogPackEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCatalogEntries(rows: List<CatalogEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFlowerDefinitions(rows: List<FlowerDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFlowerDiscoveries(rows: List<FlowerDiscoveryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFlowerAcquisitions(rows: List<FlowerAcquisitionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRewardOpportunities(rows: List<RewardOpportunityEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGameCatalogSnapshots(rows: List<GameCatalogSnapshotEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGameDiscoveries(rows: List<GameDiscoveryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGameQuestUsages(rows: List<GameQuestUsageEntity>)

    @Query("DELETE FROM completion_objectives")
    suspend fun clearCompletionObjectives()

    @Query("DELETE FROM quest_completions")
    suspend fun clearQuestCompletions()

    @Query("DELETE FROM goal_progress_applications")
    suspend fun clearGoalProgressApplications()

    @Query("DELETE FROM goal_progress")
    suspend fun clearGoalProgress()

    @Query("DELETE FROM xp_transactions")
    suspend fun clearXpTransactions()

    @Query("DELETE FROM flower_acquisitions")
    suspend fun clearFlowerAcquisitions()

    @Query("DELETE FROM reward_opportunities")
    suspend fun clearRewardOpportunities()

    @Query("DELETE FROM flower_discovery")
    suspend fun clearFlowerDiscoveries()

    @Query("DELETE FROM catalog_entries")
    suspend fun clearCatalogEntries()

    @Query("DELETE FROM catalog_packs")
    suspend fun clearCatalogPacks()

    @Query("DELETE FROM flower_definitions")
    suspend fun clearFlowerDefinitions()

    @Query("DELETE FROM rarity_scales")
    suspend fun clearRarityScales()

    @Query("DELETE FROM quest_sessions")
    suspend fun clearQuestSessions()

    @Query("DELETE FROM quest_objectives")
    suspend fun clearQuestObjectives()

    @Query("DELETE FROM quests")
    suspend fun clearQuests()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM dataset_metadata")
    suspend fun clearDatasetMetadata()

    @Query("DELETE FROM game_quest_usages")
    suspend fun clearGameQuestUsages()

    @Query("DELETE FROM game_discoveries")
    suspend fun clearGameDiscoveries()

    @Query("DELETE FROM game_catalog_snapshots")
    suspend fun clearGameCatalogSnapshots()

    @Query("DELETE FROM goal_definitions")
    suspend fun clearGoalDefinitions()

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}
