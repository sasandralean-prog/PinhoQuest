package com.pinhoquest.data.backup

import androidx.room.withTransaction
import com.pinhoquest.core.backup.RestorePlanner
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
import com.pinhoquest.data.settings.AppPreferences
import com.pinhoquest.data.settings.AppPreferencesStore
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.backup.BackupSnapshot
import com.pinhoquest.domain.backup.BackupReadResult
import com.pinhoquest.domain.backup.RestorePlanResult
import kotlinx.coroutines.flow.first

sealed interface RoomRestoreResult {
    data object Restored : RoomRestoreResult
    data class Invalid(val reason: String) : RoomRestoreResult
    data class Failed(val reason: String) : RoomRestoreResult
}

class RoomRestoreService(
    private val database: PinhoQuestDatabase,
    private val preferencesStore: AppPreferencesStore,
    private val planner: RestorePlanner = RestorePlanner(),
) {
    suspend fun restore(readResult: BackupReadResult): RoomRestoreResult =
        when (readResult) {
            is BackupReadResult.InvalidBackup -> RoomRestoreResult.Invalid(readResult.reason)
            is BackupReadResult.Valid -> restore(readResult.snapshot)
        }

    suspend fun restore(snapshot: BackupSnapshot): RoomRestoreResult {
        val plan = planner.plan(snapshot)
        if (plan is RestorePlanResult.Invalid) return RoomRestoreResult.Invalid(plan.reason)

        val targetPreferences = snapshot.preferences.toAppPreferences()
            ?: return RoomRestoreResult.Invalid("Invalid preferences")
        val previousPreferences = preferencesStore.preferences.first()

        return try {
            preferencesStore.write(targetPreferences)
            database.withTransaction {
                val dao = database.backupDao()
                dao.clearCompletionObjectives()
                dao.clearQuestCompletions()
                dao.clearGoalProgressApplications()
                dao.clearGoalProgress()
                dao.clearXpTransactions()
                dao.clearFlowerAcquisitions()
                dao.clearRewardOpportunities()
                dao.clearFlowerDiscoveries()
                dao.clearCatalogEntries()
                dao.clearCatalogPacks()
                dao.clearFlowerDefinitions()
                dao.clearRarityScales()
                dao.clearQuestSessions()
                dao.clearQuestObjectives()
                dao.clearQuests()
                dao.clearTags()
                dao.clearDatasetMetadata()
                dao.clearGameQuestUsages()
                dao.clearGameDiscoveries()
                dao.clearGameCatalogSnapshots()
                dao.clearGoalDefinitions()
                dao.clearProfiles()

                dao.insertProfilesIfAny(listOf(snapshot.profile.toEntity()))
                dao.insertTagsIfAny(snapshot.tags.map { it.toEntity() })
                dao.insertQuestsIfAny(snapshot.quests.map { it.toEntity() })
                dao.insertQuestObjectivesIfAny(snapshot.questObjectives.map { it.toEntity() })
                dao.insertQuestSessionsIfAny(snapshot.questSessions.map { it.toEntity() })
                dao.insertXpTransactionsIfAny(snapshot.xpTransactions.map { it.toEntity() })
                dao.insertGoalDefinitionsIfAny(snapshot.goalDefinitions.map { it.toEntity() })
                dao.insertGoalProgressIfAny(snapshot.goalProgress.map { it.toEntity() })
                dao.insertGoalProgressApplicationsIfAny(
                    snapshot.goalProgressApplications.map { it.toEntity() },
                )
                dao.insertQuestCompletionsIfAny(snapshot.questCompletions.map { it.toEntity() })
                dao.insertCompletionObjectivesIfAny(
                    snapshot.completionObjectives.map { it.toEntity() },
                )
                snapshot.datasetMetadata?.let { dao.insertDatasetMetadataIfAny(listOf(it.toEntity())) }
                dao.insertRarityScalesIfAny(snapshot.rarityScales.map { it.toEntity() })
                dao.insertCatalogPacksIfAny(snapshot.catalogPacks.map { it.toEntity() })
                dao.insertCatalogEntriesIfAny(snapshot.catalogEntries.map { it.toEntity() })
                dao.insertFlowerDefinitionsIfAny(snapshot.flowerDefinitions.map { it.toEntity() })
                dao.insertFlowerDiscoveriesIfAny(snapshot.flowerDiscoveries.map { it.toEntity() })
                dao.insertFlowerAcquisitionsIfAny(snapshot.flowerAcquisitions.map { it.toEntity() })
                dao.insertRewardOpportunitiesIfAny(
                    snapshot.rewardOpportunities.map { it.toEntity() },
                )
                dao.insertGameCatalogSnapshotsIfAny(
                    snapshot.gameCatalogSnapshots.map { it.toEntity() },
                )
                dao.insertGameDiscoveriesIfAny(snapshot.gameDiscoveries.map { it.toEntity() })
                dao.insertGameQuestUsagesIfAny(snapshot.gameQuestUsages.map { it.toEntity() })
            }
            RoomRestoreResult.Restored
        } catch (t: Throwable) {
            runCatching { preferencesStore.write(previousPreferences) }
            RoomRestoreResult.Failed(t.message ?: "Restore failed")
        }
    }

    private fun com.pinhoquest.domain.backup.BackupPreferences.toAppPreferences(): AppPreferences? {
        val theme = runCatching { ThemePreference.valueOf(theme) }.getOrNull() ?: return null
        if (!fontScale.isFinite() || fontScale <= 0f) return null
        return AppPreferences(theme = theme, fontScale = fontScale)
    }

    private fun com.pinhoquest.domain.backup.BackupProfile.toEntity() =
        ProfileEntity(profileId, gardenOwnerName, createdAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupTag.toEntity() =
        TagEntity(profileId, tagId, label, source, affinity, enabled)

    private fun com.pinhoquest.domain.backup.BackupQuest.toEntity() =
        QuestEntity(questId, title, description, category, environment, minMinutes, maxMinutes, difficulty, state)

    private fun com.pinhoquest.domain.backup.BackupQuestObjective.toEntity() =
        QuestObjectiveEntity(questId, objectiveId, text, optional)

    private fun com.pinhoquest.domain.backup.BackupQuestSession.toEntity() =
        QuestSessionEntity(sessionId, questId, state, createdAtEpochMillis, updatedAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupXpTransaction.toEntity() =
        XpTransactionEntity(transactionId, profileId, amount, type, completionId, flowerId, createdAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupGoalDefinition.toEntity() =
        GoalDefinitionEntity(goalId, title, ruleType, target, category, difficulty, version)

    private fun com.pinhoquest.domain.backup.BackupGoalProgress.toEntity() =
        GoalProgressEntity(profileId, goalId, currentValue, targetValue, completed)

    private fun com.pinhoquest.domain.backup.BackupGoalProgressApplication.toEntity() =
        GoalProgressApplicationEntity(profileId, goalId, completionId, appliedAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupQuestCompletion.toEntity() =
        QuestCompletionEntity(completionId, sessionId, questId, profileId, completedAtEpochMillis, xpAward)

    private fun com.pinhoquest.domain.backup.BackupCompletionObjective.toEntity() =
        CompletionObjectiveEntity(completionId, objectiveId)

    private fun com.pinhoquest.domain.backup.BackupDatasetMetadata.toEntity() =
        DatasetMetadataEntity(profileId, datasetRevision, schemaVersion, lastModifiedAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupRarityScale.toEntity() =
        RarityScaleEntity(scaleVersion, referenceMinPopulation, referenceMaxPopulation, countingBasis, methodology)

    private fun com.pinhoquest.domain.backup.BackupCatalogPack.toEntity() =
        CatalogPackEntity(
            packId,
            profileId,
            collectionIndex,
            version,
            generatedAtEpochMillis,
            rarityScaleVersion,
            sourceUrisJson,
            integrityHash,
        )

    private fun com.pinhoquest.domain.backup.BackupFlowerDefinition.toEntity() =
        FlowerDefinitionEntity(
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

    private fun com.pinhoquest.domain.backup.BackupCatalogEntry.toEntity() =
        CatalogEntryEntity(packId, flowerId, slotIndex, eligible)

    private fun com.pinhoquest.domain.backup.BackupFlowerDiscovery.toEntity() =
        FlowerDiscoveryEntity(profileId, flowerId, state)

    private fun com.pinhoquest.domain.backup.BackupFlowerAcquisition.toEntity() =
        FlowerAcquisitionEntity(profileId, flowerId, completionId, acquiredAtEpochMillis, xpAward)

    private fun com.pinhoquest.domain.backup.BackupRewardOpportunity.toEntity() =
        RewardOpportunityEntity(
            opportunityId,
            profileId,
            completionId,
            state,
            resolvedFlowerId,
            createdAtEpochMillis,
            resolvedAtEpochMillis,
        )

    private fun com.pinhoquest.domain.backup.BackupGameCatalogSnapshot.toEntity() =
        GameCatalogSnapshotEntity(snapshotId, cycleId, researchedAtEpochMillis, expiresAtEpochMillis)

    private fun com.pinhoquest.domain.backup.BackupGameDiscovery.toEntity() =
        GameDiscoveryEntity(identityKey, canonicalName, platformsJson, genresJson, availability, provenanceJson)

    private fun com.pinhoquest.domain.backup.BackupGameQuestUsage.toEntity() =
        GameQuestUsageEntity(usageId, cycleId, gameIdentityKey, variantFingerprint, usedAtEpochMillis)

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertProfilesIfAny(rows: List<ProfileEntity>) {
        if (rows.isNotEmpty()) insertProfiles(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertTagsIfAny(rows: List<TagEntity>) {
        if (rows.isNotEmpty()) insertTags(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertQuestsIfAny(rows: List<QuestEntity>) {
        if (rows.isNotEmpty()) insertQuests(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertQuestObjectivesIfAny(rows: List<QuestObjectiveEntity>) {
        if (rows.isNotEmpty()) insertQuestObjectives(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertQuestSessionsIfAny(rows: List<QuestSessionEntity>) {
        if (rows.isNotEmpty()) insertQuestSessions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertXpTransactionsIfAny(rows: List<XpTransactionEntity>) {
        if (rows.isNotEmpty()) insertXpTransactions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGoalDefinitionsIfAny(rows: List<GoalDefinitionEntity>) {
        if (rows.isNotEmpty()) insertGoalDefinitions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGoalProgressIfAny(rows: List<GoalProgressEntity>) {
        if (rows.isNotEmpty()) insertGoalProgress(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGoalProgressApplicationsIfAny(
        rows: List<GoalProgressApplicationEntity>,
    ) {
        if (rows.isNotEmpty()) insertGoalProgressApplications(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertQuestCompletionsIfAny(rows: List<QuestCompletionEntity>) {
        if (rows.isNotEmpty()) insertQuestCompletions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertCompletionObjectivesIfAny(
        rows: List<CompletionObjectiveEntity>,
    ) {
        if (rows.isNotEmpty()) insertCompletionObjectives(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertDatasetMetadataIfAny(
        rows: List<DatasetMetadataEntity>,
    ) {
        if (rows.isNotEmpty()) insertDatasetMetadata(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertRarityScalesIfAny(rows: List<RarityScaleEntity>) {
        if (rows.isNotEmpty()) insertRarityScales(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertCatalogPacksIfAny(rows: List<CatalogPackEntity>) {
        if (rows.isNotEmpty()) insertCatalogPacks(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertCatalogEntriesIfAny(rows: List<CatalogEntryEntity>) {
        if (rows.isNotEmpty()) insertCatalogEntries(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertFlowerDefinitionsIfAny(rows: List<FlowerDefinitionEntity>) {
        if (rows.isNotEmpty()) insertFlowerDefinitions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertFlowerDiscoveriesIfAny(rows: List<FlowerDiscoveryEntity>) {
        if (rows.isNotEmpty()) insertFlowerDiscoveries(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertFlowerAcquisitionsIfAny(
        rows: List<FlowerAcquisitionEntity>,
    ) {
        if (rows.isNotEmpty()) insertFlowerAcquisitions(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertRewardOpportunitiesIfAny(
        rows: List<RewardOpportunityEntity>,
    ) {
        if (rows.isNotEmpty()) insertRewardOpportunities(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGameCatalogSnapshotsIfAny(
        rows: List<GameCatalogSnapshotEntity>,
    ) {
        if (rows.isNotEmpty()) insertGameCatalogSnapshots(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGameDiscoveriesIfAny(rows: List<GameDiscoveryEntity>) {
        if (rows.isNotEmpty()) insertGameDiscoveries(rows)
    }

    private suspend fun com.pinhoquest.data.db.dao.BackupDao.insertGameQuestUsagesIfAny(rows: List<GameQuestUsageEntity>) {
        if (rows.isNotEmpty()) insertGameQuestUsages(rows)
    }
}

