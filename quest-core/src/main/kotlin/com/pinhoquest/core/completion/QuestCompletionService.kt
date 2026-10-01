package com.pinhoquest.core.completion

import com.pinhoquest.core.progression.GoalEvaluator
import com.pinhoquest.core.progression.XpPolicyV1
import com.pinhoquest.core.reward.RewardEngine
import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.FlowerAcquisition
import com.pinhoquest.domain.garden.FlowerInventory
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalEvaluationResult
import com.pinhoquest.domain.progression.GoalId
import com.pinhoquest.domain.progression.GoalProgress
import com.pinhoquest.domain.progression.QuestCompletion
import com.pinhoquest.domain.progression.QuestCompletionFacts
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import com.pinhoquest.domain.reward.DeferredRewardReason
import com.pinhoquest.domain.reward.RewardOpportunity
import com.pinhoquest.domain.reward.RewardOpportunityId
import com.pinhoquest.domain.reward.RewardResolution
import java.util.concurrent.CancellationException

data class CompletionSnapshot(
    val profileId: ProfileId,
    val quest: Quest,
    val session: QuestSession,
    val xpLedger: XpLedgerSnapshot,
    val goalDefinitions: List<GoalDefinition>,
    val goalProgress: List<GoalProgress>,
    val catalog: CatalogPack?,
    val inventory: FlowerInventory,
    val datasetRevision: Long,
)

data class CompletionReceipt(
    val completion: QuestCompletion,
    val rewardResolution: RewardResolution,
)

data class CompletionPlan(
    val expectedDatasetRevision: Long,
    val completion: QuestCompletion,
    val xpTransaction: XpTransaction,
    val goalEvaluation: GoalEvaluationResult,
    val rewardOpportunity: RewardOpportunity,
    val rewardResolution: RewardResolution,
    val acquisition: FlowerAcquisition?,
    val nextDatasetRevision: Long,
)

sealed interface CompletionLoadResult {
    data class Ready(val snapshot: CompletionSnapshot) : CompletionLoadResult
    data class Existing(val receipt: CompletionReceipt) : CompletionLoadResult
    data object NotFound : CompletionLoadResult
    data class InvalidState(val state: QuestState) : CompletionLoadResult
}

sealed interface CompletionCommitResult {
    data class Committed(val receipt: CompletionReceipt) : CompletionCommitResult
    data class Existing(val receipt: CompletionReceipt) : CompletionCommitResult
    data object Conflict : CompletionCommitResult
    data class InvalidState(val state: QuestState) : CompletionCommitResult
}

interface QuestCompletionStore {
    suspend fun load(sessionId: QuestSessionId): CompletionLoadResult
    suspend fun commit(plan: CompletionPlan): CompletionCommitResult
}

sealed interface CompletionResult {
    data class Success(
        val receipt: CompletionReceipt,
        val wasAlreadyCompleted: Boolean,
    ) : CompletionResult

    data object NotFound : CompletionResult
    data class InvalidSessionState(val state: QuestState) : CompletionResult
    data class UnknownObjectives(val objectiveIds: Set<ObjectiveId>) : CompletionResult
    data class MissingRequiredObjectives(val objectiveIds: Set<ObjectiveId>) : CompletionResult
    data object Conflict : CompletionResult
    data class TechnicalFailure(val reason: String) : CompletionResult
}

class QuestCompletionService(
    private val store: QuestCompletionStore,
    private val rewardEngine: RewardEngine,
    private val completionIdFactory: () -> CompletionId,
    private val xpTransactionIdFactory: () -> XpTransactionId,
    private val rewardOpportunityIdFactory: () -> RewardOpportunityId,
    private val nowEpochMillis: () -> Long,
    private val maxConflictRetries: Int = 3,
) {
    init {
        require(maxConflictRetries >= 1)
    }

    suspend fun complete(
        sessionId: QuestSessionId,
        completedObjectives: Set<ObjectiveId>,
    ): CompletionResult {
        return try {
            completeWithRetries(sessionId, completedObjectives)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            CompletionResult.TechnicalFailure(
                reason = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
    }

    private suspend fun completeWithRetries(
        sessionId: QuestSessionId,
        completedObjectives: Set<ObjectiveId>,
    ): CompletionResult {
        repeat(maxConflictRetries) {
            when (val load = store.load(sessionId)) {
                is CompletionLoadResult.Existing -> {
                    return CompletionResult.Success(
                        receipt = load.receipt,
                        wasAlreadyCompleted = true,
                    )
                }
                CompletionLoadResult.NotFound -> return CompletionResult.NotFound
                is CompletionLoadResult.InvalidState -> {
                    return CompletionResult.InvalidSessionState(load.state)
                }
                is CompletionLoadResult.Ready -> {
                    val planning = plan(load.snapshot, completedObjectives)
                    if (planning is PlanningResult.Rejected) return planning.result
                    val plan = (planning as PlanningResult.Planned).plan
                    when (val commit = store.commit(plan)) {
                        is CompletionCommitResult.Committed -> {
                            return CompletionResult.Success(
                                receipt = commit.receipt,
                                wasAlreadyCompleted = false,
                            )
                        }
                        is CompletionCommitResult.Existing -> {
                            return CompletionResult.Success(
                                receipt = commit.receipt,
                                wasAlreadyCompleted = true,
                            )
                        }
                        CompletionCommitResult.Conflict -> Unit
                        is CompletionCommitResult.InvalidState -> {
                            return CompletionResult.InvalidSessionState(commit.state)
                        }
                    }
                }
            }
        }
        return CompletionResult.Conflict
    }

    private fun plan(
        snapshot: CompletionSnapshot,
        completedObjectives: Set<ObjectiveId>,
    ): PlanningResult {
        val objectiveIds = snapshot.quest.objectives.map { it.id }.toSet()
        val unknown = completedObjectives - objectiveIds
        if (unknown.isNotEmpty()) {
            return PlanningResult.Rejected(CompletionResult.UnknownObjectives(unknown))
        }

        val required = snapshot.quest.objectives
            .filterNot { it.optional }
            .map { it.id }
            .toSet()
        val missing = required - completedObjectives
        if (missing.isNotEmpty()) {
            return PlanningResult.Rejected(CompletionResult.MissingRequiredObjectives(missing))
        }

        val now = nowEpochMillis()
        val completionId = completionIdFactory()
        val facts = QuestCompletionFacts(
            completionId = completionId,
            questId = snapshot.quest.id,
            category = snapshot.quest.category,
            difficulty = snapshot.quest.difficulty,
            mainObjectiveIds = required,
            bonusObjectiveIds = snapshot.quest.objectives
                .filter { it.optional }
                .map { it.id }
                .toSet(),
            completedObjectiveIds = completedObjectives,
        )
        val xpAward = XpPolicyV1.awardFor(facts)
        val xpTransaction = XpTransaction(
            id = xpTransactionIdFactory(),
            profileId = snapshot.profileId,
            amount = xpAward,
            type = XpTransactionType.QUEST_REWARD,
            completionId = completionId,
            flowerId = null,
            createdAtEpochMillis = now,
        )

        val lifetimeXpAfter = snapshot.xpLedger.lifetimeXp() + xpAward
        val goalEvaluation = GoalEvaluator(snapshot.goalDefinitions).evaluate(
            completion = facts,
            current = snapshot.goalProgress,
            lifetimeXpAfter = lifetimeXpAfter,
        )

        val rewardOpportunity = RewardOpportunity(
            id = rewardOpportunityIdFactory(),
            profileId = snapshot.profileId,
            completionId = completionId,
            createdAtEpochMillis = now,
        )
        val rewardResolution = snapshot.catalog?.let { catalog ->
            rewardEngine.resolve(
                opportunity = rewardOpportunity,
                catalog = catalog,
                inventory = snapshot.inventory,
            )
        } ?: RewardResolution.Deferred(
            opportunityId = rewardOpportunity.id,
            reason = DeferredRewardReason.NO_ELIGIBLE_FLOWER,
        )

        val completion = QuestCompletion(
            id = completionId,
            sessionId = snapshot.session.id,
            questId = snapshot.quest.id,
            profileId = snapshot.profileId,
            completedObjectiveIds = completedObjectives,
            xpAward = xpAward,
            completedAtEpochMillis = now,
        )

        val acquisition = when (rewardResolution) {
            is RewardResolution.Awarded -> FlowerAcquisition(
                profileId = snapshot.profileId,
                flowerId = rewardResolution.flowerId,
                completionId = completionId,
                acquiredAtEpochMillis = now,
                xpAward = xpAward,
            )
            is RewardResolution.Deferred -> null
        }

        return PlanningResult.Planned(
            CompletionPlan(
                expectedDatasetRevision = snapshot.datasetRevision,
                completion = completion,
                xpTransaction = xpTransaction,
                goalEvaluation = goalEvaluation,
                rewardOpportunity = rewardOpportunity,
                rewardResolution = rewardResolution,
                acquisition = acquisition,
                nextDatasetRevision = snapshot.datasetRevision + 1,
            ),
        )
    }

    private sealed interface PlanningResult {
        data class Planned(val plan: CompletionPlan) : PlanningResult
        data class Rejected(val result: CompletionResult) : PlanningResult
    }
}
