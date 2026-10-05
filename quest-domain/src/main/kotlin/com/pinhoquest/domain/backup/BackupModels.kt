package com.pinhoquest.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(val formatVersion: Int, val schemaVersion: Int, val profileId: String, val datasetRevision: Long, val createdAt: Long, val appVersion: String, val catalogVersions: Map<String, Int>, val integrityHash: String)

@Serializable
data class BackupSnapshot(
    val manifest: BackupManifest,
    val profile: BackupProfile,
    val tags: List<BackupTag> = emptyList(),
    val quests: List<BackupQuest> = emptyList(),
    val questObjectives: List<BackupQuestObjective> = emptyList(),
    val questSessions: List<BackupQuestSession> = emptyList(),
    val xpTransactions: List<BackupXpTransaction> = emptyList(),
    val goalDefinitions: List<BackupGoalDefinition> = emptyList(),
    val goalProgress: List<BackupGoalProgress> = emptyList(),
    val goalProgressApplications: List<BackupGoalProgressApplication> = emptyList(),
    val questCompletions: List<BackupQuestCompletion> = emptyList(),
    val completionObjectives: List<BackupCompletionObjective> = emptyList(),
    val datasetMetadata: BackupDatasetMetadata? = null,
    val rarityScales: List<BackupRarityScale> = emptyList(),
    val catalogPacks: List<BackupCatalogPack> = emptyList(),
    val flowerDefinitions: List<BackupFlowerDefinition> = emptyList(),
    val catalogEntries: List<BackupCatalogEntry> = emptyList(),
    val flowerDiscoveries: List<BackupFlowerDiscovery> = emptyList(),
    val flowerAcquisitions: List<BackupFlowerAcquisition> = emptyList(),
    val rewardOpportunities: List<BackupRewardOpportunity> = emptyList(),
    val gameCatalogSnapshots: List<BackupGameCatalogSnapshot> = emptyList(),
    val gameDiscoveries: List<BackupGameDiscovery> = emptyList(),
    val gameQuestUsages: List<BackupGameQuestUsage> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences(),
)
@Serializable data class BackupProfile(val profileId: String, val gardenOwnerName: String, val createdAtEpochMillis: Long)
@Serializable data class BackupTag(val profileId: String, val tagId: String, val label: String, val source: String, val affinity: Double, val enabled: Boolean)
@Serializable data class BackupQuest(val questId: String, val title: String, val description: String, val category: String, val environment: String, val minMinutes: Int, val maxMinutes: Int, val difficulty: String, val state: String)
@Serializable data class BackupQuestObjective(val questId: String, val objectiveId: String, val text: String, val optional: Boolean)
@Serializable data class BackupQuestSession(val sessionId: String, val questId: String, val state: String, val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long)
@Serializable data class BackupXpTransaction(val transactionId: String, val profileId: String, val amount: Int, val type: String, val completionId: String?, val flowerId: String?, val createdAtEpochMillis: Long)
@Serializable data class BackupGoalDefinition(val goalId: String, val title: String, val ruleType: String, val target: Int, val category: String?, val difficulty: String?, val version: Int)
@Serializable data class BackupGoalProgress(val profileId: String, val goalId: String, val currentValue: Int, val targetValue: Int, val completed: Boolean)
@Serializable data class BackupGoalProgressApplication(val profileId: String, val goalId: String, val completionId: String, val appliedAtEpochMillis: Long)
@Serializable data class BackupQuestCompletion(val completionId: String, val sessionId: String, val questId: String, val profileId: String, val completedAtEpochMillis: Long, val xpAward: Int)
@Serializable data class BackupCompletionObjective(val completionId: String, val objectiveId: String)
@Serializable data class BackupDatasetMetadata(val profileId: String, val datasetRevision: Long, val schemaVersion: Int, val lastModifiedAtEpochMillis: Long)
@Serializable data class BackupRarityScale(val scaleVersion: Int, val referenceMinPopulation: Long, val referenceMaxPopulation: Long, val countingBasis: String, val methodology: String)
@Serializable data class BackupCatalogPack(val packId: String, val profileId: String, val collectionIndex: Int, val version: Int, val generatedAtEpochMillis: Long, val rarityScaleVersion: Int, val sourceUrisJson: String, val integrityHash: String)
@Serializable data class BackupFlowerDefinition(val flowerId: String, val commonName: String, val scientificName: String, val description: String, val rarity: String, val rarityScaleVersion: Int?, val estimatedIndividuals: Long?, val lowerBound: Long?, val upperBound: Long?, val estimateDateEpochMillis: Long?, val evidenceSourceUrisJson: String, val confidence: String?, val countingBasis: String?)
@Serializable data class BackupCatalogEntry(val packId: String, val flowerId: String, val slotIndex: Int, val eligible: Boolean)
@Serializable data class BackupFlowerDiscovery(val profileId: String, val flowerId: String, val state: String)
@Serializable data class BackupFlowerAcquisition(val profileId: String, val flowerId: String, val completionId: String, val acquiredAtEpochMillis: Long, val xpAward: Int)
@Serializable data class BackupRewardOpportunity(val opportunityId: String, val profileId: String, val completionId: String, val state: String, val resolvedFlowerId: String?, val createdAtEpochMillis: Long, val resolvedAtEpochMillis: Long?)
@Serializable data class BackupGameCatalogSnapshot(val snapshotId: String, val cycleId: String, val researchedAtEpochMillis: Long, val expiresAtEpochMillis: Long)
@Serializable data class BackupGameDiscovery(val identityKey: String, val canonicalName: String, val platformsJson: String, val genresJson: String, val availability: String, val provenanceJson: String)
@Serializable data class BackupGameQuestUsage(val usageId: String, val cycleId: String, val gameIdentityKey: String, val variantFingerprint: String, val usedAtEpochMillis: Long)
@Serializable data class BackupPreferences(val theme: String = "SYSTEM", val fontScale: Float = 1.0f)
