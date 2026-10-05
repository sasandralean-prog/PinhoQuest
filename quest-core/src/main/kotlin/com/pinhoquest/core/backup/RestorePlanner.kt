package com.pinhoquest.core.backup

import com.pinhoquest.domain.backup.BackupSnapshot
import com.pinhoquest.domain.backup.RestorePlanResult

class RestorePlanner(
    private val supportedFormatVersion: Int = 1,
    private val supportedSchemaVersion: Int = 4,
) {
    fun plan(snapshot: BackupSnapshot): RestorePlanResult {
        val manifest = snapshot.manifest
        if (manifest.formatVersion != supportedFormatVersion) {
            return RestorePlanResult.Invalid("Unsupported backup format")
        }
        if (manifest.schemaVersion != supportedSchemaVersion) {
            return RestorePlanResult.Invalid("Unsupported backup schema")
        }
        if (snapshot.profile.profileId != manifest.profileId) {
            return RestorePlanResult.Invalid("Profile mismatch")
        }
        if (snapshot.manifest.profileId.isBlank()) {
            return RestorePlanResult.Invalid("Blank profile id")
        }
        if (snapshot.profile.gardenOwnerName.isBlank()) {
            return RestorePlanResult.Invalid("Blank garden owner name")
        }
        if (!snapshot.preferences.fontScale.isFinite() || snapshot.preferences.fontScale <= 0f) {
            return RestorePlanResult.Invalid("Invalid font scale")
        }

        val themeValid = snapshot.preferences.theme in setOf("SYSTEM", "LIGHT", "DARK")
        if (!themeValid) return RestorePlanResult.Invalid("Invalid theme")

        if (!uniqueBy("tag", snapshot.tags.map { it.profileId to it.tagId })) {
            return RestorePlanResult.Invalid("Duplicate tag")
        }
        if (!uniqueBy("quest", snapshot.quests.map { it.questId })) {
            return RestorePlanResult.Invalid("Duplicate quest")
        }
        if (!uniqueBy("objective", snapshot.questObjectives.map { it.questId to it.objectiveId })) {
            return RestorePlanResult.Invalid("Duplicate quest objective")
        }
        if (!uniqueBy("session", snapshot.questSessions.map { it.sessionId })) {
            return RestorePlanResult.Invalid("Duplicate session")
        }
        if (!uniqueBy("xp", snapshot.xpTransactions.map { it.transactionId })) {
            return RestorePlanResult.Invalid("Duplicate XP transaction")
        }
        if (!uniqueBy("goal", snapshot.goalDefinitions.map { it.goalId })) {
            return RestorePlanResult.Invalid("Duplicate goal")
        }
        if (!uniqueBy("completion", snapshot.questCompletions.map { it.completionId })) {
            return RestorePlanResult.Invalid("Duplicate completion")
        }
        if (!uniqueBy("completion objective", snapshot.completionObjectives.map { it.completionId to it.objectiveId })) {
            return RestorePlanResult.Invalid("Duplicate completion objective")
        }
        if (!uniqueBy("flower", snapshot.flowerDefinitions.map { it.flowerId })) {
            return RestorePlanResult.Invalid("Duplicate flower definition")
        }
        if (!uniqueBy("catalog pack", snapshot.catalogPacks.map { it.packId })) {
            return RestorePlanResult.Invalid("Duplicate catalog pack")
        }
        if (!uniqueBy("catalog entry", snapshot.catalogEntries.map { it.packId to it.flowerId })) {
            return RestorePlanResult.Invalid("Duplicate catalog entry")
        }
        if (!uniqueBy("discovery", snapshot.flowerDiscoveries.map { it.profileId to it.flowerId })) {
            return RestorePlanResult.Invalid("Duplicate flower discovery")
        }
        if (!uniqueBy("acquisition", snapshot.flowerAcquisitions.map { it.profileId to it.flowerId })) {
            return RestorePlanResult.Invalid("Duplicate flower acquisition")
        }
        if (!uniqueBy("reward opportunity", snapshot.rewardOpportunities.map { it.opportunityId })) {
            return RestorePlanResult.Invalid("Duplicate reward opportunity")
        }
        if (!uniqueBy("game snapshot", snapshot.gameCatalogSnapshots.map { it.snapshotId })) {
            return RestorePlanResult.Invalid("Duplicate game catalog snapshot")
        }
        if (!uniqueBy("game discovery", snapshot.gameDiscoveries.map { it.identityKey })) {
            return RestorePlanResult.Invalid("Duplicate game discovery")
        }
        if (!uniqueBy("game usage", snapshot.gameQuestUsages.map { it.usageId })) {
            return RestorePlanResult.Invalid("Duplicate game usage")
        }

        val profileId = manifest.profileId
        if (!allProfileRowsMatch(snapshot, profileId)) {
            return RestorePlanResult.Invalid("Foreign profile data")
        }

        val questIds = snapshot.quests.map { it.questId }.toSet()
        val objectiveIdsByQuest = snapshot.questObjectives.groupBy { it.questId }
            .mapValues { (_, rows) -> rows.map { it.objectiveId }.toSet() }
        val sessionIds = snapshot.questSessions.map { it.sessionId }.toSet()
        val completionIds = snapshot.questCompletions.map { it.completionId }.toSet()
        val flowerIds = snapshot.flowerDefinitions.map { it.flowerId }.toSet()
        val packIds = snapshot.catalogPacks.map { it.packId }.toSet()
        val goalIds = snapshot.goalDefinitions.map { it.goalId }.toSet()
        val gameIds = snapshot.gameDiscoveries.map { it.identityKey }.toSet()

        if (snapshot.questObjectives.any { it.questId !in questIds }) {
            return RestorePlanResult.Invalid("Objective references missing quest")
        }
        if (snapshot.questSessions.any { it.questId !in questIds }) {
            return RestorePlanResult.Invalid("Session references missing quest")
        }
        if (snapshot.questCompletions.any {
                it.sessionId !in sessionIds ||
                    it.questId !in questIds
            }
        ) {
            return RestorePlanResult.Invalid("Completion references missing state")
        }
        if (snapshot.completionObjectives.any {
                val completion = snapshot.questCompletions.firstOrNull { row -> row.completionId == it.completionId }
                    ?: return@any true
                it.objectiveId !in objectiveIdsByQuest[completion.questId].orEmpty()
            }
        ) {
            return RestorePlanResult.Invalid("Completion objective references missing objective")
        }
        if (snapshot.xpTransactions.any {
                it.completionId != null && it.completionId !in completionIds
            }
        ) {
            return RestorePlanResult.Invalid("XP transaction references missing completion")
        }
        if (snapshot.xpTransactions.any {
                it.flowerId != null && it.flowerId !in flowerIds
            }
        ) {
            return RestorePlanResult.Invalid("XP transaction references missing flower")
        }
        if (snapshot.goalProgress.any { it.goalId !in goalIds }) {
            return RestorePlanResult.Invalid("Goal progress references missing goal")
        }
        if (snapshot.goalProgressApplications.any {
                it.goalId !in goalIds || it.completionId !in completionIds
            }
        ) {
            return RestorePlanResult.Invalid("Goal application references missing entity")
        }
        if (snapshot.catalogEntries.any { it.packId !in packIds || it.flowerId !in flowerIds }) {
            return RestorePlanResult.Invalid("Catalog entry references missing entity")
        }
        if (snapshot.catalogPacks.any { it.rarityScaleVersion !in snapshot.rarityScales.map { scale -> scale.scaleVersion }.toSet() }) {
            return RestorePlanResult.Invalid("Catalog pack references missing rarity scale")
        }
        if (snapshot.flowerDiscoveries.any { it.flowerId !in flowerIds }) {
            return RestorePlanResult.Invalid("Flower discovery references missing flower")
        }
        if (snapshot.flowerAcquisitions.any {
                it.flowerId !in flowerIds || it.completionId !in completionIds
            }
        ) {
            return RestorePlanResult.Invalid("Flower acquisition references missing entity")
        }
        if (snapshot.rewardOpportunities.any {
                it.completionId !in completionIds ||
                    (it.resolvedFlowerId != null && it.resolvedFlowerId !in flowerIds)
            }
        ) {
            return RestorePlanResult.Invalid("Reward opportunity references missing entity")
        }
        if (snapshot.gameQuestUsages.any { it.gameIdentityKey !in gameIds }) {
            return RestorePlanResult.Invalid("Game usage references missing game")
        }
        val datasetMetadata = snapshot.datasetMetadata
        if (datasetMetadata != null && datasetMetadata.profileId != profileId) {
            return RestorePlanResult.Invalid("Dataset metadata profile mismatch")
        }

        return RestorePlanResult.Ready(snapshot)
    }

    private fun allProfileRowsMatch(snapshot: BackupSnapshot, profileId: String): Boolean =
        snapshot.tags.all { it.profileId == profileId } &&
            snapshot.xpTransactions.all { it.profileId == profileId } &&
            snapshot.goalProgress.all { it.profileId == profileId } &&
            snapshot.goalProgressApplications.all { it.profileId == profileId } &&
            snapshot.questCompletions.all { it.profileId == profileId } &&
            snapshot.datasetMetadata?.profileId.orEmpty().let { it.isEmpty() || it == profileId } &&
            snapshot.catalogPacks.all { it.profileId == profileId } &&
            snapshot.flowerDiscoveries.all { it.profileId == profileId } &&
            snapshot.flowerAcquisitions.all { it.profileId == profileId } &&
            snapshot.rewardOpportunities.all { it.profileId == profileId }

    private fun <T> uniqueBy(label: String, values: List<T>): Boolean =
        values.size == values.toSet().size
}
