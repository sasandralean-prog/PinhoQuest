package com.pinhoquest.data.completion

import androidx.room.withTransaction
import com.pinhoquest.core.completion.CompletionCommitResult
import com.pinhoquest.core.completion.CompletionLoadResult
import com.pinhoquest.core.completion.CompletionPlan
import com.pinhoquest.core.completion.CompletionReceipt
import com.pinhoquest.core.completion.CompletionSnapshot
import com.pinhoquest.core.completion.QuestCompletionStore
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.CompletionObjectiveEntity
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.FlowerAcquisitionEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.db.entity.GoalProgressApplicationEntity
import com.pinhoquest.data.db.entity.GoalProgressEntity
import com.pinhoquest.data.db.entity.QuestCompletionEntity
import com.pinhoquest.data.db.entity.RewardOpportunityEntity
import com.pinhoquest.data.db.entity.XpTransactionEntity
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.repository.RoomQuestSessionRepository
import com.pinhoquest.domain.garden.CatalogEntry
import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.CatalogPackId
import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.EstimateConfidence
import com.pinhoquest.domain.garden.FlowerDefinition
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.garden.FlowerInventory
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalId
import com.pinhoquest.domain.progression.GoalProgress
import com.pinhoquest.domain.progression.GoalRule
import com.pinhoquest.domain.progression.QuestCompletion
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import com.pinhoquest.domain.reward.DeferredRewardReason
import com.pinhoquest.domain.reward.RewardOpportunityId
import com.pinhoquest.domain.reward.RewardResolution

fun interface CompletionFailureInjector {
    fun afterWritesBeforeCommit(plan: CompletionPlan)
}

class RoomQuestCompletionStore(
    private val database: PinhoQuestDatabase,
    private val failureInjector: CompletionFailureInjector = CompletionFailureInjector { },
) : QuestCompletionStore {
    private val progression = database.progressionDao()
    private val garden = database.gardenDao()
    private val questRepository = RoomQuestRepository(database.questDao())
    private val sessionRepository = RoomQuestSessionRepository(database.questSessionDao())
    private val profileRepository = RoomProfileRepository(database.profileDao())

    override suspend fun load(sessionId: QuestSessionId): CompletionLoadResult =
        database.withTransaction {
            existingReceipt(sessionId.value)?.let {
                return@withTransaction CompletionLoadResult.Existing(it)
            }

            val session = sessionRepository.get(sessionId)
                ?: return@withTransaction CompletionLoadResult.NotFound
            if (session.state != QuestState.ACTIVE) {
                return@withTransaction CompletionLoadResult.InvalidState(session.state)
            }
            val quest = questRepository.get(session.questId)
                ?: return@withTransaction CompletionLoadResult.NotFound
            val profile = profileRepository.current()
                ?: return@withTransaction CompletionLoadResult.NotFound

            val xpLedger = XpLedgerSnapshot(
                progression.xpTransactions(profile.id.value).map { it.toDomain() },
            )
            val goalDefinitions = progression.goalDefinitions().mapNotNull { it.toDomainOrNull() }
            val applications = progression.goalApplications(profile.id.value).groupBy { it.goalId }
            val goalProgress = progression.goalProgress(profile.id.value).map { row ->
                GoalProgress(
                    goalId = GoalId(row.goalId),
                    currentValue = row.currentValue,
                    targetValue = row.targetValue,
                    completed = row.completed,
                    appliedCompletionIds = applications[row.goalId]
                        .orEmpty()
                        .map { CompletionId(it.completionId) }
                        .toSet(),
                )
            }

            val catalog = loadCatalog(profile.id)
            val inventory = FlowerInventory(
                garden.acquisitions(profile.id.value)
                    .map { FlowerId(it.flowerId) }
                    .toSet(),
            )
            val revision = progression.datasetMetadata(profile.id.value)?.datasetRevision ?: 0L

            CompletionLoadResult.Ready(
                CompletionSnapshot(
                    profileId = profile.id,
                    quest = quest,
                    session = session,
                    xpLedger = xpLedger,
                    goalDefinitions = goalDefinitions,
                    goalProgress = goalProgress,
                    catalog = catalog,
                    inventory = inventory,
                    datasetRevision = revision,
                ),
            )
        }

    override suspend fun commit(plan: CompletionPlan): CompletionCommitResult =
        database.withTransaction {
            existingReceipt(plan.completion.sessionId.value)?.let {
                return@withTransaction CompletionCommitResult.Existing(it)
            }

            val metadata = progression.datasetMetadata(plan.completion.profileId.value)
            val currentRevision = metadata?.datasetRevision ?: 0L
            if (currentRevision != plan.expectedDatasetRevision) {
                return@withTransaction CompletionCommitResult.Conflict
            }

            val session = database.questSessionDao().get(plan.completion.sessionId.value)
                ?: return@withTransaction CompletionCommitResult.InvalidState(QuestState.ABANDONED)
            val sessionState = QuestState.valueOf(session.state)
            if (sessionState != QuestState.ACTIVE) {
                return@withTransaction CompletionCommitResult.InvalidState(sessionState)
            }

            progression.insertCompletion(
                QuestCompletionEntity(
                    completionId = plan.completion.id.value,
                    sessionId = plan.completion.sessionId.value,
                    questId = plan.completion.questId.value,
                    profileId = plan.completion.profileId.value,
                    completedAtEpochMillis = plan.completion.completedAtEpochMillis,
                    xpAward = plan.completion.xpAward,
                ),
            )
            progression.insertCompletionObjectives(
                plan.completion.completedObjectiveIds.map { objectiveId ->
                    CompletionObjectiveEntity(
                        completionId = plan.completion.id.value,
                        objectiveId = objectiveId.value,
                    )
                },
            )
            progression.insertXpTransaction(plan.xpTransaction.toEntity())

            if (plan.goalEvaluation.progress.isNotEmpty()) {
                progression.upsertGoalProgress(
                    plan.goalEvaluation.progress.map { progress ->
                        GoalProgressEntity(
                            profileId = plan.completion.profileId.value,
                            goalId = progress.goalId.value,
                            currentValue = progress.currentValue,
                            targetValue = progress.targetValue,
                            completed = progress.completed,
                        )
                    },
                )
                progression.insertGoalApplications(
                    plan.goalEvaluation.progress.map { progress ->
                        GoalProgressApplicationEntity(
                            profileId = plan.completion.profileId.value,
                            goalId = progress.goalId.value,
                            completionId = plan.completion.id.value,
                            appliedAtEpochMillis = plan.completion.completedAtEpochMillis,
                        )
                    },
                )
            }

            val rewardEntity = when (val reward = plan.rewardResolution) {
                is RewardResolution.Awarded -> RewardOpportunityEntity(
                    opportunityId = plan.rewardOpportunity.id.value,
                    profileId = plan.rewardOpportunity.profileId.value,
                    completionId = plan.rewardOpportunity.completionId.value,
                    state = "RESOLVED",
                    resolvedFlowerId = reward.flowerId.value,
                    createdAtEpochMillis = plan.rewardOpportunity.createdAtEpochMillis,
                    resolvedAtEpochMillis = plan.completion.completedAtEpochMillis,
                )
                is RewardResolution.Deferred -> RewardOpportunityEntity(
                    opportunityId = plan.rewardOpportunity.id.value,
                    profileId = plan.rewardOpportunity.profileId.value,
                    completionId = plan.rewardOpportunity.completionId.value,
                    state = "DEFERRED",
                    resolvedFlowerId = null,
                    createdAtEpochMillis = plan.rewardOpportunity.createdAtEpochMillis,
                    resolvedAtEpochMillis = null,
                )
            }
            garden.insertRewardOpportunity(rewardEntity)

            plan.acquisition?.let { acquisition ->
                garden.insertAcquisition(
                    FlowerAcquisitionEntity(
                        profileId = acquisition.profileId.value,
                        flowerId = acquisition.flowerId.value,
                        completionId = acquisition.completionId.value,
                        acquiredAtEpochMillis = acquisition.acquiredAtEpochMillis,
                        xpAward = acquisition.xpAward,
                    ),
                )
                garden.upsertDiscovery(
                    FlowerDiscoveryEntity(
                        profileId = acquisition.profileId.value,
                        flowerId = acquisition.flowerId.value,
                        state = "COLLECTED",
                    ),
                )
            }

            database.questSessionDao().updateState(
                sessionId = plan.completion.sessionId.value,
                state = QuestState.COMPLETED.name,
                updatedAtEpochMillis = plan.completion.completedAtEpochMillis,
            )
            database.questDao().updateState(
                questId = plan.completion.questId.value,
                state = QuestState.COMPLETED.name,
            )

            progression.upsertDatasetMetadata(
                DatasetMetadataEntity(
                    profileId = plan.completion.profileId.value,
                    datasetRevision = plan.nextDatasetRevision,
                    schemaVersion = 2,
                    lastModifiedAtEpochMillis = plan.completion.completedAtEpochMillis,
                ),
            )

            failureInjector.afterWritesBeforeCommit(plan)

            CompletionCommitResult.Committed(
                CompletionReceipt(
                    completion = plan.completion,
                    rewardResolution = plan.rewardResolution,
                ),
            )
        }

    private suspend fun existingReceipt(sessionId: String): CompletionReceipt? {
        val row = progression.completionBySession(sessionId) ?: return null
        val opportunity = garden.rewardOpportunityByCompletion(row.completionId)
            ?: error("completion " + row.completionId + " has no reward opportunity")
        val reward = if (opportunity.state == "RESOLVED" && opportunity.resolvedFlowerId != null) {
            RewardResolution.Awarded(
                opportunityId = RewardOpportunityId(opportunity.opportunityId),
                flowerId = FlowerId(opportunity.resolvedFlowerId),
            )
        } else {
            RewardResolution.Deferred(
                opportunityId = RewardOpportunityId(opportunity.opportunityId),
                reason = DeferredRewardReason.NO_ELIGIBLE_FLOWER,
            )
        }
        return CompletionReceipt(
            completion = QuestCompletion(
                id = CompletionId(row.completionId),
                sessionId = QuestSessionId(row.sessionId),
                questId = com.pinhoquest.domain.quest.QuestId(row.questId),
                profileId = ProfileId(row.profileId),
                completedObjectiveIds = progression.completedObjectiveIds(row.completionId)
                    .map(::ObjectiveId)
                    .toSet(),
                xpAward = row.xpAward,
                completedAtEpochMillis = row.completedAtEpochMillis,
            ),
            rewardResolution = reward,
        )
    }

    private suspend fun loadCatalog(profileId: ProfileId): CatalogPack? {
        val pack = garden.latestCatalogPack(profileId.value) ?: return null
        val entries = garden.catalogEntries(pack.packId)
        if (entries.isEmpty()) {
            return CatalogPack(
                id = CatalogPackId(pack.packId),
                version = pack.version,
                generatedAtEpochMillis = pack.generatedAtEpochMillis,
                entries = emptyList(),
                sourceUris = decodeStringArray(pack.sourceUrisJson),
                integrityHash = pack.integrityHash,
            )
        }
        val definitions = garden.flowerDefinitions(entries.map { it.flowerId })
            .associateBy { it.flowerId }

        return CatalogPack(
            id = CatalogPackId(pack.packId),
            version = pack.version,
            generatedAtEpochMillis = pack.generatedAtEpochMillis,
            entries = entries.mapNotNull { entry ->
                val definition = definitions[entry.flowerId] ?: return@mapNotNull null
                CatalogEntry(
                    flower = definition.toDomain(),
                    eligible = entry.eligible,
                )
            },
            sourceUris = decodeStringArray(pack.sourceUrisJson),
            integrityHash = pack.integrityHash,
        )
    }

    private fun com.pinhoquest.data.db.entity.FlowerDefinitionEntity.toDomain(): FlowerDefinition {
        val estimate = if (
            estimatedIndividuals != null &&
            estimateDateEpochMillis != null &&
            confidence != null &&
            countingBasis != null
        ) {
            GlobalPopulationEstimate(
                estimatedIndividuals = estimatedIndividuals,
                lowerBound = lowerBound,
                upperBound = upperBound,
                estimateDateEpochMillis = estimateDateEpochMillis,
                evidenceSourceUris = decodeStringArray(evidenceSourceUrisJson),
                confidence = EstimateConfidence.valueOf(confidence),
                countingBasis = CountingBasis.valueOf(countingBasis),
            )
        } else {
            null
        }
        return FlowerDefinition(
            id = FlowerId(flowerId),
            commonName = commonName,
            scientificName = scientificName,
            description = description,
            rarity = FlowerRarity.valueOf(rarity),
            rarityScaleVersion = rarityScaleVersion,
            populationEstimate = estimate,
        )
    }

    private fun com.pinhoquest.data.db.entity.XpTransactionEntity.toDomain() = XpTransaction(
        id = XpTransactionId(transactionId),
        profileId = ProfileId(profileId),
        amount = amount,
        type = XpTransactionType.valueOf(type),
        completionId = completionId?.let(::CompletionId),
        flowerId = flowerId,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private fun XpTransaction.toEntity() = XpTransactionEntity(
        transactionId = id.value,
        profileId = profileId.value,
        amount = amount,
        type = type.name,
        completionId = completionId?.value,
        flowerId = flowerId,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private fun com.pinhoquest.data.db.entity.GoalDefinitionEntity.toDomainOrNull(): GoalDefinition? {
        val rule = when (ruleType) {
            "COMPLETION_COUNT" -> GoalRule.CompletionCount(target)
            "CATEGORY_COMPLETION" -> category?.let {
                GoalRule.CategoryCompletion(
                    com.pinhoquest.domain.quest.QuestCategory.valueOf(it),
                    target,
                )
            }
            "DIFFICULTY_COMPLETION" -> difficulty?.let {
                GoalRule.DifficultyCompletion(
                    com.pinhoquest.domain.quest.QuestDifficulty.valueOf(it),
                    target,
                )
            }
            "BONUS_OBJECTIVE_COUNT" -> GoalRule.BonusObjectiveCount(target)
            "LIFETIME_XP_MILESTONE" -> GoalRule.LifetimeXpMilestone(target)
            else -> null
        } ?: return null

        return GoalDefinition(
            id = GoalId(goalId),
            title = title,
            rule = rule,
        )
    }

    private fun decodeStringArray(value: String): Set<String> {
        val trimmed = value.trim()
        if (trimmed == "[]" || trimmed.isEmpty()) return emptySet()
        return trimmed
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
            .toSet()
    }
}
