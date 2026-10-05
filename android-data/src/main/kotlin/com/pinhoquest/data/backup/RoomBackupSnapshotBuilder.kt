package com.pinhoquest.data.backup

import androidx.room.withTransaction
import com.pinhoquest.data.db.PinhoQuestDatabase
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
import com.pinhoquest.data.settings.AppPreferencesStore
import com.pinhoquest.domain.backup.BackupCatalogEntry
import com.pinhoquest.domain.backup.BackupCatalogPack
import com.pinhoquest.domain.backup.BackupCompletionObjective
import com.pinhoquest.domain.backup.BackupDatasetMetadata
import com.pinhoquest.domain.backup.BackupFlowerAcquisition
import com.pinhoquest.domain.backup.BackupFlowerDefinition
import com.pinhoquest.domain.backup.BackupFlowerDiscovery
import com.pinhoquest.domain.backup.BackupGameCatalogSnapshot
import com.pinhoquest.domain.backup.BackupGameDiscovery
import com.pinhoquest.domain.backup.BackupGameQuestUsage
import com.pinhoquest.domain.backup.BackupGoalDefinition
import com.pinhoquest.domain.backup.BackupGoalProgress
import com.pinhoquest.domain.backup.BackupGoalProgressApplication
import com.pinhoquest.domain.backup.BackupManifest
import com.pinhoquest.domain.backup.BackupPreferences
import com.pinhoquest.domain.backup.BackupProfile
import com.pinhoquest.domain.backup.BackupQuest
import com.pinhoquest.domain.backup.BackupQuestCompletion
import com.pinhoquest.domain.backup.BackupQuestObjective
import com.pinhoquest.domain.backup.BackupQuestSession
import com.pinhoquest.domain.backup.BackupRarityScale
import com.pinhoquest.domain.backup.BackupRewardOpportunity
import com.pinhoquest.domain.backup.BackupSnapshot
import com.pinhoquest.domain.backup.BackupSnapshotBuildResult
import com.pinhoquest.domain.backup.BackupTag
import com.pinhoquest.domain.backup.BackupXpTransaction
import java.time.Clock
import kotlinx.coroutines.flow.first

class RoomBackupSnapshotBuilder(
    private val database: PinhoQuestDatabase,
    private val preferencesStore: AppPreferencesStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun build(appVersion: String): BackupSnapshotBuildResult =
        database.withTransaction {
            if (appVersion.isBlank()) {
                return@withTransaction BackupSnapshotBuildResult.InvalidState("App version is blank")
            }

            val dao = database.backupDao()
            val profiles = dao.profiles()
            if (profiles.isEmpty()) return@withTransaction BackupSnapshotBuildResult.NoProfile
            if (profiles.size != 1) return@withTransaction BackupSnapshotBuildResult.MultipleProfiles

            val profile = profiles.single()
            val profileId = profile.profileId

            val tags = dao.tags().also { requireOnlyProfile("tags", it.map(TagEntity::profileId), profileId) }
            val xpTransactions = dao.xpTransactions().also {
                requireOnlyProfile("xp_transactions", it.map(XpTransactionEntity::profileId), profileId)
            }
            val goalProgress = dao.goalProgress().also {
                requireOnlyProfile("goal_progress", it.map(GoalProgressEntity::profileId), profileId)
            }
            val goalApplications = dao.goalProgressApplications().also {
                requireOnlyProfile(
                    "goal_progress_applications",
                    it.map(GoalProgressApplicationEntity::profileId),
                    profileId,
                )
            }
            val completions = dao.questCompletions().also {
                requireOnlyProfile("quest_completions", it.map(QuestCompletionEntity::profileId), profileId)
            }
            val datasetMetadata = dao.datasetMetadata().also {
                requireOnlyProfile("dataset_metadata", it.map(DatasetMetadataEntity::profileId), profileId)
            }
            val catalogPacks = dao.catalogPacks().also {
                requireOnlyProfile("catalog_packs", it.map(CatalogPackEntity::profileId), profileId)
            }
            val discoveries = dao.flowerDiscoveries().also {
                requireOnlyProfile("flower_discovery", it.map(FlowerDiscoveryEntity::profileId), profileId)
            }
            val acquisitions = dao.flowerAcquisitions().also {
                requireOnlyProfile("flower_acquisitions", it.map(FlowerAcquisitionEntity::profileId), profileId)
            }
            val rewardOpportunities = dao.rewardOpportunities().also {
                requireOnlyProfile("reward_opportunities", it.map(RewardOpportunityEntity::profileId), profileId)
            }

            val completionIds = completions.map { it.completionId }.toSet()
            val completionObjectives = dao.completionObjectives()
                .filter { it.completionId in completionIds }

            val packIds = catalogPacks.map { it.packId }.toSet()
            val catalogEntries = dao.catalogEntries()
                .filter { it.packId in packIds }

            val metadata = datasetMetadata.singleOrNull()
            val latestFlowerCatalogVersion = catalogPacks.maxOfOrNull { it.version } ?: 0
            val createdAt = clock.millis()
            val preferences = preferencesStore.preferences.first()

            BackupSnapshotBuildResult.Ready(
                BackupSnapshot(
                    manifest = BackupManifest(
                        formatVersion = FORMAT_VERSION,
                        schemaVersion = ROOM_SCHEMA_VERSION,
                        profileId = profileId,
                        datasetRevision = metadata?.datasetRevision ?: 0L,
                        createdAt = createdAt,
                        appVersion = appVersion,
                        catalogVersions = buildMap {
                            if (latestFlowerCatalogVersion > 0) {
                                put("flowerCatalog", latestFlowerCatalogVersion)
                            }
                        },
                        integrityHash = "",
                    ),
                    profile = profile.toBackup(),
                    tags = tags.map { it.toBackup() },
                    quests = dao.quests().map { it.toBackup() },
                    questObjectives = dao.questObjectives().map { it.toBackup() },
                    questSessions = dao.questSessions().map { it.toBackup() },
                    xpTransactions = xpTransactions.map { it.toBackup() },
                    goalDefinitions = dao.goalDefinitions().map { it.toBackup() },
                    goalProgress = goalProgress.map { it.toBackup() },
                    goalProgressApplications = goalApplications.map { it.toBackup() },
                    questCompletions = completions.map { it.toBackup() },
                    completionObjectives = completionObjectives.map { it.toBackup() },
                    datasetMetadata = metadata?.toBackup(),
                    rarityScales = dao.rarityScales().map { it.toBackup() },
                    catalogPacks = catalogPacks.map { it.toBackup() },
                    flowerDefinitions = dao.flowerDefinitions().map { it.toBackup() },
                    catalogEntries = catalogEntries.map { it.toBackup() },
                    flowerDiscoveries = discoveries.map { it.toBackup() },
                    flowerAcquisitions = acquisitions.map { it.toBackup() },
                    rewardOpportunities = rewardOpportunities.map { it.toBackup() },
                    gameCatalogSnapshots = dao.gameCatalogSnapshots().map { it.toBackup() },
                    gameDiscoveries = dao.gameDiscoveries().map { it.toBackup() },
                    gameQuestUsages = dao.gameQuestUsages().map { it.toBackup() },
                    preferences = BackupPreferences(
                        theme = preferences.theme.name,
                        fontScale = preferences.fontScale,
                    ),
                ),
            )
        }

    private fun requireOnlyProfile(table: String, values: List<String>, profileId: String) {
        if (values.any { it != profileId }) {
            error("$table contains data for another profile")
        }
    }

    private fun ProfileEntity.toBackup() =
        BackupProfile(profileId, gardenOwnerName, createdAtEpochMillis)

    private fun TagEntity.toBackup() =
        BackupTag(profileId, tagId, label, source, affinity, enabled)

    private fun QuestEntity.toBackup() =
        BackupQuest(questId, title, description, category, environment, minMinutes, maxMinutes, difficulty, state)

    private fun QuestObjectiveEntity.toBackup() =
        BackupQuestObjective(questId, objectiveId, text, optional)

    private fun QuestSessionEntity.toBackup() =
        BackupQuestSession(sessionId, questId, state, createdAtEpochMillis, updatedAtEpochMillis)

    private fun XpTransactionEntity.toBackup() =
        BackupXpTransaction(transactionId, profileId, amount, type, completionId, flowerId, createdAtEpochMillis)

    private fun GoalDefinitionEntity.toBackup() =
        BackupGoalDefinition(goalId, title, ruleType, target, category, difficulty, version)

    private fun GoalProgressEntity.toBackup() =
        BackupGoalProgress(profileId, goalId, currentValue, targetValue, completed)

    private fun GoalProgressApplicationEntity.toBackup() =
        BackupGoalProgressApplication(profileId, goalId, completionId, appliedAtEpochMillis)

    private fun QuestCompletionEntity.toBackup() =
        BackupQuestCompletion(
            completionId,
            sessionId,
            questId,
            profileId,
            completedAtEpochMillis,
            xpAward,
        )

    private fun CompletionObjectiveEntity.toBackup() =
        BackupCompletionObjective(completionId, objectiveId)

    private fun DatasetMetadataEntity.toBackup() =
        BackupDatasetMetadata(profileId, datasetRevision, schemaVersion, lastModifiedAtEpochMillis)

    private fun RarityScaleEntity.toBackup() =
        BackupRarityScale(
            scaleVersion,
            referenceMinPopulation,
            referenceMaxPopulation,
            countingBasis,
            methodology,
        )

    private fun CatalogPackEntity.toBackup() =
        BackupCatalogPack(
            packId,
            profileId,
            collectionIndex,
            version,
            generatedAtEpochMillis,
            rarityScaleVersion,
            sourceUrisJson,
            integrityHash,
        )

    private fun FlowerDefinitionEntity.toBackup() =
        BackupFlowerDefinition(
            flowerId,
            commonName,
            scientificName,
            description,
            rarity,
            rarityScaleVersion,
            estimatedIndividuals,
            lowerBound,
            upperBound,
            estimateDateEpochMillis,
            evidenceSourceUrisJson,
            confidence,
            countingBasis,
        )

    private fun CatalogEntryEntity.toBackup() =
        BackupCatalogEntry(packId, flowerId, slotIndex, eligible)

    private fun FlowerDiscoveryEntity.toBackup() =
        BackupFlowerDiscovery(profileId, flowerId, state)

    private fun FlowerAcquisitionEntity.toBackup() =
        BackupFlowerAcquisition(
            profileId,
            flowerId,
            completionId,
            acquiredAtEpochMillis,
            xpAward,
        )

    private fun RewardOpportunityEntity.toBackup() =
        BackupRewardOpportunity(
            opportunityId,
            profileId,
            completionId,
            state,
            resolvedFlowerId,
            createdAtEpochMillis,
            resolvedAtEpochMillis,
        )

    private fun GameCatalogSnapshotEntity.toBackup() =
        BackupGameCatalogSnapshot(snapshotId, cycleId, researchedAtEpochMillis, expiresAtEpochMillis)

    private fun GameDiscoveryEntity.toBackup() =
        BackupGameDiscovery(
            identityKey,
            canonicalName,
            platformsJson,
            genresJson,
            availability,
            provenanceJson,
        )

    private fun GameQuestUsageEntity.toBackup() =
        BackupGameQuestUsage(usageId, cycleId, gameIdentityKey, variantFingerprint, usedAtEpochMillis)

    private companion object {
        const val FORMAT_VERSION = 1
        const val ROOM_SCHEMA_VERSION = 4
    }
}

