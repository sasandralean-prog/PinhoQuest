package com.pinhoquest.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pinhoquest.data.db.dao.GardenDao
import com.pinhoquest.data.db.dao.ProfileDao
import com.pinhoquest.data.db.dao.ProgressionDao
import com.pinhoquest.data.db.dao.QuestDao
import com.pinhoquest.data.db.dao.QuestSessionDao
import com.pinhoquest.data.db.dao.ResearchCatalogDao
import com.pinhoquest.data.db.dao.TagDao
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

@Database(
    entities = [
        ProfileEntity::class,
        TagEntity::class,
        QuestEntity::class,
        QuestObjectiveEntity::class,
        QuestSessionEntity::class,
        XpTransactionEntity::class,
        GoalDefinitionEntity::class,
        GoalProgressEntity::class,
        GoalProgressApplicationEntity::class,
        QuestCompletionEntity::class,
        CompletionObjectiveEntity::class,
        DatasetMetadataEntity::class,
        RarityScaleEntity::class,
        CatalogPackEntity::class,
        FlowerDefinitionEntity::class,
        CatalogEntryEntity::class,
        FlowerDiscoveryEntity::class,
        FlowerAcquisitionEntity::class,
        RewardOpportunityEntity::class,
        GameCatalogSnapshotEntity::class,
        GameDiscoveryEntity::class,
        GameQuestUsageEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class PinhoQuestDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun tagDao(): TagDao
    abstract fun questDao(): QuestDao
    abstract fun questSessionDao(): QuestSessionDao
    abstract fun progressionDao(): ProgressionDao
    abstract fun researchCatalogDao(): ResearchCatalogDao
    abstract fun gardenDao(): GardenDao
}
